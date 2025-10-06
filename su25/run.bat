@echo off
echo ========================================
echo    BanHangRong Application Starter
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
echo Starting Spring Boot application...
echo Please wait...

java -cp "target\classes;target\dependency\*" banhangrong.su25.Su25Application

echo.
echo Application stopped.
pause
