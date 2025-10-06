@echo off
echo ========================================
echo    BanHangRong Application Launcher
echo ========================================
echo.

cd /d "d:\BanHangRong-main\BanHangRong-main\su25"

echo Checking Java installation...
java -version
if %ERRORLEVEL% neq 0 (
    echo ERROR: Java not found! Please install Java 21 or higher.
    pause
    exit /b 1
)

echo.
echo Compiling application...
call mvnw.cmd clean compile -q
if %ERRORLEVEL% neq 0 (
    echo Compilation failed! Trying alternative method...
    echo.
)

echo Starting Spring Boot application...
echo.
echo 📧 Email Configuration:
echo    From: nguyenhung14012k5@gmail.com
echo    App Password: eqpmnlwwgkwiizgbz
echo.
echo 💡 If email fails, check console for reset link!
echo.

call mvnw.cmd spring-boot:run

echo.
echo Application stopped.
pause
