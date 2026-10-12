#!/bin/bash
set -euo pipefail

echo "========================================"
echo "EC2 Environment Setup - MySQL + Tomcat"
echo "========================================"

if [ ! -d /data/mysql ]; then
  echo "❌ /data/mysql not found. Please mount EBS volume first:"
  echo "   sudo mkfs -t ext4 /dev/nvme1n1"
  echo "   sudo mkdir -p /data/mysql"
  echo "   sudo mount /dev/nvme1n1 /data/mysql"
  exit 1
fi

echo "[1/6] Installing dependencies..."
sudo yum update -y > /dev/null
sudo yum install -y \
  java-21-amazon-corretto-devel \
  mysql-server \
  wget tar git curl \
  > /dev/null

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
sudo systemctl stop mysqld || true

sudo mkdir -p /data/mysql
sudo chown -R mysql:mysql /data/mysql
sudo chmod 750 /data/mysql

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

max_connections=100
innodb_buffer_pool_size=256M

[mysqld_safe]
log-error=/var/log/mysql/error.log
pid-file=/var/run/mysqld/mysqld.pid
MYSQL_EOF

sudo systemctl enable mysqld
sudo systemctl start mysqld
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
sudo chown root:root /opt/db-credentials.env

echo "[4/6] Configuring Tomcat..."
sudo tee /opt/tomcat/bin/setenv.sh > /dev/null <<'TOMCAT_EOF'
#!/bin/bash
if [ -f /opt/db-credentials.env ]; then
  source /opt/db-credentials.env
else
  echo "ERROR: /opt/db-credentials.env not found" >&2
  exit 1
fi

export CATALINA_OPTS="
  -Dspring.datasource.url=jdbc:mysql://localhost:3306/$DB_NAME?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true
  -Dspring.datasource.username=$DB_USER
  -Dspring.datasource.password=$DB_PASS
  -Dserver.port=8080
  -Dspring.profiles.active=production
"

export JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -Djava.awt.headless=true"
TOMCAT_EOF

sudo chmod +x /opt/tomcat/bin/setenv.sh
sudo chown tomcat:tomcat /opt/tomcat/bin/setenv.sh

echo "[5/6] Installing deployment scripts..."
sudo mkdir -p /opt/backups/war
sudo chown root:root /opt/backups/war
sudo chmod 755 /opt/backups/war

sudo tee /opt/deploy.log > /dev/null
sudo chmod 666 /opt/deploy.log

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

if command -v firewall-cmd &> /dev/null; then
  sudo firewall-cmd --permanent --add-service=http > /dev/null 2>&1 || true
  sudo firewall-cmd --permanent --add-service=https > /dev/null 2>&1 || true
  sudo firewall-cmd --permanent --remove-port=3306/tcp > /dev/null 2>&1 || true
  sudo firewall-cmd --reload > /dev/null 2>&1 || true
fi

echo ""
echo "========================================"
echo "✅ Setup Complete!"
echo "========================================"
echo ""
echo "Database Credentials saved to: /opt/db-credentials.env"
echo "MySQL User: $DB_USER"
echo "MySQL Database: $DB_NAME"
echo ""
echo "Next steps:"
echo "1. Configure GitHub Secrets in repository"
echo "2. Push to main branch to trigger deployment"
echo ""
echo "Useful commands:"
echo "  tail -f /var/log/deploy.log"
echo "  tail -f /opt/tomcat/logs/catalina.out"
echo "  tail -f /var/log/mysql/error.log"
echo ""
