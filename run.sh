#!/bin/bash

echo "========================================"
echo "  Bhojpurri - Billu the Cat"
echo "  Starting application..."
echo "========================================"
echo ""

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "ERROR: Maven is not installed or not in PATH"
    echo "Please install Maven from https://maven.apache.org/"
    exit 1
fi

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo "ERROR: Java is not installed or not in PATH"
    echo "Please install Java 11 or higher"
    exit 1
fi

# Display Java version
echo "Checking Java version..."
java -version
echo ""

# Build and run the application
echo "Building project with Maven..."
mvn clean install

if [ $? -eq 0 ]; then
    echo ""
    echo "Build successful! Starting application..."
    echo ""
    echo "Instructions:"
    echo "- Press and hold SPACEBAR to record your voice"
    echo "- Release SPACEBAR to stop and translate"
    echo "- Close the window to exit"
    echo ""
    mvn exec:java -Dexec.mainClass="com.bhojpurri.MainApp"
else
    echo ""
    echo "ERROR: Build failed!"
    echo "Please check the error messages above."
    exit 1
fi
