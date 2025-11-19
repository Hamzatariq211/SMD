@echo off
echo ============================================
echo MySQL XAMPP Fix Script - Enhanced Version
echo ============================================
echo.

echo Step 1: Stopping any running MySQL processes...
taskkill /F /IM mysqld.exe 2>nul
if %errorlevel% == 0 (
    echo MySQL process terminated.
) else (
    echo No MySQL process running.
)
echo.

echo Step 2: Looking for XAMPP installation...
set XAMPP_PATH=D:\xampp
if not exist "%XAMPP_PATH%" (
    set XAMPP_PATH=C:\xampp
)
if not exist "%XAMPP_PATH%" (
    set XAMPP_PATH=C:\Program Files\xampp
)
if not exist "%XAMPP_PATH%" (
    set XAMPP_PATH=C:\Program Files (x86)\xampp
)

if not exist "%XAMPP_PATH%" (
    echo ERROR: XAMPP installation not found!
    echo Please manually locate your XAMPP folder and update this script.
    pause
    exit /b
)

echo XAMPP found at: %XAMPP_PATH%
echo.

echo Step 3: Checking for port conflicts...
netstat -ano | findstr :3306 > nul
if %errorlevel% == 0 (
    echo WARNING: Port 3306 is in use by another process!
    echo Checking which process is using it...
    netstat -ano | findstr :3306
    echo.
    echo You may need to change MySQL port in my.ini
    echo.
)

echo Step 4: Backing up current files...
cd /d "%XAMPP_PATH%\mysql\data"

if exist ibdata1 (
    if not exist ibdata1.bak (
        copy ibdata1 ibdata1.bak
        echo ibdata1 backed up
    )
)

if exist ib_logfile0 (
    if not exist ib_logfile0.bak (
        copy ib_logfile0 ib_logfile0.bak
        echo ib_logfile0 backed up
    )
)

if exist ib_logfile1 (
    if not exist ib_logfile1.bak (
        copy ib_logfile1 ib_logfile1.bak
        echo ib_logfile1 backed up
    )
)
echo.

echo Step 5: Cleaning corrupted files and locks...
del /F /Q ibdata1 2>nul
del /F /Q ib_logfile0 2>nul
del /F /Q ib_logfile1 2>nul
del /F /Q ibtmp1 2>nul
del /F /Q *.pid 2>nul
del /F /Q aria_log_control 2>nul
del /F /Q aria_log.* 2>nul
echo Corrupted files removed.
echo.

echo Step 6: Checking my.ini configuration...
cd /d "%XAMPP_PATH%\mysql\bin"

echo.
echo Step 7: Attempting to start MySQL manually for diagnostics...
echo This will show any error messages...
echo.
start /wait cmd /c "%XAMPP_PATH%\mysql\bin\mysqld.exe --console" --standalone --log-error="%XAMPP_PATH%\mysql\data\mysql_error.log"
echo.

echo ============================================
echo NEXT STEPS:
echo ============================================
echo 1. Check if any errors appeared above
echo 2. Open XAMPP Control Panel AS ADMINISTRATOR
echo 3. Click START on MySQL
echo 4. If it still fails, check the log file at:
echo    %XAMPP_PATH%\mysql\data\mysql_error.log
echo.
echo Common Issues:
echo - Port conflict: Change port in my.ini to 3307
echo - Permission denied: Run XAMPP as Administrator
echo - Missing database: Restore from backup folder
echo ============================================
echo.
pause
