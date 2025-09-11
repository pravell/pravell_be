#!/bin/bash

APP_DIR="/home/ubuntu/cicd"
LOG_FILE="$APP_DIR/app.log"

echo "Starting deployment..."

# 1. JAR 파일 선택 (가장 최신 파일)
JAR_NAME=$(ls -t $APP_DIR/*.jar | grep -v 'plain' | head -n 1)

if [ -z "$JAR_NAME" ]; then
  echo "No jar file found in $APP_DIR"
  exit 1
fi

echo "📦 Found jar: $JAR_NAME"

# 2. 기존 애플리케이션 종료
EXISTING_PID=$(lsof -ti tcp:8080)

if [ -n "$EXISTING_PID" ]; then
  echo "Stopping process using port 8080 (PID: $EXISTING_PID)..."
  kill -9 "$EXISTING_PID"
  sleep 3
else
  echo "No process is using port 8080."
fi

# 3. 권한 설정
chmod +x "$JAR_NAME"

# 4. 앱 실행 (로그 출력 포함)
echo "Starting app..."
nohup java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &

echo "Deployment completed. Logs: $LOG_FILE"
