@echo off
echo ============================================
echo MySQL Quick Fix for D:\xampp
echo ============================================
echo.

echo Stopping MySQL if running...
taskkill /F /IM mysqld.exe 2>nul
timeout /t 2 /nobreak >nul
echo.

echo Navigating to MySQL data folder...
cd /d "D:\xampp\mysql\data"
echo Current directory: %CD%
echo.

echo Checking for mysql system database...
if exist mysql (
    echo mysql folder exists - GOOD
) else (
    echo ERROR: mysql system database folder is MISSING!
    echo This is why MySQL stops immediately after starting.
    echo.
    echo SOLUTION: We need to restore it from backup
    goto RESTORE
)

echo.
echo Removing corrupted InnoDB files...
del /F /Q ibdata1 2>nul
del /F /Q ib_logfile0 2>nul
del /F /Q ib_logfile1 2>nul
del /F /Q ibtmp1 2>nul
del /F /Q *.pid 2>nul
echo Done.
echo.

echo ============================================
echo NOW TRY STARTING MYSQL IN XAMPP
echo ============================================
pause
exit /b

:RESTORE
echo.
echo Restoring mysql system database from backup...
if exist "D:\xampp\mysql\backup\mysql" (
    echo Backup found! Copying...
    xcopy "D:\xampp\mysql\backup\mysql" "D:\xampp\mysql\data\mysql" /E /I /Y
    echo Restore complete!
    echo.
    echo Now removing corrupted InnoDB files...
    del /F /Q ibdata1 2>nul
    del /F /Q ib_logfile0 2>nul
    del /F /Q ib_logfile1 2>nul
    del /F /Q ibtmp1 2>nul
    del /F /Q *.pid 2>nul
    echo.
    echo ============================================
    echo MYSQL SYSTEM DATABASE RESTORED
    echo NOW TRY STARTING MYSQL IN XAMPP
    echo ============================================
) else (
    echo ERROR: Backup folder not found at D:\xampp\mysql\backup\mysql
    echo.
    echo MANUAL FIX REQUIRED:
    echo 1. Reinstall XAMPP or
    echo 2. Copy mysql folder from another XAMPP installation or
    echo 3. Download fresh XAMPP and copy the mysql system database
    echo.
)
pause

