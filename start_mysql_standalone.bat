@echo off
echo ============================================
echo COMPLETE MYSQL FIX - STEP BY STEP
echo ============================================
echo.

echo Step 1: Stopping all services...
taskkill /F /IM httpd.exe 2>nul
taskkill /F /IM mysqld.exe 2>nul
timeout /t 3 /nobreak >nul
echo Services stopped.
echo.

echo Step 2: Cleaning MySQL data files...
cd /d "D:\xampp\mysql\data"
del /F /Q ibdata1 2>nul
del /F /Q ib_logfile0 2>nul
del /F /Q ib_logfile1 2>nul
del /F /Q ibtmp1 2>nul
del /F /Q *.pid 2>nul
del /F /Q aria_log_control 2>nul
echo MySQL files cleaned.
echo.

echo Step 3: Starting MySQL ALONE (without Apache)...
echo This will help us verify MySQL works independently.
echo.
start "" "D:\xampp\mysql\bin\mysqld.exe" --console
echo.
echo Waiting 5 seconds for MySQL to start...
timeout /t 5 /nobreak >nul
echo.

echo Step 4: Testing MySQL connection...
tasklist | findstr /I "mysqld.exe"
if %errorlevel% == 0 (
    echo.
    echo ============================================
    echo SUCCESS! MySQL is running!
    echo ============================================
    echo.
    echo Now you can:
    echo 1. Keep this window open to see MySQL logs
    echo 2. Test connection by visiting:
    echo    http://localhost/instagram_api/test_connection.php
    echo.
    echo To stop MySQL: Close this window or press Ctrl+C
    echo.
    echo After confirming MySQL works, you can start it
    echo normally from XAMPP Control Panel.
    echo ============================================
) else (
    echo.
    echo ERROR: MySQL failed to start.
    echo Check the console output above for error messages.
    echo.
)
echo.
pause

