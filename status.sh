#!/bin/bash
# ============================================================
#  E-Commerce Microservices — View Service Status & Logs
#  Usage: chmod +x status.sh && ./status.sh
# ============================================================

APP_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_BASE_DIR="$APP_DIR/logs"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

SERVICES=("user-service" "product-service" "order-service")
PORTS=(8081 8082 8083)

echo ""
echo "╔═══════════════════════════════════════════════════╗"
echo "║   E-Commerce Services — Status                   ║"
echo "╚═══════════════════════════════════════════════════╝"
echo ""

printf "%-20s %-8s %-10s %-10s %s\n" "SERVICE" "PORT" "PID" "STATUS" "LOG FILE"
printf "%-20s %-8s %-10s %-10s %s\n" "───────────────────" "────────" "──────────" "──────────" "─────────────────────────"

for i in "${!SERVICES[@]}"; do
    svc="${SERVICES[$i]}"
    port="${PORTS[$i]}"
    pid_file="$LOG_BASE_DIR/$svc/app.pid"
    log_file=$(ls -t "$LOG_BASE_DIR/$svc/app_"*.log 2>/dev/null | head -1 || echo "N/A")

    pid="N/A"
    status="${RED}DOWN${NC}"

    if [ -f "$pid_file" ]; then
        pid=$(cat "$pid_file")
        if kill -0 "$pid" 2>/dev/null; then
            # Check health endpoint
            if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
                status="${GREEN}HEALTHY${NC}"
            else
                status="${YELLOW}STARTING${NC}"
            fi
        else
            pid="N/A"
        fi
    fi

    log_basename=$(basename "$log_file" 2>/dev/null || echo "N/A")
    printf "%-20s %-8s %-10s " "$svc" "$port" "$pid"
    echo -en "$status"
    printf "%*s %s\n" $((10 - 7)) "" "$log_basename"
done

echo ""

# Show last 5 lines of each service's latest log
if [ "$1" == "--logs" ] || [ "$1" == "-l" ]; then
    for svc in "${SERVICES[@]}"; do
        latest_log=$(ls -t "$LOG_BASE_DIR/$svc/app_"*.log 2>/dev/null | head -1)
        if [ -n "$latest_log" ]; then
            echo -e "${CYAN}── $svc (last 5 lines) ──${NC}"
            tail -5 "$latest_log"
            echo ""
        fi
    done
fi

echo "Tip: Run './status.sh --logs' to see recent log lines"
echo "     Run 'tail -f logs/<service>/app_*.log' for live logs"
echo ""
