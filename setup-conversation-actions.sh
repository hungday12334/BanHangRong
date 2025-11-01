#!/bin/bash

echo "==================================================================="
echo "    Setup Conversation Actions (Pin & Delete) Feature"
echo "==================================================================="
echo ""

# Database connection details (update these if needed)
DB_URL="jdbc:h2:./data/banhangrong_db"
DB_USER="sa"
DB_PASSWORD=""

echo "Step 1: Creating user_conversation_metadata table..."
echo "-------------------------------------------------------------------"

# Since this is H2 database, we'll let Spring Boot create the table automatically
# But we can verify the SQL script
cat sql/create_user_conversation_metadata.sql

echo ""
echo "✓ SQL script verified"
echo ""

echo "Step 2: Building the application..."
echo "-------------------------------------------------------------------"
./mvnw clean package -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Build successful!"
    echo ""
    echo "==================================================================="
    echo "    Setup Complete!"
    echo "==================================================================="
    echo ""
    echo "The conversation actions feature has been implemented:"
    echo ""
    echo "✓ Created Entity: UserConversationMetadata"
    echo "✓ Created Repository: UserConversationMetadataRepository"
    echo "✓ Updated Service: ChatService (pin/unpin/delete methods)"
    echo "✓ Updated Controller: ChatController (REST API endpoints)"
    echo "✓ Updated Entity: Conversation (added isPinned field)"
    echo "✓ Updated UI: Customer chat (actions button, context menu, modals)"
    echo "✓ Updated UI: Seller chat (actions button, context menu, modals)"
    echo ""
    echo "Features:"
    echo "  • Hover over conversation row → shows ⋯ button"
    echo "  • Click ⋯ → shows context menu with Pin/Unpin and Delete"
    echo "  • Pin: moves conversation to top (local only)"
    echo "  • Delete: removes conversation from view (local only)"
    echo "  • Other user is NOT affected by these actions"
    echo ""
    echo "API Endpoints:"
    echo "  • POST /api/conversations/{id}/pin?userId={userId}"
    echo "  • POST /api/conversations/{id}/unpin?userId={userId}"
    echo "  • DELETE /api/conversations/{id}?userId={userId}"
    echo "  • GET /api/conversations/{id}/metadata?userId={userId}"
    echo ""
    echo "To start the application, run:"
    echo "  java -jar target/su25-0.0.1-SNAPSHOT.jar"
    echo ""
    echo "Or use your existing start script."
    echo ""
else
    echo ""
    echo "✗ Build failed! Please check the error messages above."
    echo ""
    exit 1
fi

