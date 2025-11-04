#!/bin/bash

echo "================================"
echo "SETUP CHAT TABLES FOR ONLINE DB"
echo "================================"
echo ""

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Loaded environment variables from .env"
else
    echo "❌ Error: .env file not found"
    exit 1
fi

# Find MySQL connector JAR
MYSQL_JAR=$(find ~/.m2/repository/com/mysql/mysql-connector-j -name "*.jar" 2>/dev/null | head -n 1)

if [ -z "$MYSQL_JAR" ]; then
    echo "⚠️  MySQL connector not found in Maven cache, using Maven to compile..."
    # Compile and run using Maven
    javac -cp "$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout):." SetupChatTables.java
    java -cp "$(mvn dependency:build-classpath -q -Dmdep.outputFile=/dev/stdout):." SetupChatTables
else
    echo "✓ Found MySQL connector: $MYSQL_JAR"
    echo ""
    # Compile and run the setup script
    javac -cp "$MYSQL_JAR:." SetupChatTables.java
    java -cp "$MYSQL_JAR:." SetupChatTables
fi

# Clean up compiled class
rm -f SetupChatTables.class

echo ""
echo "Done!"

