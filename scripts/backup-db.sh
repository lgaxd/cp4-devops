#!/usr/bin/env bash

set -euo pipefail

RM="561413"
RESOURCE_GROUP="${RM}-spacecrop-rg"
DB_CONTAINER="${RM}-spacecrop-db"
STORAGE_ACCOUNT="${RM}spacecropstorage"
FILE_SHARE="spacecrop-backup"

BACKUP_FILE="spacecrop-backup-$(date +%Y%m%d-%H%M%S).dmp"

echo "Criando backup Oracle..."

az container exec \
  --resource-group "$RESOURCE_GROUP" \
  --name "$DB_CONTAINER" \
  --exec-command \
"bash -c '
sqlplus -s / as sysdba <<EOF
CREATE OR REPLACE DIRECTORY BACKUP_DIR AS '\''/backup'\'';
GRANT READ, WRITE ON DIRECTORY BACKUP_DIR TO SPACECRP;
EXIT;
EOF

expdp SPACECRP/SpaceCropDb123!@FREEPDB1 \
  DIRECTORY=BACKUP_DIR \
  DUMPFILE=$BACKUP_FILE \
  LOGFILE=spacecrop-export.log \
  SCHEMAS=SPACECRP
'"

echo ""
echo "Backup criado no File Share:"
echo "$FILE_SHARE/$BACKUP_FILE"

echo ""
echo "Arquivos no Storage Account:"

STORAGE_KEY=$(az storage account keys list \
  --resource-group "$RESOURCE_GROUP" \
  --account-name "$STORAGE_ACCOUNT" \
  --query "[0].value" \
  --output tsv)

az storage file list \
  --account-name "$STORAGE_ACCOUNT" \
  --account-key "$STORAGE_KEY" \
  --share-name "$FILE_SHARE" \
  --output table