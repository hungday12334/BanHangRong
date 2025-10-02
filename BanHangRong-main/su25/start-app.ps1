Write-Host "Starting BanHangRong Application..." -ForegroundColor Green
Write-Host ""

Set-Location "D:\BanHangRong-main\BanHangRong-main\su25"

Write-Host "Compiling application..." -ForegroundColor Yellow
& .\mvnw.cmd clean compile -q
if ($LASTEXITCODE -ne 0) {
    Write-Host "Compilation failed!" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Starting application..." -ForegroundColor Yellow
Write-Host "Application will be available at: http://localhost:8080" -ForegroundColor Cyan
Write-Host "Press Ctrl+C to stop the application" -ForegroundColor Cyan
Write-Host ""

& java -cp "target/classes;target/dependency/*" banhangrong.su25.Su25Application