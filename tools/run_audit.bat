@echo off
title RNDM Inspector - Code & Database Audit Suite
color 0b

echo ============================================================
echo   RNDM Inspector - Code & Database Performance Suite
echo ============================================================
echo Starting local analysis engine server...
echo.

python "%~dp0run_audit.py"

pause
