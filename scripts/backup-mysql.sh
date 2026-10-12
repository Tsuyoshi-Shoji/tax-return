#!/bin/bash
# /opt/backup-mysql.sh - MySQL バックアップスクリプト
# 用途: 毎日自動実行（cron）、または手動実行
# 実行ユーザー: root

set -e
exec 2>&1

BACKUP_DIR="/data/mysql/backups"
BACKUP_RETENTION_DAYS=7
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
BACKUP_FILE="$BACKUP_DIR/backup_$TIMESTAMP.sql.gz"
LOG_FILE="/var/log/mysql-backup.log"

# ============================================================
# ログ出力関数
# ============================================================
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$LOG_FILE"
}

log_error() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $*" | tee -a "$LOG_FILE"
}

# ============================================================
# エラーハンドリング
# ============================================================
trap 'log_error "Backup failed at line $LINENO"; exit 1' ERR

# ============================================================
# バックアップディレクトリ確認
# ============================================================
if [ ! -d "$BACKUP_DIR" ]; then
  log "Creating backup directory: $BACKUP_DIR"
  mkdir -p "$BACKUP_DIR"
  chmod 700 "$BACKUP_DIR"
fi

# ============================================================
# MySQL バックアップ実行
# ============================================================
log "Starting MySQL backup..."

# 認証情報ファイルから読み込み
if [ -f /opt/db-credentials.env ]; then
  set -a
  source /opt/db-credentials.env
  set +a
fi

# バックアップコマンド実行
if mysqldump \
  -u "${DB_USER:-taxapp}" \
  -p"${DB_PASS:-changeme}" \
  -h localhost \
  --single-transaction \
  --routines \
  --triggers \
  --events \
  "${DB_NAME:-taxtax}" 2>/dev/null | gzip > "$BACKUP_FILE"; then
  
  BACKUP_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
  log "✓ Backup completed: $BACKUP_FILE ($BACKUP_SIZE)"
else
  log_error "Failed to create backup"
  exit 1
fi

# ============================================================
# バックアップ完全性チェック
# ============================================================
log "Verifying backup integrity..."

if gzip -t "$BACKUP_FILE" 2>&1 | tee -a "$LOG_FILE"; then
  log "✓ Backup file is valid"
else
  log_error "Backup file is corrupted"
  rm -f "$BACKUP_FILE"
  exit 1
fi

# ============================================================
# 古いバックアップ削除
# ============================================================
log "Cleaning up old backups (keeping last $BACKUP_RETENTION_DAYS days)..."

DELETED_COUNT=0
while IFS= read -r old_backup; do
  if [ -f "$old_backup" ]; then
    log "Deleting: $(basename "$old_backup")"
    rm -f "$old_backup"
    ((DELETED_COUNT++))
  fi
done < <(find "$BACKUP_DIR" -name "backup_*.sql.gz" -type f -mtime +"$BACKUP_RETENTION_DAYS")

log "Deleted $DELETED_COUNT old backup(s)"

# ============================================================
# バックアップ一覧表示
# ============================================================
log "Current backups:"
ls -lh "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null | awk '{print "  " $9 " (" $5 ")"}' | tee -a "$LOG_FILE"

log "Backup process completed successfully"
