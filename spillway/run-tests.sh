#!/bin/bash

# Check if FFmpeg is available
if command -v ffmpeg &> /dev/null; then
    echo "FFmpeg is available, running all tests..."
    export FFMPEG_AVAILABLE=true
    ./mvnw test -Dffmpeg.available=true
else
    echo "FFmpeg not found, running basic tests only..."
    ./mvnw test
fi

# Run specific test suites
echo "Running unit tests..."
./mvnw test -Dtest="*Test"

echo "Running integration tests..."
./mvnw test -Dtest="*IT,*IntegrationTest"

# Generate test report
./mvnw surefire-report:report

echo "Test reports generated in target/site/surefire-report.html"