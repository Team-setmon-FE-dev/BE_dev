#!/usr/bin/env zsh
# ──────────────────────────────────────────────────────────────────────────────
# run-local.sh  ·  로컬 개발 서버 실행 스크립트
# ──────────────────────────────────────────────────────────────────────────────
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"

# 1. .env 존재 확인
if [[ ! -f "$ENV_FILE" ]]; then
  echo "❌  .env 파일이 없습니다."
  exit 1
fi

# 2. DB 컨테이너 확인
if ! docker ps --format '{{.Names}}' 2>/dev/null | grep -q "setmon-db"; then
  echo "🐳  MySQL 컨테이너 기동..."
  docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d db
  echo "⏳  MySQL 준비 대기 (20초)..."
  sleep 20
fi

# 3. .env → gradle.properties 변환 (Gradle이 직접 읽을 수 있도록)
echo "📦  .env → Gradle 프로퍼티 변환 중..."
PROPS_FILE="$SCRIPT_DIR/gradle.properties"
: > "$PROPS_FILE"  # 파일 초기화

while IFS='=' read -r key value; do
  [[ "$key" =~ ^[[:space:]]*# ]] && continue
  [[ -z "${key// }" ]] && continue
  
  key="${key## }"; key="${key%% }"
  value="${value## }"; value="${value%% }"
  value="${value%\"}"; value="${value#\"}"
  
  # Gradle systemProp 형식으로 변환
  case "$key" in
    DB_URL)           echo "systemProp.DB_URL=$value" >> "$PROPS_FILE" ;;
    DB_USERNAME)      echo "systemProp.DB_USERNAME=$value" >> "$PROPS_FILE" ;;
    DB_PASSWORD)      echo "systemProp.DB_PASSWORD=$value" >> "$PROPS_FILE" ;;
    JWT_SECRET)       echo "systemProp.JWT_SECRET=$value" >> "$PROPS_FILE" ;;
    S3_BUCKET)        echo "systemProp.S3_BUCKET=$value" >> "$PROPS_FILE" ;;
    AWS_REGION)       echo "systemProp.AWS_REGION=$value" >> "$PROPS_FILE" ;;
    AWS_ACCESS_KEY_ID) echo "systemProp.AWS_ACCESS_KEY_ID=$value" >> "$PROPS_FILE" ;;
    AWS_SECRET_ACCESS_KEY) echo "systemProp.AWS_SECRET_ACCESS_KEY=$value" >> "$PROPS_FILE" ;;
  esac
done < "$ENV_FILE"

echo "    ✅ gradle.properties 생성 완료"
echo "    DB_URL      = $(grep DB_URL "$PROPS_FILE" | cut -d'=' -f2-)"
echo "    DB_USERNAME = $(grep DB_USERNAME "$PROPS_FILE" | cut -d'=' -f2)"

# 4. Spring Boot 실행
echo "🚀  Spring Boot 기동 중..."
cd "$SCRIPT_DIR"
./gradlew bootRun --no-daemon

