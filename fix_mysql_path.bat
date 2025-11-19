@echo off
echo ============================================
echo MySQL Path Fix - Force Correct Data Dir
echo ============================================
echo.

echo Step 1: Stopping all MySQL processes and services...
net stop mysql 2>nul
taskkill /F /IM mysqld.exe 2>nul
timeout /t 2 /nobreak >nul
echo.

echo Step 2: Removing old MySQL Windows service if exists...
sc delete mysql 2>nul
echo.

echo Step 3: Cleaning temporary files in D:\xampp\mysql\data...
cd /d "D:\xampp\mysql\data"
del /F /Q ibdata1 2>nul
del /F /Q ib_logfile0 2>nul
del /F /Q ib_logfile1 2>nul
del /F /Q ibtmp1 2>nul
del /F /Q *.pid 2>nul
del /F /Q aria_log_control 2>nul
echo Temporary files cleaned.
echo.

echo Step 4: Starting MySQL with explicit datadir parameter...
echo This forces MySQL to use D:\xampp\mysql\data
echo.
cd /d "D:\xampp\mysql\bin"
start /B mysqld.exe --defaults-file="D:\xampp\mysql\bin\my.ini" --datadir="D:/xampp/mysql/data" --console > "D:\xampp\mysql\data\startup_fixed.log" 2>&1
timeout /t 5 /nobreak >nul

echo Step 5: Checking if MySQL started...
tasklist | findstr /I "mysqld.exe" >nul
if %errorlevel% == 0 (
    echo.
    echo ============================================
    echo SUCCESS! MySQL is now running with correct path!
    echo ============================================
    echo.
    echo Stopping test MySQL...
    taskkill /F /IM mysqld.exe 2>nul
    timeout /t 2 /nobreak >nul
    echo.
    echo ============================================
    echo PERMANENT FIX - Creating startup batch file
    echo ============================================
    echo.

    REM Create a custom MySQL startup script
    echo @echo off > "D:\xampp\mysql_start.bat"
    echo cd /d "D:\xampp\mysql\bin" >> "D:\xampp\mysql_start.bat"
    echo mysqld.exe --defaults-file="D:\xampp\mysql\bin\my.ini" --datadir="D:/xampp/mysql/data" >> "D:\xampp\mysql_start.bat"

    echo Created: D:\xampp\mysql_start.bat
    echo.
    echo FROM NOW ON, to start MySQL:
    echo 1. Open XAMPP Control Panel
    echo 2. Click Config next to MySQL
    echo 3. Or run: D:\xampp\mysql_start.bat
    echo.
    echo Now starting MySQL properly...
    start "" "D:\xampp\mysql_start.bat"
    timeout /t 3 /nobreak >nul

    tasklist | findstr /I "mysqld.exe" >nul
    if %errorlevel% == 0 (
        echo.
        echo ============================================
        echo MYSQL IS NOW RUNNING SUCCESSFULLY!
        echo ============================================
        echo.
        echo You can now use your app!
        echo To stop MySQL: Use XAMPP Control Panel or Task Manager
        echo.
        echo MySQL will keep running in the background.
        echo.
    ) else (
        echo MySQL failed to start with custom script.
        echo Check: D:\xampp\mysql\data\startup_fixed.log
    )

) else (
    echo.
    echo ============================================
    echo ERROR: MySQL still failed to start
    echo ============================================
    echo.
    echo Reading error log...
    type "D:\xampp\mysql\data\startup_fixed.log"
    echo.
)

pause

