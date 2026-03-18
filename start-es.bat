@echo off
set ES_PATH=C:\develop\elasticsearch-8.12.2-windows-x86_64\elasticsearch-8.12.2

echo Checking port 9201...
for /f "tokens=5" %%a in ('netstat -aon ^| findstr :9201') do (
    if NOT "%%a"=="" (
        echo Port 9201 is occupied by PID %%a, killing process...
        taskkill /F /PID %%a
    )
)

echo.
echo Starting Elasticsearch on port 9201...
echo (Note: This script passes -Ehttp.port=9201 to the ES startup)
echo.

cd /d "%ES_PATH%\bin"
elasticsearch.bat -Ehttp.port=9201
