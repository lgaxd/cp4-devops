#!/bin/bash

set -euo pipefail

# ============================================================
# SpaceCrop - Azure Deploy
# Checkpoint DevOps - ACR + ACI + Azure File
# RM: 561413
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$PROJECT_DIR/.env"

# ------------------------------------------------------------
# 1. Carregar .env de forma segura
# ------------------------------------------------------------

if [ ! -f "$ENV_FILE" ]; then
    echo "ERRO: arquivo .env não encontrado em:"
    echo "$ENV_FILE"
    exit 1
fi

# Remove CRLF caso o arquivo tenha vindo do Windows
sed -i 's/\r$//' "$ENV_FILE"

set -a
source "$ENV_FILE"
set +a

# ------------------------------------------------------------
# 2. Variáveis fixas da infraestrutura
# ------------------------------------------------------------

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

# ------------------------------------------------------------
# 3. Configuração da aplicação
# ------------------------------------------------------------

# Usuário Oracle FIXO para evitar configuração inconsistente
APP_USER="spacecrop"

ORACLE_PASSWORD="${ORACLE_PASSWORD:-}"
APP_USER_PASSWORD="${APP_USER_PASSWORD:-}"
JWT_SECRET="${JWT_SECRET:-}"
JWT_EXPIRATION="${JWT_EXPIRATION:-86400000}"

# ------------------------------------------------------------
# 4. Validação das variáveis
# ------------------------------------------------------------

if [ -z "$ORACLE_PASSWORD" ]; then
    echo "ERRO: ORACLE_PASSWORD não foi definida no .env"
    exit 1
fi

if [ -z "$APP_USER_PASSWORD" ]; then
    echo "ERRO: APP_USER_PASSWORD não foi definida no .env"
    exit 1
fi

if [ -z "$JWT_SECRET" ]; then
    echo "ERRO: JWT_SECRET não foi definida no .env"
    exit 1
fi

if [ -z "$JWT_EXPIRATION" ]; then
    echo "ERRO: JWT_EXPIRATION não foi definida."
    exit 1
fi

echo "============================================================"
echo " SpaceCrop - Azure Deploy"
echo "============================================================"
echo "Resource Group : $RESOURCE_GROUP"
echo "Location       : $LOCATION"
echo "ACR            : $ACR_NAME"
echo "Oracle User    : $APP_USER"
echo "JWT Expiration : $JWT_EXPIRATION"
echo "============================================================"

echo ""
echo "===> Validando Azure CLI..."

az account show \
    --query "{subscription:name, user:user.name}" \
    -o table

# ------------------------------------------------------------
# 5. Resource Group
# ------------------------------------------------------------

echo ""
echo "===> 1) Criando Resource Group..."

az group create \
    --name "$RESOURCE_GROUP" \
    --location "$LOCATION" \
    --output none

# ------------------------------------------------------------
# 6. ACR
# ------------------------------------------------------------

echo ""
echo "===> 2) Criando ACR..."

az acr create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACR_NAME" \
    --sku Basic \
    --admin-enabled true \
    --output none

ACR_LOGIN_SERVER=$(az acr show \
    --name "$ACR_NAME" \
    --query loginServer \
    -o tsv)

echo "ACR: $ACR_LOGIN_SERVER"

# ------------------------------------------------------------
# 7. Build local
# ------------------------------------------------------------

echo ""
echo "===> 3) Build local das imagens..."

docker build \
    -t "${DB_IMAGE_NAME}:${IMAGE_TAG}" \
    -f docker/Dockerfile.db \
    docker/

docker build \
    -t "${APP_IMAGE_NAME}:${IMAGE_TAG}" \
    -f docker/Dockerfile.app \
    .

echo ""
echo "Imagens construídas."

# ------------------------------------------------------------
# 8. Login ACR
# ------------------------------------------------------------

echo ""
echo "===> 4) Login no ACR..."

az acr login \
    --name "$ACR_NAME"

# ------------------------------------------------------------
# 9. Tag das imagens
# ------------------------------------------------------------

docker tag \
    "${DB_IMAGE_NAME}:${IMAGE_TAG}" \
    "${ACR_LOGIN_SERVER}/${DB_IMAGE_NAME}:${IMAGE_TAG}"

docker tag \
    "${APP_IMAGE_NAME}:${IMAGE_TAG}" \
    "${ACR_LOGIN_SERVER}/${APP_IMAGE_NAME}:${IMAGE_TAG}"

# ------------------------------------------------------------
# 10. Push banco
# ------------------------------------------------------------

echo ""
echo "===> 5) Push da imagem do banco..."

docker push \
    "${ACR_LOGIN_SERVER}/${DB_IMAGE_NAME}:${IMAGE_TAG}"

# ------------------------------------------------------------
# 11. Push aplicação
# ------------------------------------------------------------

echo ""
echo "===> 6) Push da imagem da aplicação..."

docker push \
    "${ACR_LOGIN_SERVER}/${APP_IMAGE_NAME}:${IMAGE_TAG}"

# ------------------------------------------------------------
# 12. Storage Account
# ------------------------------------------------------------

echo ""
echo "===> 7) Criando Storage Account..."

az storage account create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$STORAGE_ACCOUNT" \
    --location "$LOCATION" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --output none

STORAGE_KEY=$(az storage account keys list \
    --resource-group "$RESOURCE_GROUP" \
    --account-name "$STORAGE_ACCOUNT" \
    --query "[0].value" \
    -o tsv)

# ------------------------------------------------------------
# 13. Azure File Share
# ------------------------------------------------------------

echo ""
echo "===> 8) Criando Azure File Share..."

az storage share create \
    --account-name "$STORAGE_ACCOUNT" \
    --account-key "$STORAGE_KEY" \
    --name "$FILE_SHARE_NAME" \
    --quota 10 \
    --output none

# ------------------------------------------------------------
# 14. Credenciais ACR
# ------------------------------------------------------------

ACR_USERNAME=$(az acr credential show \
    --name "$ACR_NAME" \
    --query username \
    -o tsv)

ACR_PASSWORD=$(az acr credential show \
    --name "$ACR_NAME" \
    --query "passwords[0].value" \
    -o tsv)

# ------------------------------------------------------------
# 15. ACI Oracle
# ------------------------------------------------------------

echo ""
echo "===> 9) Criando ACI do Oracle..."

az container create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_DB_NAME" \
    --image "${ACR_LOGIN_SERVER}/${DB_IMAGE_NAME}:${IMAGE_TAG}" \
    --registry-login-server "$ACR_LOGIN_SERVER" \
    --registry-username "$ACR_USERNAME" \
    --registry-password "$ACR_PASSWORD" \
    --cpu 2 \
    --memory 4 \
    --os-type Linux \
    --ports 1521 \
    --ip-address Public \
    --dns-name-label "$DB_DNS_LABEL" \
    --environment-variables \
        APP_USER="$APP_USER" \
    --secure-environment-variables \
        ORACLE_PASSWORD="$ORACLE_PASSWORD" \
        APP_USER_PASSWORD="$APP_USER_PASSWORD" \
    --azure-file-volume-account-name "$STORAGE_ACCOUNT" \
    --azure-file-volume-account-key "$STORAGE_KEY" \
    --azure-file-volume-share-name "$FILE_SHARE_NAME" \
    --azure-file-volume-mount-path "/opt/oracle/oradata" \
    --output none

DB_FQDN=$(az container show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_DB_NAME" \
    --query "ipAddress.fqdn" \
    -o tsv)

echo ""
echo "Banco: $DB_FQDN:1521"

# ------------------------------------------------------------
# 16. Aguardar Oracle
# ------------------------------------------------------------

echo ""
echo "===> Aguardando Oracle iniciar..."

MAX_ATTEMPTS=60
DB_READY=false

for ((i=1; i<=MAX_ATTEMPTS; i++)); do

    CURRENT_STATE=$(az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_DB_NAME" \
        --query "containers[0].instanceView.currentState.state" \
        -o tsv 2>/dev/null || true)

    DETAIL_STATUS=$(az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_DB_NAME" \
        --query "containers[0].instanceView.currentState.detailStatus" \
        -o tsv 2>/dev/null || true)

    echo "Tentativa $i/$MAX_ATTEMPTS - estado: ${CURRENT_STATE:-unknown} - ${DETAIL_STATUS:-unknown}"

    if [[ "$DETAIL_STATUS" == *"CrashLoopBackOff"* ]] || \
       [[ "$DETAIL_STATUS" == *"Error"* ]]; then

        echo ""
        echo "ERRO: Oracle entrou em estado de falha."
        echo ""
        echo "Estado atual:"
        az container show \
            --resource-group "$RESOURCE_GROUP" \
            --name "$ACI_DB_NAME" \
            --query "containers[0].instanceView" \
            -o json

        exit 1
    fi

    if [ "$CURRENT_STATE" = "Running" ]; then

        if timeout 5 bash -c "</dev/tcp/${DB_FQDN}/1521" 2>/dev/null; then
            echo "Porta 1521 acessível."

            # Mantém uma janela para o Oracle terminar sua inicialização
            echo "Aguardando inicialização completa do Oracle..."
            sleep 30

            CURRENT_STATE=$(az container show \
                --resource-group "$RESOURCE_GROUP" \
                --name "$ACI_DB_NAME" \
                --query "containers[0].instanceView.currentState.state" \
                -o tsv 2>/dev/null || true)

            DETAIL_STATUS=$(az container show \
                --resource-group "$RESOURCE_GROUP" \
                --name "$ACI_DB_NAME" \
                --query "containers[0].instanceView.currentState.detailStatus" \
                -o tsv 2>/dev/null || true)

            if [ "$CURRENT_STATE" = "Running" ] && \
               [[ "$DETAIL_STATUS" != *"CrashLoopBackOff"* ]]; then

                DB_READY=true
                break
            fi
        fi
    fi

    sleep 10
done

if [ "$DB_READY" != true ]; then
    echo ""
    echo "ERRO: Oracle não ficou disponível."

    az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_DB_NAME" \
        --query "containers[0].instanceView" \
        -o json

    exit 1
fi

echo ""
echo "Oracle aparentemente está estável."

# ------------------------------------------------------------
# 17. ACI aplicação
# ------------------------------------------------------------

echo ""
echo "===> 10) Criando ACI da aplicação..."

DATABASE_URL="jdbc:oracle:thin:@//${DB_FQDN}:1521/FREEPDB1"

az container create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_APP_NAME" \
    --image "${ACR_LOGIN_SERVER}/${APP_IMAGE_NAME}:${IMAGE_TAG}" \
    --registry-login-server "$ACR_LOGIN_SERVER" \
    --registry-username "$ACR_USERNAME" \
    --registry-password "$ACR_PASSWORD" \
    --cpu 1 \
    --memory 2 \
    --os-type Linux \
    --ports 8080 \
    --ip-address Public \
    --dns-name-label "$APP_DNS_LABEL" \
    --environment-variables \
        DATABASE_URL="$DATABASE_URL" \
        DATABASE_USERNAME="$APP_USER" \
        JWT_EXPIRATION="$JWT_EXPIRATION" \
    --secure-environment-variables \
        DATABASE_PASSWORD="$APP_USER_PASSWORD" \
        JWT_SECRET="$JWT_SECRET" \
    --output none

# ------------------------------------------------------------
# 18. Aguardar aplicação
# ------------------------------------------------------------

echo ""
echo "===> Aguardando aplicação Spring Boot..."

APP_READY=false

for ((i=1; i<=40; i++)); do

    APP_STATE=$(az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_APP_NAME" \
        --query "containers[0].instanceView.currentState.state" \
        -o tsv 2>/dev/null || true)

    APP_DETAIL=$(az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_APP_NAME" \
        --query "containers[0].instanceView.currentState.detailStatus" \
        -o tsv 2>/dev/null || true)

    echo "Tentativa $i/40 - estado: ${APP_STATE:-unknown} - ${APP_DETAIL:-unknown}"

    if [[ "$APP_DETAIL" == *"CrashLoopBackOff"* ]] || \
       [[ "$APP_DETAIL" == *"Error"* ]]; then

        echo ""
        echo "ERRO: aplicação entrou em estado de falha."
        echo ""
        az container logs \
            --resource-group "$RESOURCE_GROUP" \
            --name "$ACI_APP_NAME" || true

        exit 1
    fi

    if [ "$APP_STATE" = "Running" ]; then
        APP_READY=true
        break
    fi

    sleep 10
done

if [ "$APP_READY" != true ]; then
    echo ""
    echo "ERRO: aplicação não ficou disponível."

    az container show \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_APP_NAME" \
        --query "containers[0].instanceView" \
        -o json

    az container logs \
        --resource-group "$RESOURCE_GROUP" \
        --name "$ACI_APP_NAME" || true

    exit 1
fi

# ------------------------------------------------------------
# 19. Informações finais
# ------------------------------------------------------------

APP_FQDN=$(az container show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACI_APP_NAME" \
    --query "ipAddress.fqdn" \
    -o tsv)

echo ""
echo "============================================================"
echo " SpaceCrop - Deploy concluído"
echo "============================================================"
echo " Resource Group: $RESOURCE_GROUP"
echo " ACR:            $ACR_LOGIN_SERVER"
echo " ACI DB:         $ACI_DB_NAME"
echo " ACI APP:        $ACI_APP_NAME"
echo ""
echo " API:"
echo " http://${APP_FQDN}:8080"
echo ""
echo " Swagger:"
echo " http://${APP_FQDN}:8080/swagger"
echo ""
echo " Banco:"
echo " ${DB_FQDN}:1521/FREEPDB1"
echo ""
echo " Oracle User:"
echo " $APP_USER"
echo "============================================================"