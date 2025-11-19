@echo off
echo ============================================
echo MySQL Diagnostic - Detailed Error Check
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
    echo This is why MySQL cannot start.
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

echo Step 4: Cleaning temporary files...
del /F /Q ibdata1 2>nul
del /F /Q ib_logfile0 2>nul
del /F /Q ib_logfile1 2>nul
del /F /Q ibtmp1 2>nul
del /F /Q *.pid 2>nul
del /F /Q aria_log_control 2>nul
echo Temporary files cleaned.
echo.

echo Step 5: Attempting to start MySQL with verbose output...
echo Starting MySQL... (If you see errors, read them carefully)
echo.
"D:\xampp\mysql\bin\mysqld.exe" --console --skip-grant-tables
goto END

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
    echo Restore complete! Now cleaning temp files and trying to start...
    del /F /Q ibdata1 2>nul
    del /F /Q ib_logfile0 2>nul
    del /F /Q ib_logfile1 2>nul
    del /F /Q ibtmp1 2>nul
    del /F /Q *.pid 2>nul
    echo.
    echo Starting MySQL...
    "D:\xampp\mysql\bin\mysqld.exe" --console --skip-grant-tables
) else (
    echo.
    echo [ERROR] Backup not found at: D:\xampp\mysql\backup\mysql
    echo.
    echo MANUAL FIX REQUIRED:
    echo 1. You need to reinstall XAMPP or
    echo 2. Copy the mysql system database from another XAMPP installation
    echo.
    echo The mysql system database contains user authentication tables
    echo and is required for MySQL to start.
    echo.
    pause
)

:END

