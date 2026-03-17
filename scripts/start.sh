#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-all}"

print_step() { echo -e "\n\033[1;34m▶ $1\033[0m"; }
print_done() { echo -e "\033[1;32m✔ $1\033[0m"; }

start_infra() {
    print_step "Starting infrastructure..."
    cd "$ROOT_DIR"
    docker compose up -d postgres kafka
    until docker compose exec -T postgres pg_isready -U db_user -q 2>/dev/null; do
        sleep 1
    done
    print_done "PostgreSQL + Kafka ready"
}

start_visualizer() {
    print_step "Starting visualizer-service on :8080..."
    cd "$ROOT_DIR/backend"
    ./gradlew :visualizer-service:bootRun --console=plain &
}

start_fake_caller() {
    print_step "Starting fake-hint-caller on :8084..."
    cd "$ROOT_DIR/backend"
    ./gradlew :fake-hint-caller:bootRun --console=plain &
}

start_frontend() {
    print_step "Starting Angular dev server on :4200..."
    cd "$ROOT_DIR/frontend"
    npx ng serve --open &
}

case "$TARGET" in
    all)
        start_infra
        start_visualizer
        start_fake_caller
        start_frontend
        print_done "All services starting — wait for boot logs"
        wait
        ;;
    backend|be)
        start_visualizer
        start_fake_caller
        wait
        ;;
    visualizer)
        start_visualizer
        wait
        ;;
    fake|caller)
        start_fake_caller
        wait
        ;;
    frontend|fe)
        start_frontend
        wait
        ;;
    infra)
        start_infra
        ;;
    *)
        echo "Usage: $0 [all|backend|be|visualizer|fake|frontend|fe|infra]"
        exit 1
        ;;
esac
