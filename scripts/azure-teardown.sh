#!/bin/bash
# =====================================================================
# SpaceCrop - Remove toda a infraestrutura do checkpoint
# =====================================================================
set -euo pipefail

RM="561413"
RESOURCE_GROUP="rm${RM}-spacecrop-rg"

echo "Removendo Resource Group ${RESOURCE_GROUP}..."

if ! az group show --name "$RESOURCE_GROUP" --output none 2>/dev/null; then
  echo "Resource Group não encontrado. Nada a remover."
  exit 0
fi

# Sem --no-wait: o script só termina quando a remoção estiver concluída.
az group delete \
  --name "$RESOURCE_GROUP" \
  --yes

echo "Resource Group ${RESOURCE_GROUP} removido com sucesso."
