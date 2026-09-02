#!/bin/bash
# =========================================================
# Entrypoint customizado do banco SpaceCrop (Oracle Free)
#
# - Sobe o Oracle normalmente, usando o entrypoint oficial
#   da imagem gvenzl/oracle-free (datafiles ficam no disco
#   LOCAL do container - rápido e 100% compatível)
# - Em paralelo, a cada poucos minutos, gera um backup
#   (Data Pump) e grava na Conta de Armazenamento montada
#   em /mnt/backup (Azure File Share) - é aqui que a
#   persistência em nuvem acontece
# =========================================================
set -e

BACKUP_DIR="/mnt/backup"
DUMP_FILE="spacecrop_backup.dmp"
LOG_FILE="spacecrop_backup.log"
SCHEMA="SPACECROP"

# Sobe o Oracle usando o entrypoint oficial da imagem (fica em foreground)
container-entrypoint.sh &
ORACLE_PID=$!

# --------- Rotina de backup em background ---------
(
  # dá tempo da primeira inicialização do banco terminar
  sleep 120

  while kill -0 "$ORACLE_PID" 2>/dev/null; do
    if [ -d "$BACKUP_DIR" ] && [ -n "${APP_USER_PASSWORD:-}" ]; then

      sqlplus -s "${APP_USER}/${APP_USER_PASSWORD}@//localhost:1521/FREEPDB1" <<SQL || true
CREATE OR REPLACE DIRECTORY spacecrop_backup_dir AS '${BACKUP_DIR}';
GRANT READ, WRITE ON DIRECTORY spacecrop_backup_dir TO ${APP_USER};
EXIT;
SQL

      expdp "${APP_USER}/${APP_USER_PASSWORD}@//localhost:1521/FREEPDB1" \
        directory=spacecrop_backup_dir \
        dumpfile="${DUMP_FILE}" \
        logfile="${LOG_FILE}" \
        schemas="${SCHEMA}" \
        reuse_dumpfiles=y \
        || echo "[backup] expdp falhou nesta rodada - tentando de novo no próximo ciclo"

      echo "[backup] snapshot gravado em ${BACKUP_DIR}/${DUMP_FILE} - $(date)"
    fi
    sleep 300
  done
) &

wait "$ORACLE_PID"
