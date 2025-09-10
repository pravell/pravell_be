#!/bin/bash

APP_DIR="/home/ec2-user/cicd"
LOG_FILE="$APP_DIR/app.log"

echo "Starting deployment..."

# 1. JAR 파일 선택 (가장 최신 파일)
JAR_NAME=$(ls -t $APP_DIR/*.jar 2>/dev/null | head -n 1)

if [ -z "$JAR_NAME" ]; then
  echo "No jar file found in $APP_DIR"
  exit 1
fi

echo "📦 Found jar: $JAR_NAME"

# 2. 기존 애플리케이션 종료
PID=$(pgrep -f "$JAR_NAME")

if [ -n "$PID" ]; then
  echo "Stopping running app (PID: $PID)..."
  kill "$PID"
  sleep 3
else
  echo "No running app to stop."
fi

# 3. 권한 설정
chmod +x "$JAR_NAME"

# 4. 앱 실행 (로그 출력 포함)
echo "Starting app..."
nohup java -jar "$JAR_NAME" > "$LOG_FILE" 2>&1 &

echo "Deployment completed. Logs: $LOG_FILE"
