#!/bin/bash
# MySQL バックアップから復旧するスクリプト
# 用途: バックアップファイルから MySQL を復旧
# 実行ユーザー: root

set -e
exec 2>&1

# ============================================================
# 変数定義
# ============================================================
BACKUP_DIR="/data/mysql/backups"
LOG_FILE="/var/log/mysql-restore.log"
TIMESTAMP=$(date '+%Y%m%d_%H%M%S')

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
# 使用方法表示
# ============================================================
usage() {
  cat <<EOF
Usage: $(basename "$0") [OPTIONS]

Options:
  -f FILE       Backup file to restore (default: latest)
  -d DATABASE   Database to restore (default: taxtax)
  -h            Show this help message

Examples:
  # Latest backup から復旧
  $(basename "$0")

  # 特定のバックアップファイルから復旧
  $(basename "$0") -f /data/mysql/backups/backup_20261011_030000.sql.gz

  # 別のデータベースへ復旧
  $(basename "$0") -d taxtax_restore -f backup_latest.sql.gz

EOF
  exit 1
}

# ============================================================
# コマンドラインオプション解析
# ============================================================
BACKUP_FILE=""
DATABASE="taxtax"

while getopts "f:d:h" opt; do
  case $opt in
    f) BACKUP_FILE="$OPTARG" ;;
    d) DATABASE="$OPTARG" ;;
    h) usage ;;
    *) usage ;;
  esac
done

# ============================================================
# バックアップファイル選択
# ============================================================
if [ -z "$BACKUP_FILE" ]; then
  log "Backup file not specified, using latest..."
  BACKUP_FILE=$(ls -t "$BACKUP_DIR"/backup_*.sql.gz 2>/dev/null | head -1)
  
  if [ -z "$BACKUP_FILE" ]; then
    log_error "No backup files found in $BACKUP_DIR"
    exit 1
  fi
fi

if [ ! -f "$BACKUP_FILE" ]; then
  log_error "Backup file not found: $BACKUP_FILE"
  exit 1
fi

log "Using backup file: $BACKUP_FILE"
log "Target database: $DATABASE"
log "File size: $(du -h "$BACKUP_FILE" | cut -f1)"

# ============================================================
# バックアップ完全性チェック
# ============================================================
log "Verifying backup file integrity..."

if ! gzip -t "$BACKUP_FILE" 2>&1 | tee -a "$LOG_FILE"; then
  log_error "Backup file is corrupted or invalid"
  exit 1
fi

log "✓ Backup file is valid"

# ============================================================
# 認証情報取得
# ============================================================
if [ -f /opt/db-credentials.env ]; then
  set -a
  source /opt/db-credentials.env
  set +a
fi

DB_USER="${DB_USER:-taxapp}"
DB_PASS="${DB_PASS:-changeme}"

# ============================================================
# 確認プロンプト
# ============================================================
log ""
log "=================================================="
log "復旧対象:"
log "  バックアップファイル: $BACKUP_FILE"
log "  対象データベース: $DATABASE"
log "=================================================="
log ""

read -p "本当に復旧しますか? (yes/no): " -r CONFIRM
if [ "$CONFIRM" != "yes" ]; then
  log "Restore cancelled"
  exit 0
fi

# ============================================================
# 既存データベースバックアップ
# ============================================================
log "Backing up current database..."

CURRENT_BACKUP="$BACKUP_DIR/pre-restore_${TIMESTAMP}.sql.gz"

if mysqldump -u "$DB_USER" -p"$DB_PASS" -h localhost \
  --single-transaction --routines --triggers --events "$DATABASE" 2>/dev/null \
  | gzip > "$CURRENT_BACKUP"; then
  
  log "✓ Current database backed up to: $CURRENT_BACKUP"
else
  log "Warning: Could not backup current database (may not exist yet)"
fi

# ============================================================
# データベース再作成
# ============================================================
log "Dropping and recreating database..."

mysql -u "$DB_USER" -p"$DB_PASS" -h localhost <<EOF
DROP DATABASE IF EXISTS \`$DATABASE\`;
CREATE DATABASE \`$DATABASE\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
EOF

log "✓ Database recreated"

# ============================================================
# バックアップから復旧
# ============================================================
log "Restoring from backup..."

if gunzip < "$BACKUP_FILE" \
  | mysql -u "$DB_USER" -p"$DB_PASS" -h localhost "$DATABASE" 2>&1 | tee -a "$LOG_FILE"; then
  
  log "✓ Restore completed successfully"
else
  log_error "Restore failed"
  exit 1
fi

# ============================================================
# 復旧後の検証
# ============================================================
log "Verifying restored database..."

TABLE_COUNT=$(mysql -u "$DB_USER" -p"$DB_PASS" -h localhost "$DATABASE" \
  -se "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DATABASE';" 2>/dev/null)

log "Restored table count: $TABLE_COUNT"

if [ "$TABLE_COUNT" -eq 0 ]; then
  log_error "Restored database is empty"
  exit 1
fi

# ============================================================
# 復旧完了
# ============================================================
log ""
log "=================================================="
log "復旧が完了しました"
log "=================================================="
log "Database: $DATABASE"
log "Table count: $TABLE_COUNT"
log "Timestamp: $(date)"
log ""
log "前の状態は以下に保管されています:"
log "  $CURRENT_BACKUP"
log ""

exit 0
