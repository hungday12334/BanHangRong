@echo off
echo Starting BanHangRong Application...
echo.

cd /d "D:\BanHangRong-main\BanHangRong-main\su25"

echo Compiling application...
call mvnw.cmd clean compile -q
if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo.
echo Starting application...
echo Application will be available at: http://localhost:8080
echo Press Ctrl+C to stop the application
echo.

java -cp "target/classes;target/dependency/*" banhangrong.su25.Su25Application

pause
