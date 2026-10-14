#!/bin/bash
set -euo pipefail

EBS_DEVICE="${EBS_DEVICE:-/dev/xvdf}"
DATA_DIR="${DATA_DIR:-/data/mysql}"
TOMCAT_HOME="${TOMCAT_HOME:-/opt/tomcat}"
DB_USER="${DB_USER:-taxapp}"
DB_NAME="${DB_NAME:-taxtax}"
SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

echo "========================================"
echo "EC2 Environment Setup - MySQL + Tomcat"
echo "========================================"

prepare_ebs_volume() {
  echo "[0/6] Preparing EBS volume..."

  sudo mkdir -p "$DATA_DIR"

  if mountpoint -q "$DATA_DIR"; then
    echo "✓ $DATA_DIR is already mounted"
    return
  fi

  DATA_DEVICE="$EBS_DEVICE"
  if [ ! -b "$DATA_DEVICE" ]; then
    ROOT_SOURCE=$(findmnt -no SOURCE /)
    ROOT_DISK=$(lsblk -no PKNAME "$ROOT_SOURCE" 2>/dev/null || true)
    if [ -n "$ROOT_DISK" ]; then
      DATA_DEVICE=$(lsblk -dpno NAME,SIZE,TYPE,MOUNTPOINT | awk -v root="/dev/$ROOT_DISK" '$3=="disk" && $1 != root && $4=="" {print $1; exit}')
    else
      DATA_DEVICE=$(lsblk -dpno NAME,SIZE,TYPE,MOUNTPOINT | awk '$3=="disk" && $4=="" {print $1; exit}')
    fi
  fi

  if [ -z "${DATA_DEVICE:-}" ] || [ ! -b "$DATA_DEVICE" ]; then
    echo "❌ EBS volume not found. Attach a 20GiB EBS volume and retry."
    lsblk
    exit 1
  fi

  FS_TYPE=$(sudo blkid -o value -s TYPE "$DATA_DEVICE" 2>/dev/null || true)
  if [ -z "$FS_TYPE" ]; then
    echo "Formatting $DATA_DEVICE as ext4..."
    sudo mkfs.ext4 -F "$DATA_DEVICE"
  fi

  UUID=$(sudo blkid -s UUID -o value "$DATA_DEVICE")
  if ! grep -q "$UUID" /etc/fstab; then
    echo "UUID=$UUID $DATA_DIR ext4 defaults,nofail 0 2" | sudo tee -a /etc/fstab > /dev/null
  fi

  sudo mount "$DATA_DEVICE" "$DATA_DIR"
}

prepare_ebs_volume

echo "[1/6] Installing dependencies..."
sudo dnf update -y > /dev/null
sudo dnf install -y \
  java-21-amazon-corretto-devel \
  mariadb105-server \
  mariadb105 \
  maven \
  cronie \
  wget tar git \
  > /dev/null

sudo chown -R mysql:mysql "$DATA_DIR"
sudo chmod 750 "$DATA_DIR"

echo "[2/6] Installing Tomcat..."
if [ ! -d /opt/tomcat ]; then
  cd /tmp
  wget -q https://archive.apache.org/dist/tomcat/tomcat-11/v11.0.21/bin/apache-tomcat-11.0.21.tar.gz
  sudo tar xzf apache-tomcat-11.0.21.tar.gz -C /opt
  sudo mv /opt/apache-tomcat-11.0.21 /opt/tomcat
  sudo useradd -m -s /bin/bash tomcat || true
  sudo chown -R tomcat:tomcat /opt/tomcat
  rm apache-tomcat-11.0.21.tar.gz
fi

echo "[3/6] Setting up MySQL..."
sudo systemctl stop mariadb 2>/dev/null || true
sudo systemctl stop mysqld 2>/dev/null || true

sudo mkdir -p /var/log/mysql /var/run/mysqld
sudo chown -R mysql:mysql /var/log/mysql /var/run/mysqld
sudo chmod 755 /var/log/mysql /var/run/mysqld

sudo tee /etc/my.cnf.d/mysql-custom.cnf > /dev/null <<'MYSQL_EOF'
[mysqld]
datadir=/data/mysql
socket=/data/mysql/mysql.sock
pid-file=/var/run/mysqld/mysqld.pid
symbolic-links=0

default-storage-engine=InnoDB
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci

log-error=/var/log/mysql/error.log
slow_query_log=1
slow_query_log_file=/var/log/mysql/slow-query.log
long_query_time=2

max_connections=50
innodb_buffer_pool_size=128M

[mysqld_safe]
log-error=/var/log/mysql/error.log
pid-file=/var/run/mysqld/mysqld.pid
MYSQL_EOF

if [ ! -d "$DATA_DIR/mysql" ] && [ ! -f "$DATA_DIR/ibdata1" ]; then
  if command -v mariadb-install-db >/dev/null 2>&1; then
    sudo mariadb-install-db --user=mysql --datadir="$DATA_DIR" >/dev/null
  elif command -v mysql_install_db >/dev/null 2>&1; then
    sudo mysql_install_db --user=mysql --datadir="$DATA_DIR" >/dev/null
  fi
fi

sudo systemctl enable mariadb 2>/dev/null || true
sudo systemctl enable mysqld 2>/dev/null || true

sudo systemctl start mariadb 2>/dev/null || sudo systemctl start mysqld
sleep 5

ROOT_PASS=$(openssl rand -base64 16)
sudo mysql <<SQL
ALTER USER 'root'@'localhost' IDENTIFIED BY '$ROOT_PASS';
DELETE FROM mysql.user WHERE User='root' AND Host NOT IN ('localhost');
DELETE FROM mysql.user WHERE User='';
DELETE FROM mysql.db WHERE Db='test' OR Db='test\_%';
FLUSH PRIVILEGES;
SQL

DB_USER="taxapp"
DB_PASS=$(openssl rand -base64 16)
DB_NAME="taxtax"

sudo mysql -u root -p"$ROOT_PASS" <<SQL
CREATE DATABASE IF NOT EXISTS $DB_NAME CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER '$DB_USER'@'localhost' IDENTIFIED BY '$DB_PASS';
GRANT ALL PRIVILEGES ON $DB_NAME.* TO '$DB_USER'@'localhost';
FLUSH PRIVILEGES;
SQL

sudo tee /opt/db-credentials.env > /dev/null <<EOF
DB_USER=$DB_USER
DB_PASS=$DB_PASS
DB_NAME=$DB_NAME
DB_HOST=localhost
ROOT_PASS=$ROOT_PASS
EOF
sudo chmod 600 /opt/db-credentials.env
sudo chown root:tomcat /opt/db-credentials.env
sudo chmod 640 /opt/db-credentials.env

echo "[4/6] Configuring Tomcat..."
sudo mkdir -p "$TOMCAT_HOME"/{webapps,logs,work,temp,run}
sudo chown -R tomcat:tomcat "$TOMCAT_HOME"/{webapps,logs,work,temp,run}
sudo install -o tomcat -g tomcat -m 755 "$SCRIPT_DIR/tomcat-setenv.sh" "$TOMCAT_HOME/bin/setenv.sh"
sudo install -o root -g tomcat -m 640 "$SCRIPT_DIR/private-access/tomcat-server.xml" "$TOMCAT_HOME/conf/server.xml"
sudo install -o root -g tomcat -m 640 "$SCRIPT_DIR/private-access/tomcat-context.xml" "$TOMCAT_HOME/conf/context.xml"
sudo chgrp -R tomcat "$TOMCAT_HOME/bin" "$TOMCAT_HOME/conf" "$TOMCAT_HOME/lib"
sudo chmod 750 "$TOMCAT_HOME/bin" "$TOMCAT_HOME/conf" "$TOMCAT_HOME/lib"
sudo find "$TOMCAT_HOME/bin" -type f -name '*.sh' -exec chmod 750 {} +
sudo find "$TOMCAT_HOME/bin" "$TOMCAT_HOME/conf" "$TOMCAT_HOME/lib" -type f ! -name '*.sh' -exec chmod 640 {} +

echo "[5/6] Installing deployment scripts..."
sudo mkdir -p /opt/backups/war
sudo chown root:root /opt/backups/war
sudo chmod 755 /opt/backups/war

sudo tee /opt/deploy.log > /dev/null
sudo chown root:adm /opt/deploy.log
sudo chmod 640 /opt/deploy.log

echo "[6/6] Setting up monitoring & backups..."
sudo mkdir -p /data/mysql/backups
sudo chown mysql:mysql /data/mysql/backups
sudo chmod 700 /data/mysql/backups

sudo tee /opt/backup-mysql.sh > /dev/null <<'BACKUP_EOF'
#!/bin/bash
set -euo pipefail
source /opt/db-credentials.env

BACKUP_DIR=/data/mysql/backups
TIMESTAMP=$(date +%Y%m%d-%H%M%S)
BACKUP_FILE=$BACKUP_DIR/backup-$TIMESTAMP.sql.gz

mysqldump -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" 2>/dev/null | gzip > "$BACKUP_FILE"
chmod 600 "$BACKUP_FILE"

find "$BACKUP_DIR" -name "backup-*.sql.gz" -mtime +7 -delete

echo "[$(date '+%Y-%m-%d %H:%M:%S')] Backup completed: $BACKUP_FILE" >> /var/log/mysql-backup.log
BACKUP_EOF

sudo chmod +x /opt/backup-mysql.sh
sudo chown root:root /opt/backup-mysql.sh

(sudo crontab -l 2>/dev/null || true; echo "0 3 * * * /opt/backup-mysql.sh") | sudo crontab -
sudo systemctl enable crond 2>/dev/null || true
sudo systemctl start crond 2>/dev/null || true

if command -v firewall-cmd &> /dev/null; then
  sudo firewall-cmd --permanent --remove-port=8080/tcp
  sudo firewall-cmd --permanent --remove-port=3306/tcp
  sudo firewall-cmd --reload
fi

echo ""
echo "========================================"
echo "✅ Setup Complete!"
echo "========================================"
echo ""
echo "EBS mounted at: $DATA_DIR"
echo "Database Credentials saved to: /opt/db-credentials.env"
echo "MySQL User: $DB_USER"
echo "MySQL Database: $DB_NAME"
echo ""
echo "Next steps:"
echo "1. Configure GitHub Secrets in repository"
echo "2. Configure WireGuard-only HTTPS access: docs/PRIVATE-ACCESS.md"
echo "3. Push to main branch to trigger deployment"
echo ""
echo "Useful commands:"
echo "  tail -f /var/log/deploy.log"
echo "  tail -f /opt/tomcat/logs/catalina.out"
echo "  tail -f /var/log/mysql/error.log"
echo ""
