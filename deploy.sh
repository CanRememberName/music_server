#!/bin/bash

# Configuration
SERVER_PORT=31275
APP_NAME="server-0.0.1-SNAPSHOT.jar"
APP_PATH="./server-0.0.1-SNAPSHOT.jar"
LOG_FILE="app.log"

echo "=================================================="
echo "Starting deployment script for port $SERVER_PORT"
echo "=================================================="

# 1. Check if port is in use and kill the process
echo "Checking port $SERVER_PORT..."
PID=$(lsof -t -i:$SERVER_PORT)

if [ -n "$PID" ]; then
    echo "Port $SERVER_PORT is in use by PID $PID. Killing process..."
    kill -9 $PID
    if [ $? -eq 0 ]; then
        echo "Process $PID killed successfully."
    else
        echo "Failed to kill process $PID."
        exit 1
    fi
else
    echo "Port $SERVER_PORT is free."
fi
# 3. Run the application
echo "Starting application..."
if [ -f "$APP_PATH" ]; then
    # Run in background with nohup
    nohup java -jar $APP_PATH > $LOG_FILE 2>&1 &
    
    NEW_PID=$!
    echo "Application started with PID $NEW_PID"
    echo "Logs are being written to $LOG_FILE"
    
    # Wait a moment to check if it crashes immediately
    sleep 3
    if ps -p $NEW_PID > /dev/null; then
        echo "Application is running successfully."
    else
        echo "Application failed to start. Check $LOG_FILE for details."
        cat $LOG_FILE
    fi
else
    echo "Error: JAR file not found at $APP_PATH"
    exit 1
fi

echo "=================================================="
echo "Deployment finished."
echo "=================================================="
