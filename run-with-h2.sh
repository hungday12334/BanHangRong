#!/bin/bash

echo "🚀 Starting application with H2 database..."

# Run với H2 local database
java -jar target/su25-0.0.1-SNAPSHOT.jar \
  --spring.datasource.url=jdbc:h2:file:./data/banhangrong_db \
  --spring.datasource.username=sa \
  --spring.datasource.password= \
  --spring.datasource.driver-class-name=org.h2.Driver \
  --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect \
  --spring.h2.console.enabled=true \
  --spring.h2.console.path=/h2-console

echo "✅ Application stopped"

