@echo off
echo Starting BanHangRong Application...
echo.

cd /d "d:\BanHangRong-main\BanHangRong-main\su25"

echo Compiling application...
call mvn clean compile -q

if %ERRORLEVEL% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Starting Spring Boot application...
call mvn spring-boot:run

pause
