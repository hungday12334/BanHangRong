@echo off
echo ========================================
echo    TEST EMAIL CONFIGURATION
echo ========================================
echo.

echo Starting application...
echo Open browser and go to: http://localhost:8080/test-email
echo.

echo Press any key to start the application...
pause > nul

mvn spring-boot:run

echo.
echo Application stopped.
pause
