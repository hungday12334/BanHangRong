@echo off
echo Generating password hash for 123456...
echo.

REM Start the application in background
start /B mvn spring-boot:run

REM Wait for application to start
timeout /t 30 /nobreak > nul

REM Generate hash using the API
curl -s "http://localhost:8080/api/password-hash/generate?password=123456"

echo.
echo.
echo Copy the hash from above and use it in your SQL update.
echo.
echo Press any key to stop the application...
pause > nul

REM Stop the application
taskkill /f /im java.exe
