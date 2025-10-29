#!/bin/bash

# Load environment variables from .env file
if [ -f .env ]; then
    export $(cat .env | grep -v '^#' | xargs)
    echo "✓ Environment variables loaded from .env file"
    echo "  DB_HOST: $DB_HOST"
    echo "  DB_NAME: $DB_NAME"
    echo "  DB_USERNAME: $DB_USERNAME"
    echo ""
else
    echo "⚠ Warning: .env file not found"
    exit 1
fi

# Build the application
echo "Building application..."
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Build successful!"
    echo ""
    echo "Starting application with online database..."
    java -jar target/su25-0.0.1-SNAPSHOT.jar
else
    echo ""
    echo "✗ Build failed!"
    exit 1
fi

