#!/bin/bash
# /opt/health-check.sh - アプリケーションヘルスチェック
# 用途: cron で定期実行、または監視ツールから呼び出し

set -e

HEALTH_CHECK_URL="http://localhost:8080/final-tax-return-1.0-SNAPSHOT/home"
TIMEOUT=10
MAX_RETRIES=3
RETRY_INTERVAL=2

# ============================================================
# ログ関数
# ============================================================
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" >> /var/log/health-check.log
}

# ============================================================
# 関数: HTTP ヘルスチェック
# ============================================================
check_http() {
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" \
    --connect-timeout "$TIMEOUT" \
    --max-time "$TIMEOUT" \
    "$HEALTH_CHECK_URL")
  
  echo "$HTTP_CODE"
}

# ============================================================
# 関数: MySQL 接続確認
# ============================================================
check_mysql() {
  if [ -f /opt/db-credentials.env ]; then
    set -a
    source /opt/db-credentials.env
    set +a
  fi
  
  if mysql -u "${DB_USER:-taxapp}" -p"${DB_PASS:-changeme}" -h localhost \
    -e "SELECT 1" >/dev/null 2>&1; then
    echo "UP"
  else
    echo "DOWN"
  fi
}

# ============================================================
# 関数: Tomcat プロセス確認
# ============================================================
check_tomcat() {
  if pgrep -f "catalina.sh" > /dev/null; then
    echo "UP"
  else
    echo "DOWN"
  fi
}

# ============================================================
# ヘルスチェック実行
# ============================================================
HTTP_STATUS=""
for ((i=1; i<=MAX_RETRIES; i++)); do
  HTTP_STATUS=$(check_http)
  if [ "$HTTP_STATUS" = "200" ]; then
    break
  fi
  if [ $i -lt $MAX_RETRIES ]; then
    sleep "$RETRY_INTERVAL"
  fi
done

MYSQL_STATUS=$(check_mysql)
TOMCAT_STATUS=$(check_tomcat)

# ============================================================
# ステータス判定
# ============================================================
OVERALL_STATUS="UP"

if [ "$HTTP_STATUS" != "200" ]; then
  OVERALL_STATUS="DOWN"
fi

if [ "$MYSQL_STATUS" != "UP" ]; then
  OVERALL_STATUS="DOWN"
fi

if [ "$TOMCAT_STATUS" != "UP" ]; then
  OVERALL_STATUS="DOWN"
fi

# ============================================================
# ログ出力
# ============================================================
log "HTTP Status: $HTTP_STATUS"
log "MySQL Status: $MYSQL_STATUS"
log "Tomcat Status: $TOMCAT_STATUS"
log "Overall Status: $OVERALL_STATUS"

# ============================================================
# 終了コード
# ============================================================
if [ "$OVERALL_STATUS" = "UP" ]; then
  exit 0
else
  exit 1
fi
