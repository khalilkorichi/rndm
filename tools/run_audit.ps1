# RNDM Inspector - PowerShell Launcher
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  RNDM Inspector - Code & Database Performance Suite" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
python "$scriptDir\run_audit.py"
