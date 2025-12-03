@echo off
echo ========================================
echo   MySQL Setup Helper
echo ========================================
echo.

REM Check if MySQL service exists
sc query MySQL80 >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ❌ MySQL80 service not found!
    echo.
    echo 💡 Possible reasons:
    echo    1. MySQL is not installed
    echo    2. MySQL service has a different name
    echo.
    echo 🔍 Checking for other MySQL services...
    sc query state= all | findstr /i "mysql"
    echo.
    echo 📥 To install MySQL:
    echo    1. Download from: https://dev.mysql.com/downloads/installer/
    echo    2. Install MySQL Community Server
    echo    3. Set root password to: kali
    echo    4. Run this script again
    echo.
    pause
    exit /b 1
)

REM Check if MySQL is running
sc query MySQL80 | findstr /i "running" >nul
if %ERRORLEVEL% EQU 0 (
    echo ✅ MySQL is already running!
) else (
    echo 🔄 Starting MySQL...
    net start MySQL80
    if %ERRORLEVEL% EQU 0 (
        echo ✅ MySQL started successfully!
    ) else (
        echo ❌ Failed to start MySQL!
        echo    Run as Administrator if needed
        pause
        exit /b 1
    )
)

echo.
echo ========================================
echo   Testing Database Connection
echo ========================================
echo.

REM Run database test
call mvn clean compile
if %ERRORLEVEL% NEQ 0 (
    echo ❌ Compilation failed!
    pause
    exit /b 1
)

echo.
echo Running database test...
echo.
call mvn exec:java -Dexec.mainClass="com.bhojpurri.TestDatabase"

echo.
echo ========================================
echo   Setup Complete!
echo ========================================
echo.
echo ✅ MySQL is running
echo ✅ Database test completed
echo.
echo 💡 Next steps:
echo    1. Check the test results above
echo    2. If all tests passed, run: run-jar.bat
echo    3. Press SPACE to start recording!
echo.
pause
