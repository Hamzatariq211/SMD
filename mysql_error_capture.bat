@echo off
echo ============================================
echo MySQL Error Capture - Step by Step
echo ============================================
echo.

echo Step 1: Stopping all MySQL processes...
taskkill /F /IM mysqld.exe 2>nul
timeout /t 2 /nobreak >nul
echo.

echo Step 2: Checking if mysql system database exists...
cd /d "D:\xampp\mysql\data"
if exist mysql (
    echo [OK] mysql system database folder exists
) else (
    echo [ERROR] mysql system database folder is MISSING!
    goto RESTORE_MYSQL
)
echo.

echo Step 3: Checking critical MySQL files...
if exist mysql\user.frm (
    echo [OK] mysql\user.frm exists
) else (
    echo [ERROR] mysql\user.frm is missing - mysql database is corrupted
    goto RESTORE_MYSQL
)
echo.

echo Step 4: Listing database folders...
echo Found these databases:
dir /AD /B
echo.

echo Step 5: Checking if instagram_clone database exists...
if exist instagram_clone (
    echo [OK] instagram_clone database exists
) else (
    echo [WARNING] instagram_clone database not found
)
echo.

echo Step 6: Cleaning temporary files...
del /F /Q ibdata1 2>nul
del /F /Q ib_logfile0 2>nul
del /F /Q ib_logfile1 2>nul
del /F /Q ibtmp1 2>nul
del /F /Q *.pid 2>nul
del /F /Q aria_log_control 2>nul
echo Temporary files cleaned.
echo.

echo Step 7: Testing MySQL startup...
echo Running MySQL in background and capturing errors...
echo.
cd /d "D:\xampp\mysql\bin"
start /B mysqld.exe --console > "D:\xampp\mysql\data\startup_test.log" 2>&1
timeout /t 5 /nobreak >nul

tasklist | findstr /I "mysqld.exe" >nul
if %errorlevel% == 0 (
    echo.
    echo ============================================
    echo SUCCESS! MySQL is running!
    echo ============================================
    echo.
    taskkill /F /IM mysqld.exe 2>nul
    echo MySQL test stopped. You can now start it from XAMPP.
    echo.
    goto END_SUCCESS
) else (
    echo.
    echo ============================================
    echo ERROR: MySQL failed to start
    echo ============================================
    echo.
    echo Reading error log...
    echo.
    type "D:\xampp\mysql\data\startup_test.log"
    echo.
    echo ============================================
    goto END_FAIL
)

:RESTORE_MYSQL
echo.
echo ============================================
echo MYSQL SYSTEM DATABASE IS CORRUPTED/MISSING
echo ============================================
echo.
echo Attempting to restore from backup...
if exist "D:\xampp\mysql\backup\mysql" (
    echo Backup found! Restoring...
    echo.
    rd /S /Q "D:\xampp\mysql\data\mysql" 2>nul
    xcopy "D:\xampp\mysql\backup\mysql" "D:\xampp\mysql\data\mysql\" /E /I /Y
    echo.
    if %errorlevel% == 0 (
        echo [OK] Restore complete!
        echo.
        echo Now try starting MySQL from XAMPP Control Panel.
        goto END_SUCCESS
    ) else (
        echo [ERROR] Restore failed!
        goto END_FAIL
    )
) else (
    echo.
    echo [ERROR] Backup not found at: D:\xampp\mysql\backup\mysql
    echo.
    echo Checking alternate backup location...
    if exist "D:\xampp\mysql\backup" (
        echo Backup folder exists. Contents:
        dir "D:\xampp\mysql\backup" /B
    ) else (
        echo Backup folder doesn't exist at all!
    )
    echo.
    goto END_FAIL
)

:END_FAIL
echo.
echo ============================================
echo TROUBLESHOOTING STEPS:
echo ============================================
echo 1. Check the error log above for specific error messages
echo 2. If mysql database is missing, you need to:
echo    - Reinstall XAMPP, or
echo    - Copy mysql folder from D:\xampp\mysql\backup\mysql
echo 3. Common errors:
echo    - "Can't find file" = missing mysql system database
echo    - "Access denied" = run as Administrator
echo    - "Port in use" = another MySQL is running
echo ============================================
echo.
pause
exit /b 1

:END_SUCCESS
echo.
echo ============================================
echo NEXT STEPS:
echo ============================================
echo 1. Open XAMPP Control Panel AS ADMINISTRATOR
echo 2. Click START on MySQL
echo 3. It should work now!
echo ============================================
echo.
pause
exit /b 0

