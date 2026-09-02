#!/bin/bash
# =====================================================================
# azure-deploy.sh
# Provisiona toda a infraestrutura do Checkpoint (ACR + ACI) via Azure CLI
# Grupo: SpaceCrop DevOps | RM representante: 561413
# =====================================================================
set -e

# --------------------------- .env (raiz do projeto) --------------------
# Este script fica em scripts/, então a raiz do projeto é um nível acima.
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"

if [ ! -f "$ENV_FILE" ]; then
    echo "ERRO: arquivo .env não encontrado em:"
    echo "  $ENV_FILE"
    echo ""
    echo "Copie o .env.example para .env na raiz do projeto e preencha os valores:"
    echo "  cp .env.example .env"
    exit 1
fi

# Remove CRLF (caso o arquivo tenha vindo de um editor no Windows)
sed -i 's/\r$//' "$ENV_FILE"

# Carrega todas as variáveis do .env no ambiente deste script
set -a
source "$ENV_FILE"
set +a

echo "Variáveis carregadas de: $ENV_FILE"

# --------------------------- VARIÁVEIS --------------------------------
RM="${RM:-561413}"                           # <-- RM do representante do grupo (prefixo obrigatório)
RESOURCE_GROUP="rm${RM}-spacecrop-rg"
LOCATION="brazilsouth"

ACR_NAME="rm${RM}acr"                        # nomes de ACR só podem ter letras/números
STORAGE_ACCOUNT="rm${RM}storage"             # idem para storage account
FILE_SHARE_NAME="oracle-data"

DB_IMAGE_NAME="rm${RM}-spacecrop-db"
APP_IMAGE_NAME="rm${RM}-spacecrop-app"
IMAGE_TAG="latest"

ACI_DB_NAME="rm${RM}-db"
ACI_APP_NAME="rm${RM}-app"

DB_DNS_LABEL="rm${RM}-spacecrop-db"
APP_DNS_LABEL="rm${RM}-spacecrop-app"

# Credenciais do banco (troque antes de rodar / use variáveis de ambiente)
ORACLE_PASSWORD="${ORACLE_PASSWORD:?defina a variável ORACLE_PASSWORD antes de rodar}"
APP_USER="${APP_USER:-spacecrop}"
APP_USER_PASSWORD="${APP_USER_PASSWORD:?defina a variável APP_USER_PASSWORD antes de rodar}"

JWT_SECRET="${JWT_SECRET:?defina a variável JWT_SECRET antes de rodar}"
JWT_EXPIRATION="${JWT_EXPIRATION:-86400000}"

echo "===> 1) Criando Resource Group..."
az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION"

echo "===> 2) Criando Azure Container Registry (ACR)..."
az acr create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --sku Basic \
  --admin-enabled true

echo "===> 3) Build e push das imagens direto no ACR (docker build + docker push)"
# Login no ACR (equivalente a docker login usando as credenciais do registry)
az acr login --name "$ACR_NAME"

# ---- Build local das imagens (etapa 1 e 2 do checklist) ----
docker build -t "${DB_IMAGE_NAME}:${IMAGE_TAG}" -f docker/Dockerfile.db docker/
docker build -t "${APP_IMAGE_NAME}:${IMAGE_TAG}" -f docker/Dockerfile.app .

# ---- Testar localmente antes do push (docker-compose) ----
# docker compose -f docker-compose.yml up -d   (ver seção "Testes locais" do README)

# ---- Tag apontando para o ACR ----
ACR_LOGIN_SERVER=$(az acr show --name "$ACR_NAME" --query loginServer -o tsv)
docker tag "${DB_IMAGE_NAME}:${IMAGE_TAG}"  "${ACR_LOGIN_SERVER}/${DB_IMAGE_NAME}:${IMAGE_TAG}"
docker tag "${APP_IMAGE_NAME}:${IMAGE_TAG}" "${ACR_LOGIN_SERVER}/${APP_IMAGE_NAME}:${IMAGE_TAG}"

# ---- Push das imagens para o ACR ----
docker push "${ACR_LOGIN_SERVER}/${DB_IMAGE_NAME}:${IMAGE_TAG}"
docker push "${ACR_LOGIN_SERVER}/${APP_IMAGE_NAME}:${IMAGE_TAG}"

echo "===> 4) Criando Conta de Armazenamento + File Share (persistência do Oracle)"
az storage account create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$STORAGE_ACCOUNT" \
  --location "$LOCATION" \
  --sku Standard_LRS \
  --kind StorageV2

STORAGE_KEY=$(az storage account keys list \
  --resource-group "$RESOURCE_GROUP" \
  --account-name "$STORAGE_ACCOUNT" \
  --query "[0].value" -o tsv)

# A Storage Account recém-criada pode levar alguns segundos para o
# endpoint de Files propagar. Tenta criar o share com retry em vez de
# falhar direto com "ResourceNotFound".
SHARE_CREATED=false
for attempt in $(seq 1 10); do
  if az storage share create \
      --name "$FILE_SHARE_NAME" \
      --account-name "$STORAGE_ACCOUNT" \
      --account-key "$STORAGE_KEY" \
      --quota 20 \
      --output none 2>/tmp/share_create.log; then
    SHARE_CREATED=true
    break
  fi
  echo "Aguardando propagação do endpoint de Files (tentativa $attempt/10)..."
  sleep 15
done

if [ "$SHARE_CREATED" != true ]; then
  echo ""
  echo "ERRO: não foi possível criar o File Share após várias tentativas."
  cat /tmp/share_create.log
  exit 1
fi

echo "File Share '$FILE_SHARE_NAME' criado com sucesso."

echo "===> 5) Obtendo credenciais do ACR para o ACI"
ACR_USERNAME=$(az acr credential show --name "$ACR_NAME" --query username -o tsv)
ACR_PASSWORD=$(az acr credential show --name "$ACR_NAME" --query "passwords[0].value" -o tsv)

echo "===> 6) Criando o ACI do Banco de Dados (rm${RM}-db) com volume persistente"
az container create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACI_DB_NAME" \
  --image "${ACR_LOGIN_SERVER}/${DB_IMAGE_NAME}:${IMAGE_TAG}" \
  --registry-login-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_USERNAME" \
  --registry-password "$ACR_PASSWORD" \
  --cpu 2 --memory 4 \
  --os-type Linux \
  --ports 1521 \
  --ip-address Public \
  --dns-name-label "$DB_DNS_LABEL" \
  --environment-variables \
      ORACLE_PASSWORD="$ORACLE_PASSWORD" \
      APP_USER="$APP_USER" \
      APP_USER_PASSWORD="$APP_USER_PASSWORD" \
  --azure-file-volume-account-name "$STORAGE_ACCOUNT" \
  --azure-file-volume-account-key "$STORAGE_KEY" \
  --azure-file-volume-share-name "$FILE_SHARE_NAME" \
  --azure-file-volume-mount-path "/mnt/backup"

echo "===> Aguardando o banco iniciar (pode levar alguns minutos)..."
DB_FQDN=$(az container show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACI_DB_NAME" \
  --query "ipAddress.fqdn" -o tsv)
echo "Banco disponível em: ${DB_FQDN}:1521"
sleep 180

echo "===> 7) Criando o ACI da aplicação (rm${RM}-app) apontando para o banco"
az container create \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACI_APP_NAME" \
  --image "${ACR_LOGIN_SERVER}/${APP_IMAGE_NAME}:${IMAGE_TAG}" \
  --registry-login-server "$ACR_LOGIN_SERVER" \
  --registry-username "$ACR_USERNAME" \
  --registry-password "$ACR_PASSWORD" \
  --cpu 1 --memory 2 \
  --os-type Linux \
  --ports 8080 \
  --ip-address Public \
  --dns-name-label "$APP_DNS_LABEL" \
  --environment-variables \
      DATABASE_URL="jdbc:oracle:thin:@${DB_FQDN}:1521/FREEPDB1" \
      DATABASE_USERNAME="$APP_USER" \
      DATABASE_PASSWORD="$APP_USER_PASSWORD" \
      JWT_SECRET="$JWT_SECRET" \
      JWT_EXPIRATION="$JWT_EXPIRATION"

APP_FQDN=$(az container show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACI_APP_NAME" \
  --query "ipAddress.fqdn" -o tsv)

echo "============================================================"
echo " Deploy concluído!"
echo " API:      http://${APP_FQDN}:8080"
echo " Swagger:  http://${APP_FQDN}:8080/swagger"
echo " Banco:    ${DB_FQDN}:1521/FREEPDB1"
echo "============================================================"
