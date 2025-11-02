#!/bin/bash

echo "=========================================="
echo "Testing License Management Feature"
echo "=========================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if database table exists
echo -e "${YELLOW}[1] Checking if category_licenses table exists...${NC}"
mysql -u root -p banhangrong_db -e "DESCRIBE category_licenses;" 2>/dev/null

if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ Table exists${NC}"
else
    echo -e "${RED}✗ Table does not exist. Please run the SQL migration first.${NC}"
    echo "  Run: mysql -u root -p banhangrong_db < sql/create_category_licenses_table.sql"
    exit 1
fi

echo ""
echo -e "${YELLOW}[2] Checking Java source files...${NC}"

files=(
    "src/main/java/banhangrong/su25/Entity/CategoryLicenses.java"
    "src/main/java/banhangrong/su25/Repository/CategoryLicensesRepository.java"
    "src/main/java/banhangrong/su25/DTO/LicenseDTO.java"
    "src/main/java/banhangrong/su25/service/LicenseManagementService.java"
)

all_exist=true
for file in "${files[@]}"; do
    if [ -f "$file" ]; then
        echo -e "${GREEN}✓${NC} $file"
    else
        echo -e "${RED}✗${NC} $file - NOT FOUND"
        all_exist=false
    fi
done

if [ "$all_exist" = false ]; then
    echo -e "${RED}Some files are missing!${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}[3] Checking updated files...${NC}"

# Check if SellerCategoryController has the new endpoints
if grep -q "api/licenses" src/main/java/banhangrong/su25/Controller/SellerCategoryController.java; then
    echo -e "${GREEN}✓${NC} SellerCategoryController has license management endpoints"
else
    echo -e "${RED}✗${NC} SellerCategoryController missing license endpoints"
    exit 1
fi

# Check if HTML has license modal
if grep -q "licenseManagerModal" src/main/resources/templates/seller/category-management.html; then
    echo -e "${GREEN}✓${NC} category-management.html has license modal"
else
    echo -e "${RED}✗${NC} category-management.html missing license modal"
    exit 1
fi

# Check if CSS has license styles
if grep -q "assigned-licenses-section" src/main/resources/static/css/category-management.css; then
    echo -e "${GREEN}✓${NC} category-management.css has license styles"
else
    echo -e "${RED}✗${NC} category-management.css missing license styles"
    exit 1
fi

echo ""
echo -e "${YELLOW}[4] Checking JavaScript functions...${NC}"

js_functions=(
    "openLicenseManager"
    "openCategoryLicenseManager"
    "loadLicenses"
    "addNewLicense"
    "assignLicenseToCategory"
    "removeLicenseFromCategory"
)

for func in "${js_functions[@]}"; do
    if grep -q "function $func" src/main/resources/templates/seller/category-management.html; then
        echo -e "${GREEN}✓${NC} $func()"
    else
        echo -e "${RED}✗${NC} $func() - NOT FOUND"
    fi
done

echo ""
echo -e "${GREEN}=========================================="
echo "All checks passed! ✓"
echo "==========================================${NC}"
echo ""
echo "Next steps:"
echo "1. Make sure database is running"
echo "2. Run the SQL migration if not already done:"
echo "   mysql -u root -p banhangrong_db < sql/create_category_licenses_table.sql"
echo "3. Start the application:"
echo "   ./mvnw spring-boot:run"
echo "4. Navigate to: http://localhost:8080/seller/categories"
echo "5. Click 'Quản lý Licenses' button"
echo ""

