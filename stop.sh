#!/bin/bash
# ============================================================
#  E-Commerce Microservices — Stop All Services
#  Usage: chmod +x stop.sh && ./stop.sh
# ============================================================

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_BASE_DIR="$APP_DIR/logs"

RED='\033[0;31m'
GREEN='\033[0;32m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${CYAN}[INFO]${NC}  $(date '+%H:%M:%S') $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $(date '+%H:%M:%S') $1"; }

SERVICES=("user-service" "product-service" "order-service")
PORTS=(8081 8082 8083)

echo ""
log_info "Stopping all services..."
echo ""

for i in "${!SERVICES[@]}"; do
    svc="${SERVICES[$i]}"
    port="${PORTS[$i]}"
    pid_file="$LOG_BASE_DIR/$svc/app.pid"

    # Try PID file first
    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            kill "$pid" 2>/dev/null
            sleep 2
            kill -9 "$pid" 2>/dev/null || true
            log_ok "$svc stopped (PID: $pid)"
            rm -f "$pid_file"
            continue
        fi
    fi

    # Fallback: kill by port
    pid=$(lsof -ti:$port 2>/dev/null || true)
    if [ -n "$pid" ]; then
        kill $pid 2>/dev/null
        sleep 1
        kill -9 $pid 2>/dev/null || true
        log_ok "$svc stopped (PID: $pid, port: $port)"
    else
        log_info "$svc — not running"
    fi
done

echo ""
log_ok "All services stopped."
echo ""
