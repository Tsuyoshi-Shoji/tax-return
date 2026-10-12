#!/bin/bash
set -euo pipefail
exec 2>&1

DEPLOY_LOG="/var/log/deploy.log"
REPO_DIR="/home/ec2-user/tax-return"
TOMCAT_HOME="/opt/tomcat"
WAR_NAME="final-tax-return-1.0-SNAPSHOT.war"
BACKUP_DIR="/opt/backups/war"
MAX_BACKUPS=5
JAVA_HOME="${JAVA_HOME:-/usr/lib/jvm/java-21-amazon-corretto.aarch64}"
HEALTH_URL="http://localhost:8080/final-tax-return-1.0-SNAPSHOT/login"

log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $*" | tee -a "$DEPLOY_LOG"
}

log_error() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] ERROR: $*" | tee -a "$DEPLOY_LOG"
}

log_section() {
  echo "" | tee -a "$DEPLOY_LOG"
  echo "============================================================" | tee -a "$DEPLOY_LOG"
  echo "$*" | tee -a "$DEPLOY_LOG"
  echo "============================================================" | tee -a "$DEPLOY_LOG"
}

trap 'log_error "Deployment failed at line $LINENO"; exit 1' ERR

# ============================================================
# 初期化
# ============================================================
log_section "Deployment started - $(git -C "$REPO_DIR" rev-parse --short HEAD 2>/dev/null || echo 'unknown')"

mkdir -p "$BACKUP_DIR"
mkdir -p "$TOMCAT_HOME"/{webapps,logs,work,temp,run}
chown -R tomcat:tomcat "$TOMCAT_HOME/webapps" "$TOMCAT_HOME/logs" "$TOMCAT_HOME/work" "$TOMCAT_HOME/temp" "$TOMCAT_HOME/run"
chgrp -R tomcat "$TOMCAT_HOME/bin" "$TOMCAT_HOME/conf" "$TOMCAT_HOME/lib"
chmod 750 "$TOMCAT_HOME/bin" "$TOMCAT_HOME/conf" "$TOMCAT_HOME/lib"
find "$TOMCAT_HOME/bin" -type f -name '*.sh' -exec chmod 750 {} +
find "$TOMCAT_HOME/bin" "$TOMCAT_HOME/conf" "$TOMCAT_HOME/lib" -type f ! -name '*.sh' -exec chmod 640 {} +
chown root:tomcat /opt/db-credentials.env
chmod 640 /opt/db-credentials.env

# ============================================================
# リポジトリ更新
# ============================================================
log_section "Step 1: Updating repository"

cd "$REPO_DIR"
if ! git diff --quiet || ! git diff --cached --quiet || [ -n "$(git ls-files --others --exclude-standard)" ]; then
  log_error "Repository has uncommitted changes; refusing to reset or clean it"
  exit 1
fi
git fetch origin main
git reset --hard origin/main
git clean -fd

COMMIT_SHA=$(git rev-parse --short HEAD)
log "Repository updated: $COMMIT_SHA"

# ============================================================
# ビルド
# ============================================================
log_section "Step 2: Building application"

cd "$REPO_DIR"
if [ ! -x "$JAVA_HOME/bin/java" ]; then
  log_error "Java 21 was not found at $JAVA_HOME"
  exit 1
fi
JAVA_HOME="$JAVA_HOME" mvn clean package -DskipTests -q 2>&1 | tail -5 >> "$DEPLOY_LOG"

WAR_FILE="$REPO_DIR/target/$WAR_NAME"
if [ ! -f "$WAR_FILE" ]; then
  log_error "WAR file not found: $WAR_FILE"
  exit 1
fi

WAR_SIZE=$(du -h "$WAR_FILE" | cut -f1)
log "✓ WAR built successfully: $WAR_SIZE"

# ============================================================
# 前のバージョンをバックアップ
# ============================================================
log_section "Step 3: Backing up current deployment"

if [ -f "$TOMCAT_HOME/webapps/$WAR_NAME" ]; then
  BACKUP_FILE="$BACKUP_DIR/${WAR_NAME}.$(date +%Y%m%d-%H%M%S).bak"
  cp "$TOMCAT_HOME/webapps/$WAR_NAME" "$BACKUP_FILE"
  log "✓ Current WAR backed up: $BACKUP_FILE"
  
  # 古いバックアップを削除（最新5つを保持）
  ls -t "$BACKUP_DIR"/*.bak 2>/dev/null | tail -n +$((MAX_BACKUPS+1)) | xargs -r rm
fi

# ============================================================
# Tomcat 停止
# ============================================================
log_section "Step 4: Stopping Tomcat"

if pgrep -u tomcat -f "org.apache.catalina.startup.Bootstrap" > /dev/null; then
  log "Stopping Tomcat..."
  runuser -u tomcat -- "$TOMCAT_HOME/bin/catalina.sh" stop 60 -force 2>&1 | tail -3 >> "$DEPLOY_LOG"
  sleep 5
fi

if pgrep -u tomcat -f "org.apache.catalina.startup.Bootstrap" > /dev/null; then
  log_error "Tomcat did not stop cleanly"
  exit 1
fi

log "✓ Tomcat stopped"

# ============================================================
# 旧デプロイをクリーン
# ============================================================
log_section "Step 5: Cleaning old deployment"

rm -f "$TOMCAT_HOME/webapps/$WAR_NAME"
rm -rf "$TOMCAT_HOME/webapps/final-tax-return-1.0-SNAPSHOT"
rm -rf "$TOMCAT_HOME/work/Catalina/localhost/final-tax-return-1.0-SNAPSHOT"

log "✓ Old deployment removed"

# ============================================================
# 新 WAR をデプロイ
# ============================================================
log_section "Step 6: Deploying new WAR"

cp "$WAR_FILE" "$TOMCAT_HOME/webapps/$WAR_NAME"
chown tomcat:tomcat "$TOMCAT_HOME/webapps/$WAR_NAME"
chmod 644 "$TOMCAT_HOME/webapps/$WAR_NAME"

log "✓ WAR deployed"

# ============================================================
# Tomcat 起動
# ============================================================
log_section "Step 7: Starting Tomcat"

rm -f "$TOMCAT_HOME/logs/catalina.out"
cd "$TOMCAT_HOME"
runuser -u tomcat -- "$TOMCAT_HOME/bin/catalina.sh" start 2>&1 | tail -3 >> "$DEPLOY_LOG"

sleep 15

if ! pgrep -f "java.*tomcat" > /dev/null; then
  log_error "Tomcat process not found after startup"
  exit 1
fi

log "✓ Tomcat started"

# ============================================================
# ヘルスチェック
# ============================================================
log_section "Step 8: Health check"

RETRY_COUNT=30
RETRY_INTERVAL=2

for ((i=1; i<=RETRY_COUNT; i++)); do
  HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" || echo "000")
  
  if [ "$HTTP_CODE" = "200" ]; then
    log "✓ Health check passed (HTTP $HTTP_CODE)"
    log_section "✅ Deployment completed successfully"
    log "Commit: $COMMIT_SHA | Time: $(date '+%Y-%m-%d %H:%M:%S')"
    exit 0
  fi
  
  log "  Attempt $i/$RETRY_COUNT: HTTP $HTTP_CODE"
  sleep "$RETRY_INTERVAL"
done

log_error "Application not responding after $RETRY_COUNT attempts"
exit 1
