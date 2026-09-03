#!/usr/bin/env bash

set -euo pipefail

RM="561413"
RESOURCE_GROUP="${RM}-spacecrop-rg"

echo "=========================================="
echo "SpaceCrop - Azure Teardown"
echo "=========================================="
echo ""
echo "Resource Group: $RESOURCE_GROUP"
echo ""

read -r -p "Digite DELETE para confirmar: " CONFIRM

if [ "$CONFIRM" != "DELETE" ]; then
    echo "Operação cancelada."
    exit 0
fi

az group delete \
  --name "$RESOURCE_GROUP" \
  --yes \
  --no-wait

echo ""
echo "Resource Group enviado para exclusão."