@echo off
echo ========================================
echo   FIX MySQL Character Encoding
echo   For Devanagari Script Support
echo ========================================
echo.

echo This will:
echo  1. Drop existing database (if exists)
echo  2. Create new database with UTF-8MB4
echo  3. Create tables with proper encoding
echo  4. Insert test data
echo  5. Verify encoding works
echo.
echo ⚠️  WARNING: This will DELETE all existing data!
echo.
set /p confirm="Continue? (yes/no): "

if /i not "%confirm%"=="yes" (
    echo.
    echo ❌ Cancelled. No changes made.
    pause
    exit /b 0
)

echo.
echo 🔄 Running encoding fix...
echo.

mysql -u root -pkali < fix_mysql_encoding.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   ✅ SUCCESS!
    echo ========================================
    echo.
    echo Database recreated with proper UTF-8MB4 encoding.
    echo.
    echo 📋 Test the database:
    echo    mysql -u root -pkali bhojpuri_billa
    echo    SELECT * FROM translations;
    echo.
    echo You should see: नमस्कार, कैसे बानी?
    echo (NOT: ???)
    echo.
    echo 🎯 Next steps:
    echo    1. Verify test data shows Devanagari correctly
    echo    2. Run: .\run-jar.bat
    echo    3. Record and translate
    echo    4. Check database again
    echo.
) else (
    echo.
    echo ========================================
    echo   ❌ FAILED!
    echo ========================================
    echo.
    echo Possible issues:
    echo  1. MySQL not running: net start MySQL80
    echo  2. Wrong password (should be 'kali')
    echo  3. MySQL client not in PATH
    echo.
    echo Try manually:
    echo    mysql -u root -pkali ^< fix_mysql_encoding.sql
    echo.
)

pause
