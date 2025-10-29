#!/bin/bash

# Script để fix Lombok và build project
# Chạy: bash fix_and_build.sh

echo "================================================"
echo "  FIX LOMBOK VÀ BUILD DỰ ÁN BANHANGRONG"
echo "================================================"
echo ""

# Bước 1: Clean Maven cache
echo "→ Bước 1: Dọn dẹp Maven cache..."
rm -rf ~/.m2/repository/org/projectlombok/lombok
mvn clean

# Bước 2: Force download Lombok mới
echo ""
echo "→ Bước 2: Download Lombok 1.18.34..."
mvn dependency:get -Dartifact=org.projectlombok:lombok:1.18.34

# Bước 3: Build project (skip tests để nhanh)
echo ""
echo "→ Bước 3: Build project..."
mvn clean package -DskipTests

# Kiểm tra kết quả
if [ $? -eq 0 ]; then
    echo ""
    echo "✅ BUILD THÀNH CÔNG!"
    echo ""
    echo "→ Khởi động ứng dụng:"
    echo "   mvn spring-boot:run"
    echo ""
    echo "→ Hoặc:"
    echo "   java -jar target/su25-0.0.1-SNAPSHOT.jar"
    echo ""
else
    echo ""
    echo "❌ BUILD THẤT BẠI!"
    echo ""
    echo "Thử các cách sau:"
    echo "1. Kiểm tra Java version: java -version (cần Java 21)"
    echo "2. Cập nhật Maven: mvn -version"
    echo "3. Xem chi tiết lỗi: mvn clean compile -X"
    echo ""
fi

echo "================================================"

