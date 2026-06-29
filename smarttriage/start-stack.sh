#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
export NODE_ENV="${NODE_ENV:-development}"
export PORT="${PORT:-7016}"
export SMARTTRIAGE_MONGO_URI="${SMARTTRIAGE_MONGO_URI:-mongodb://mongo:27017/smarttriage}"
export SMARTTRIAGE_REDIS_HOST="${SMARTTRIAGE_REDIS_HOST:-127.0.0.1}"
export SMARTTRIAGE_REDIS_PORT="${SMARTTRIAGE_REDIS_PORT:-6379}"
export SMARTTRIAGE_REDIS_DB="${SMARTTRIAGE_REDIS_DB:-0}"

log_info() {
  echo "[smarttriage-stack] $*"
}

if command -v docker >/dev/null 2>&1 && docker compose version >/dev/null 2>&1; then
  log_info "检测到 Docker，使用 docker-compose 启动（Mongo/Redis/APP）。"
  (cd "$ROOT_DIR" && docker compose up -d --build)
  log_info "已提交启动。管理端可在 http://127.0.0.1:${PORT} 访问"
  log_info "管理员默认密码请使用数据库中配置的 root 用户。你临时测试可用 root / root1234。"
  exit 0
fi

log_info "未检测到可用 Docker，尝试直接本地启动 Node 后端。"
log_info "注意：当前代码在 Node 22 下可能仍与部分旧模块（如 oracledb、旧构建链）存在兼容问题。"

if ! command -v node >/dev/null 2>&1; then
  echo "缺少 node，请先安装 Node.js"
  exit 1
fi

log_info "执行顺序：先 npm install，再启动服务器"
cd "$ROOT_DIR"
npm install
node server.js
