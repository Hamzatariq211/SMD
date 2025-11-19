@echo off
cd /d "E:\Mobile dev Projects\i210396"
echo Cleaning build cache...
call gradlew.bat clean
echo.
echo Building project...
call gradlew.bat build
echo.
echo Build complete!
pause

