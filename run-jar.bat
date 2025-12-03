@echo off
echo ========================================
echo   Bhojpurri - Billu the Cat
echo   Building Executable JAR...
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

REM Build the fat JAR with all dependencies
echo Building project with all dependencies...
call mvn clean package

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========================================
    echo   Build successful!
    echo ========================================
    echo.
    echo Starting application...
    echo.
    echo Instructions:
    echo - Press and hold SPACEBAR to record your voice
    echo - Release SPACEBAR to stop and translate
    echo - Close the window to exit
    echo.
    echo Running: java -jar target\bhojpurri-app-1.0.0.jar
    echo.
    java -jar target\bhojpurri-app-1.0.0.jar
) else (
    echo.
    echo ERROR: Build failed!
    echo Please check the error messages above.
    pause
    exit /b 1
)

pause
