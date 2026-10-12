#!/bin/bash

if [ ! -r /opt/db-credentials.env ]; then
  echo "ERROR: /opt/db-credentials.env is not readable" >&2
  exit 1
fi

set -a
source /opt/db-credentials.env
set +a

if [ -z "${DB_NAME:-}" ] || [ -z "${DB_USER:-}" ] || [ -z "${DB_PASS:-}" ]; then
  echo "ERROR: Database credentials are incomplete" >&2
  exit 1
fi

export JAVA_OPTS="-Xms128m -Xmx256m -XX:+UseG1GC -Djava.awt.headless=true -Dfile.encoding=UTF-8 -Duser.timezone=UTC"
export CATALINA_OPTS="-Dapp.config=application-production.properties -Dapp.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false\&serverTimezone=UTC\&allowPublicKeyRetrieval=true -Dapp.datasource.username=${DB_USER} -Dapp.datasource.password=\${DB_PASS} -Dapp.datasource.driver-class-name=com.mysql.cj.jdbc.Driver -Dapp.jpa.hibernate.ddl-auto=validate -Dapp.jpa.dialect=org.hibernate.dialect.MySQL8Dialect -Dspring.profiles.active=production -Dspring.datasource.hikari.maximumPoolSize=3 -Dlogging.file.name=/opt/tomcat/logs/application.log"
export CATALINA_PID=/opt/tomcat/run/catalina.pid
