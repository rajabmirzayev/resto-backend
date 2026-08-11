#!/usr/bin/env bash
set -euo pipefail
trap 'echo "Deploy failed"' ERR

# Production deploy script. Runs on the target server (root).
# Builds and starts the stack with script/compose.yml.

SERVER_APP_DIR="${SERVER_APP_DIR:-/app/backend}"
SERVER_REPO="${SERVER_REPO:-https://github.com/rajabmirzayev/resto-backend.git}"
COMPOSE_FILE="${COMPOSE_FILE:-compose.yml}"
BUILD_PARALLEL="${BUILD_PARALLEL:-3}"

log() { echo "[DEPLOY] $*"; }

log "Ensuring Docker is installed..."
if ! command -v docker >/dev/null 2>&1; then
  export DEBIAN_FRONTEND=noninteractive
  apt-get update -y
  apt-get install -y ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  # shellcheck disable=SC1091
  . /etc/os-release
  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu ${VERSION_CODENAME} stable" > /etc/apt/sources.list.d/docker.list
  apt-get update -y
  apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi
systemctl enable --now docker >/dev/null 2>&1 || true

log "Updating application source in ${SERVER_APP_DIR}..."
mkdir -p "$SERVER_APP_DIR"
cd "$SERVER_APP_DIR"
if [ ! -d .git ]; then
  git init -q
  git remote add origin "$SERVER_REPO" 2>/dev/null || git remote set-url origin "$SERVER_REPO"
fi
git fetch origin master
git checkout -B master origin/master

cd script

log "Preparing .env (kept if it already exists)..."
[ -f .env ] || cp .env.example .env

log "Stopping existing stack to free memory (volumes are kept)..."
docker compose -f "$COMPOSE_FILE" down --remove-orphans || true

log "Building images (parallel ${BUILD_PARALLEL})..."
if docker compose -f "$COMPOSE_FILE" build --help 2>&1 | grep -q -- '--parallel-limit'; then
  docker compose -f "$COMPOSE_FILE" build --parallel-limit "$BUILD_PARALLEL"
else
  docker compose -f "$COMPOSE_FILE" build
fi

log "Starting stack..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

log "Bootstrapping Keycloak (idempotent)..."
bootstrap_ok=0
for i in 1 2 3 4 5 6 7 8 9 10; do
  if docker compose -f "$COMPOSE_FILE" exec -T keycloak /bin/sh /tmp/scripts/bootstrap-kc.sh; then
    bootstrap_ok=1
    break
  fi
  log "Keycloak not ready yet, retry ${i}/10 in 15s..."
  sleep 15
done
[ "$bootstrap_ok" = 1 ] || log "WARNING: Keycloak bootstrap did not complete"

log "Health check..."
healthy=0
for i in 1 2 3 4 5 6 7 8; do
  if curl -fsS -o /dev/null http://localhost:8001/actuator/health; then
    healthy=1
    break
  fi
  log "Gateway not ready yet, retry ${i}/8 in 15s..."
  sleep 15
done
if [ "$healthy" = 1 ]; then
  log "Gateway is healthy"
else
  log "WARNING: gateway health check failed - check 'docker compose -f ${COMPOSE_FILE} logs'"
fi

echo "Deploy finished"
