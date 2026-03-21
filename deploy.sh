#!/bin/bash
# ============================================================
#  E-Commerce Microservices — QA Deployment Script (Ubuntu EC2)
#  Usage: chmod +x deploy.sh && ./deploy.sh
# ============================================================

set -e

# ── Configuration ─────────────────────────────────────────────
APP_DIR="$(cd "$(dirname "$0")" && pwd)"
LOG_BASE_DIR="$APP_DIR/logs"
PROFILE="qa"
JAVA_OPTS="-Xms256m -Xmx512m"

# Service definitions
SERVICES=("user-service" "product-service" "order-service")
PORTS=(8081 8082 8083)

# Environment variables — UPDATE THESE for your EC2 environment
export JWT_SECRET="${JWT_SECRET:-my-super-secret-key-for-dev-only-change-in-production-min32chars}"
export DB_HOST="${DB_HOST:-localhost}"
export DB_PORT="${DB_PORT:-5432}"
export DB_USERNAME="${DB_USERNAME:-postgres}"
export DB_PASSWORD="${DB_PASSWORD:-postgres}"
export REDIS_HOST="${REDIS_HOST:-localhost}"
export REDIS_PORT="${REDIS_PORT:-6379}"
export PAYMENT_MODE="${PAYMENT_MODE:-mock}"
export USER_SERVICE_URL="${USER_SERVICE_URL:-http://localhost:8081}"
export PRODUCT_SERVICE_URL="${PRODUCT_SERVICE_URL:-http://localhost:8082}"

# ── Colors ────────────────────────────────────────────────────
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

log_info()  { echo -e "${CYAN}[INFO]${NC}  $(date '+%H:%M:%S') $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $(date '+%H:%M:%S') $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $(date '+%H:%M:%S') $1"; }
log_err()   { echo -e "${RED}[ERROR]${NC} $(date '+%H:%M:%S') $1"; }

# ── Pre-flight checks ────────────────────────────────────────
preflight() {
    log_info "Running pre-flight checks..."

    # Check Java
    if ! command -v java &> /dev/null; then
        log_err "Java is not installed. Install JDK 17: sudo apt install openjdk-17-jdk"
        exit 1
    fi
    JAVA_VER=$(java -version 2>&1 | head -1)
    log_ok "Java found: $JAVA_VER"

    # Check Maven
    if ! command -v mvn &> /dev/null; then
        log_err "Maven is not installed. Install: sudo apt install maven"
        exit 1
    fi
    MVN_VER=$(mvn --version 2>&1 | head -1)
    log_ok "Maven found: $MVN_VER"

    # Check PostgreSQL connectivity (optional)
    if command -v pg_isready &> /dev/null; then
        if pg_isready -h "$DB_HOST" -p "$DB_PORT" &> /dev/null; then
            log_ok "PostgreSQL is reachable at $DB_HOST:$DB_PORT"
        else
            log_warn "PostgreSQL is NOT reachable at $DB_HOST:$DB_PORT — services may fail to start"
        fi
    fi

    # Check Redis connectivity (optional)
    if command -v redis-cli &> /dev/null; then
        if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" ping &> /dev/null; then
            log_ok "Redis is reachable at $REDIS_HOST:$REDIS_PORT"
        else
            log_warn "Redis is NOT reachable at $REDIS_HOST:$REDIS_PORT — caching will be unavailable"
        fi
    fi
}

# ── Create log directories ────────────────────────────────────
setup_logs() {
    log_info "Setting up log directories..."
    for svc in "${SERVICES[@]}"; do
        mkdir -p "$LOG_BASE_DIR/$svc"
        log_ok "  $LOG_BASE_DIR/$svc"
    done
}

# ── Stop existing services ────────────────────────────────────
stop_services() {
    log_info "Stopping any running services..."
    for i in "${!SERVICES[@]}"; do
        local svc="${SERVICES[$i]}"
        local port="${PORTS[$i]}"
        local pid=$(lsof -ti:$port 2>/dev/null || true)
        if [ -n "$pid" ]; then
            kill $pid 2>/dev/null || true
            sleep 1
            kill -9 $pid 2>/dev/null || true
            log_ok "  Stopped $svc (PID: $pid, port: $port)"
        fi
    done
}

# ── Build all services ────────────────────────────────────────
build_services() {
    log_info "═══════════════════════════════════════════════"
    log_info "  BUILDING ALL SERVICES (mvn clean install)"
    log_info "═══════════════════════════════════════════════"

    for svc in "${SERVICES[@]}"; do
        local svc_dir="$APP_DIR/$svc"
        local log_file="$LOG_BASE_DIR/$svc/build_$(date '+%Y%m%d_%H%M%S').log"

        log_info "Building $svc..."
        if (cd "$svc_dir" && mvn clean install -DskipTests -Pqa 2>&1 | tee "$log_file"); then
            log_ok "$svc — BUILD SUCCESS"
        else
            log_err "$svc — BUILD FAILED. Check: $log_file"
            exit 1
        fi
        echo ""
    done

    log_ok "All services built successfully!"
}

# ── Start all services ────────────────────────────────────────
start_services() {
    log_info "═══════════════════════════════════════════════"
    log_info "  STARTING ALL SERVICES (profile: $PROFILE)"
    log_info "═══════════════════════════════════════════════"

    local timestamp=$(date '+%Y%m%d_%H%M%S')

    for i in "${!SERVICES[@]}"; do
        local svc="${SERVICES[$i]}"
        local port="${PORTS[$i]}"
        local svc_dir="$APP_DIR/$svc"
        local jar_file=$(ls "$svc_dir/target/"*.jar 2>/dev/null | head -1)
        local log_file="$LOG_BASE_DIR/$svc/app_${timestamp}.log"
        local pid_file="$LOG_BASE_DIR/$svc/app.pid"

        if [ -z "$jar_file" ]; then
            log_err "No JAR found for $svc. Run build first."
            exit 1
        fi

        log_info "Starting $svc on port $port..."
        log_info "  JAR: $jar_file"
        log_info "  Log: $log_file"

        # Set service-specific DB name
        local db_name="${svc//-/_}db"
        db_name="${db_name//service_/}"  # user_servicedb -> userdb

        nohup java $JAVA_OPTS \
            -Dspring.profiles.active=$PROFILE \
            -DDB_HOST=$DB_HOST \
            -DDB_PORT=$DB_PORT \
            -DDB_NAME=$db_name \
            -DDB_USERNAME=$DB_USERNAME \
            -DDB_PASSWORD=$DB_PASSWORD \
            -DJWT_SECRET=$JWT_SECRET \
            -DREDIS_HOST=$REDIS_HOST \
            -DREDIS_PORT=$REDIS_PORT \
            -DPAYMENT_MODE=$PAYMENT_MODE \
            -DUSER_SERVICE_URL=$USER_SERVICE_URL \
            -DPRODUCT_SERVICE_URL=$PRODUCT_SERVICE_URL \
            -jar "$jar_file" \
            > "$log_file" 2>&1 &

        local pid=$!
        echo "$pid" > "$pid_file"
        log_ok "$svc started — PID: $pid"

        # Wait a bit between services so dependencies can come up
        if [ "$svc" != "order-service" ]; then
            log_info "  Waiting 10s for $svc to initialize..."
            sleep 10
        fi
    done
}

# ── Health check ──────────────────────────────────────────────
health_check() {
    log_info "═══════════════════════════════════════════════"
    log_info "  HEALTH CHECK (waiting for services...)"
    log_info "═══════════════════════════════════════════════"

    sleep 15

    for i in "${!SERVICES[@]}"; do
        local svc="${SERVICES[$i]}"
        local port="${PORTS[$i]}"
        local max_retries=5
        local retry=0

        while [ $retry -lt $max_retries ]; do
            if curl -sf "http://localhost:$port/actuator/health" > /dev/null 2>&1; then
                log_ok "$svc (port $port) — HEALTHY ✓"
                break
            fi
            retry=$((retry + 1))
            if [ $retry -lt $max_retries ]; then
                log_warn "$svc (port $port) — not ready yet, retrying in 5s... ($retry/$max_retries)"
                sleep 5
            else
                log_err "$svc (port $port) — FAILED TO START ✗"
                log_err "  Check logs: $LOG_BASE_DIR/$svc/"
            fi
        done
    done
}

# ── Print status summary ─────────────────────────────────────
print_summary() {
    echo ""
    log_info "═══════════════════════════════════════════════"
    log_info "  DEPLOYMENT SUMMARY"
    log_info "═══════════════════════════════════════════════"
    echo ""
    echo "  Profile:    $PROFILE"
    echo "  Database:   PostgreSQL @ $DB_HOST:$DB_PORT"
    echo "  Redis:      $REDIS_HOST:$REDIS_PORT"
    echo "  Payment:    $PAYMENT_MODE mode"
    echo ""
    echo "  Services:"
    for i in "${!SERVICES[@]}"; do
        local svc="${SERVICES[$i]}"
        local port="${PORTS[$i]}"
        local pid_file="$LOG_BASE_DIR/$svc/app.pid"
        local pid=$(cat "$pid_file" 2>/dev/null || echo "N/A")
        echo "    $svc  →  http://localhost:$port  (PID: $pid)"
    done
    echo ""
    echo "  Logs:       $LOG_BASE_DIR/<service-name>/"
    echo ""
    log_info "To stop all services:  ./stop.sh"
    log_info "To view live logs:     tail -f $LOG_BASE_DIR/<service-name>/app_*.log"
    echo ""
}

# ── Main ──────────────────────────────────────────────────────
main() {
    echo ""
    echo "╔═══════════════════════════════════════════════════╗"
    echo "║   E-Commerce QA Deployment — Ubuntu EC2          ║"
    echo "╚═══════════════════════════════════════════════════╝"
    echo ""

    preflight
    setup_logs
    stop_services
    build_services
    start_services
    health_check
    print_summary
}

main "$@"
