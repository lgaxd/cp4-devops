#!/bin/bash
# Remove todo o Resource Group do checkpoint (ACR, ACI, Storage Account)
set -e

RM="561413"
RESOURCE_GROUP="rm${RM}-spacecrop-rg"

echo "Removendo Resource Group ${RESOURCE_GROUP} (todos os recursos junto)..."
az group delete --name "$RESOURCE_GROUP" --yes --no-wait
echo "Solicitação de remoção enviada."
