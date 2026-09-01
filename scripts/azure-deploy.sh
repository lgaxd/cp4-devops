#!/bin/bash
# =====================================================================
# azure-deploy.sh
# Provisiona toda a infraestrutura do Checkpoint (ACR + ACI) via Azure CLI
# Grupo: SpaceCrop DevOps | RM representante: 561413
# =====================================================================
set -e

# --------------------------- VARIÁVEIS --------------------------------
RM="561413"
RESOURCE_GROUP="rm${RM}-spacecrop-rg"
LOCATION="brazilsouth"

ACR_NAME="rm${RM}acr"
STORAGE_ACCOUNT="rm${RM}storage"
FILE_SHARE_NAME="oracle-data"

DB_IMAGE_NAME="rm${RM}-spacecrop-db"
APP_IMAGE_NAME="rm${RM}-spacecrop-app"
IMAGE_TAG="latest"

ACI_DB_NAME="rm${RM}-db"
ACI_APP_NAME="rm${RM}-app"

DB_DNS_LABEL="rm${RM}-spacecrop-db"
APP_DNS_LABEL="rm${RM}-spacecrop-app"

# Credenciais do banco
ORACLE_PASSWORD="${ORACLE_PASSWORD}"
APP_USER="${APP_USER:-spacecrop}"
APP_USER_PASSWORD="${APP_USER_PASSWORD}"

JWT_SECRET="${JWT_SECRET}"
JWT_EXPIRATION="${JWT_EXPIRATION}"

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

az acr login --name "$ACR_NAME"

docker build -t "${DB_IMAGE_NAME}:${IMAGE_TAG}" -f ../docker/Dockerfile.db ../docker/
docker build -t "${APP_IMAGE_NAME}:${IMAGE_TAG}" -f ../Dockerfile.app ..

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

az storage share create \
  --name "$FILE_SHARE_NAME" \
  --account-name "$STORAGE_ACCOUNT" \
  --account-key "$STORAGE_KEY" \
  --quota 20

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
  --azure-file-volume-mount-path "/opt/oracle/oradata"

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
