@echo off
echo ========================================
echo   Bhojpurri - Billu the Cat
echo   Starting application...
echo ========================================
echo.

REM Check if Maven is installed
where mvn >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Maven is not installed or not in PATH
    echo Please install Maven from https://maven.apache.org/
    pause
    exit /b 1
)

REM Check if Java is installed
where java >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or higher
    pause
    exit /b 1
)

REM Display Java version
echo Checking Java version...
java -version
echo.

REM Build and run the application
echo Building project with Maven...
call mvn clean install

if %ERRORLEVEL% EQU 0 (
    echo.
    echo Build successful! Starting application...
    echo.
    echo Instructions:
    echo - Press and hold SPACEBAR to record your voice
    echo - Release SPACEBAR to stop and translate
    echo - Close the window to exit
    echo.
    echo IMPORTANT: Running with runtime classpath for MP3 support
    echo.
    call mvn exec:java -Dexec.classpathScope=runtime
) else (
    echo.
    echo ERROR: Build failed!
    echo Please check the error messages above.
    pause
    exit /b 1
)

pause
