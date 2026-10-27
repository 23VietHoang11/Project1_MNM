@echo off
title Vigil Backend Server
cd /d "%~dp0"
echo ====================================================
echo   Khoi dong Vigil Backend REST API Server...
echo ====================================================
echo Dang chay Node.js Server (http://localhost:3000)...
node server.js
if %errorlevel% neq 0 (
    echo.
    echo [Thong bao] Chuyen sang chay bang Python Mock Server...
    python mock_server.py
)
pause
