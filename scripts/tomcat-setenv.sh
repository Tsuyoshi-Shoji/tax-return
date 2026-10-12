#!/bin/bash
# /opt/tomcat/bin/setenv.sh - Tomcat 環境変数設定

# ============================================================
# Java Memory Configuration
# ============================================================
# インスタンスサイズに応じて調整してください
# t3.medium (4GB RAM): Xms=1024m Xmx=2048m
# t3.large (8GB RAM):  Xms=2048m Xmx=4096m
# t3.xlarge (16GB RAM): Xms=4096m Xmx=8192m

export JAVA_OPTS="
  -Xms1024m
  -Xmx2048m
  -XX:+UseG1GC
  -XX:MaxGCPauseMillis=200
  -XX:+ParallelRefProcEnabled
  -XX:+UnlockDiagnosticVMOptions
  -XX:G1NewCollectionHeuristicPercent=35
  -XX:G1ReservePercent=20
  -XX:InitiatingHeapOccupancyPercent=15
  -Djava.awt.headless=true
  -Dfile.encoding=UTF-8
  -Duser.timezone=UTC
  -Dspring.profiles.active=production
"

# ============================================================
# Database Credentials
# ============================================================
if [ -f /opt/db-credentials.env ]; then
  set -a
  source /opt/db-credentials.env
  set +a
fi

# ============================================================
# Application Configuration
# ============================================================
export CATALINA_OPTS="
  $JAVA_OPTS
  -Dapp.config=application-production.properties
  -Dapp.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}
  -Dapp.datasource.username=${DB_USER}
  -Dapp.datasource.password=${DB_PASS}
  -Dapp.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
  -Dapp.jpa.hibernate.ddl-auto=validate
  -Dapp.jpa.dialect=org.hibernate.dialect.MySQL8Dialect
  -Dlogging.file.name=/opt/tomcat/logs/application.log
"

# ============================================================
# Tomcat Security
# ============================================================
export CATALINA_PID=/opt/tomcat/catalina.pid

# ============================================================
# Optional: Debug Mode
# ============================================================
# Uncomment for development debugging
# export CATALINA_OPTS="$CATALINA_OPTS -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
