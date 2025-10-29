#!/bin/bash

echo "================================"
echo "DEBUG BUILD SCRIPT"
echo "================================"
echo ""

# Hiển thị thông tin hệ thống
echo "1. Kiểm tra Java version..."
javac -version 2>&1 || echo "javac không tìm thấy"
echo ""

echo "2. Maven version..."
mvn -v | head -5
echo ""

echo "3. JAVA_HOME..."
echo "JAVA_HOME=$JAVA_HOME"
echo ""

echo "4. Xóa target folder..."
rm -rf target/
echo "✓ Đã xóa target/"
echo ""

echo "5. Compile với annotation processing disabled..."
mvn clean compile -DskipTests
RESULT=$?

echo ""
echo "================================"
if [ $RESULT -eq 0 ]; then
    echo "✅ BUILD THÀNH CÔNG!"
    echo ""
    echo "Chạy ứng dụng:"
    echo "  mvn spring-boot:run"
else
    echo "❌ BUILD THẤT BẠI!"
    echo ""
    echo "Lỗi có thể do:"
    echo "- Lombok không tương thích với Java version"
    echo "- Cần cài đặt Java 17 hoặc 21"
fi
echo "================================"

