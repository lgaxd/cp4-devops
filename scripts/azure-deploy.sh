#!/usr/bin/env bash

set -euo pipefail

# =========================================================
# SpaceCrop - Azure ACR + Storage Account + ACI
# =========================================================

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)
PROJECT_ROOT=$(cd -- "$SCRIPT_DIR/.." && pwd)
ENV_FILE="$PROJECT_ROOT/.env"

if [ ! -f "$ENV_FILE" ]; then
  echo "Arquivo .env nao encontrado: $ENV_FILE" >&2
  exit 1
fi

set -a
. "$ENV_FILE"
set +a

# =========================================================
# Configuracoes
# =========================================================

RM="561413"
LOCATION="brazilsouth"

RESOURCE_GROUP="${RM}-spacecrop-rg"
ACR_NAME="${RM}spacecropacr"

STORAGE_ACCOUNT="${RM}spacecropstorage"
FILE_SHARE="spacecrop-backup"

DB_CONTAINER="${RM}-spacecrop-db"
APP_CONTAINER="${RM}-spacecrop-api"

APP_DNS_LABEL="${RM}spacecrop"

DATABASE_USERNAME="${DATABASE_USERNAME:-${APP_USER:-}}"
DATABASE_PASSWORD="${DATABASE_PASSWORD:-${APP_USER_PASSWORD:-}}"

: "${ORACLE_PASSWORD:?ORACLE_PASSWORD nao definido no .env}"
: "${DATABASE_USERNAME:?APP_USER ou DATABASE_USERNAME nao definido no .env}"
: "${DATABASE_PASSWORD:?APP_USER_PASSWORD ou DATABASE_PASSWORD nao definido no .env}"
: "${JWT_SECRET:?JWT_SECRET nao definido no .env}"

echo "=========================================="
echo "SpaceCrop - Azure Deployment"
echo "=========================================="

# =========================================================
# 1. Login
# =========================================================

echo ""
echo "[1/10] Login Azure"

az account show >/dev/null

# =========================================================
# 2. Resource Group
# =========================================================

echo ""
echo "[2/10] Criando Resource Group"

az group create \
  --name "$RESOURCE_GROUP" \
  --location "$LOCATION"

# =========================================================
# 3. Provider
# =========================================================

echo ""
echo "[3/10] Registrando Container Registry"

az provider register \
  --namespace Microsoft.ContainerRegistry

# =========================================================
# 4. ACR
# =========================================================

echo ""
echo "[4/10] Criando ACR"

if az acr show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" >/dev/null 2>&1; then

  echo "ACR ja existe. Reutilizando."

else

  az acr create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$ACR_NAME" \
    --sku Standard \
    --location "$LOCATION" \
    --public-network-enabled true \
    --admin-enabled true

fi

# =========================================================
# 5. Storage Account
# =========================================================

echo ""
echo "[5/10] Criando Storage Account"

if az storage account show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$STORAGE_ACCOUNT" >/dev/null 2>&1; then

  echo "Storage Account ja existe. Reutilizando."

else

  az storage account create \
    --resource-group "$RESOURCE_GROUP" \
    --name "$STORAGE_ACCOUNT" \
    --location "$LOCATION" \
    --sku Standard_LRS \
    --kind StorageV2 \
    --min-tls-version TLS1_2

fi

STORAGE_KEY=$(az storage account keys list \
  --resource-group "$RESOURCE_GROUP" \
  --account-name "$STORAGE_ACCOUNT" \
  --query "[0].value" \
  --output tsv)

# =========================================================
# 6. File Share
# =========================================================

echo ""
echo "[6/10] Criando File Share para backups"

az storage share create \
  --name "$FILE_SHARE" \
  --account-name "$STORAGE_ACCOUNT" \
  --account-key "$STORAGE_KEY" \
  >/dev/null

LOGIN_SERVER=$(az acr show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --query loginServer \
  --output tsv)

ACR_USERNAME=$(az acr credential show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --query username \
  --output tsv)

ACR_PASSWORD=$(az acr credential show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$ACR_NAME" \
  --query "passwords[0].value" \
  --output tsv)

echo ""
echo "=========================================="
echo "ACR"
echo "=========================================="
echo "Server: $LOGIN_SERVER"

# =========================================================
# 7. Login ACR
# =========================================================

echo ""
echo "[7/10] Login no ACR"

az acr login --name "$ACR_NAME"

# =========================================================
# 8. Build
# =========================================================

echo ""
echo "[8/10] Build das imagens"

docker build \
  -f docker/Dockerfile.app \
  -t "${LOGIN_SERVER}/spacecrop-api:v1" \
  .

docker build \
  -f docker/Dockerfile.db \
  -t "${LOGIN_SERVER}/spacecrop-db:v1" \
  .

# =========================================================
# 9. Push
# =========================================================

echo ""
echo "[9/10] Push das imagens"

docker push "${LOGIN_SERVER}/spacecrop-api:v1"
docker push "${LOGIN_SERVER}/spacecrop-db:v1"

echo ""
echo "=========================================="
echo "Imagens disponíveis no ACR"
echo "=========================================="

az acr repository list \
  --name "$ACR_NAME" \
  --output table

# =========================================================
# 10. ACI Banco
# =========================================================

echo ""
echo "[10/10] Criando ACI do banco"

export LOCATION
export ACR_SERVER="$LOGIN_SERVER"
export ACR_USERNAME
export ACR_PASSWORD
export RESOURCE_GROUP
export STORAGE_ACCOUNT
export STORAGE_KEY
export FILE_SHARE
export DB_CONTAINER
export APP_CONTAINER
export APP_DNS_LABEL
export DATABASE_USERNAME
export DATABASE_PASSWORD
export ORACLE_PASSWORD
export JWT_SECRET

az container delete \
  --resource-group "$RESOURCE_GROUP" \
  --name "$DB_CONTAINER" \
  --yes \
  >/dev/null 2>&1 || true

envsubst < "$PROJECT_ROOT/azure/aci-db.yaml" > /tmp/spacecrop-aci-db.yaml

az container create \
  --resource-group "$RESOURCE_GROUP" \
  --file /tmp/spacecrop-aci-db.yaml

echo ""
echo "Aguardando inicialização do Oracle..."

for i in {1..60}; do
  STATE=$(az container show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$DB_CONTAINER" \
    --query "instanceView.state" \
    --output tsv 2>/dev/null || true)

  echo "Estado do banco: ${STATE:-aguardando}"

  if [ "$STATE" = "Running" ]; then
    break
  fi

  sleep 10
done

DB_IP=$(az container show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$DB_CONTAINER" \
  --query "ipAddress.ip" \
  --output tsv)

echo ""
echo "=========================================="
echo "Banco de dados"
echo "=========================================="
echo "IP: $DB_IP"
echo "Porta: 1521"
echo "Service: FREEPDB1"

echo ""
echo "Logs do banco:"
az container logs \
  --resource-group "$RESOURCE_GROUP" \
  --name "$DB_CONTAINER" \
  --container-name spacecrop-db \
  || true

# =========================================================
# ACI API
# =========================================================

echo ""
echo "Criando ACI da API"

export DB_IP

envsubst < "$PROJECT_ROOT/azure/aci-app.yaml" > /tmp/spacecrop-aci-app.yaml

az container delete \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_CONTAINER" \
  --yes \
  >/dev/null 2>&1 || true

az container create \
  --resource-group "$RESOURCE_GROUP" \
  --file /tmp/spacecrop-aci-app.yaml

echo ""
echo "Aguardando inicialização da API..."

for i in {1..30}; do
  STATE=$(az container show \
    --resource-group "$RESOURCE_GROUP" \
    --name "$APP_CONTAINER" \
    --query "instanceView.state" \
    --output tsv 2>/dev/null || true)

  echo "Estado da API: ${STATE:-aguardando}"

  if [ "$STATE" = "Running" ]; then
    break
  fi

  sleep 10
done

APP_IP=$(az container show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_CONTAINER" \
  --query "ipAddress.ip" \
  --output tsv)

APP_FQDN=$(az container show \
  --resource-group "$RESOURCE_GROUP" \
  --name "$APP_CONTAINER" \
  --query "ipAddress.fqdn" \
  --output tsv)

echo ""
echo "=========================================="
echo "DEPLOY CONCLUÍDO"
echo "=========================================="
echo ""
echo "Resource Group:"
echo "$RESOURCE_GROUP"
echo ""
echo "ACR:"
echo "$LOGIN_SERVER"
echo ""
echo "Database ACI:"
echo "$DB_CONTAINER"
echo "Database IP:"
echo "$DB_IP"
echo ""
echo "API ACI:"
echo "$APP_CONTAINER"
echo "API IP:"
echo "$APP_IP"
echo "API FQDN:"
echo "$APP_FQDN"
echo ""
echo "Swagger:"
echo "http://${APP_IP}:8080/swagger"
echo ""
echo "Swagger FQDN:"
echo "http://${APP_FQDN}:8080/swagger"
echo ""
echo "=========================================="