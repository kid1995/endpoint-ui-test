#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-all}"

print_step() { echo -e "\n\033[1;33m▶ $1\033[0m"; }
print_done() { echo -e "\033[1;32m✔ $1\033[0m"; }

clean_backend() {
    print_step "Cleaning backend build artifacts..."
    cd "$ROOT_DIR/backend"
    if [ -f gradlew ]; then
        ./gradlew clean --console=plain
    fi
    rm -rf .gradle
    rm -rf visualizer-model/build
    rm -rf visualizer-service/build
    rm -rf fake-hint-caller/build
    print_done "Backend cleaned"
}

clean_frontend() {
    print_step "Cleaning frontend artifacts..."
    cd "$ROOT_DIR/frontend"
    rm -rf node_modules
    rm -rf dist
    rm -rf .angular
    print_done "Frontend cleaned"
}

clean_infra() {
    print_step "Stopping and removing containers + volumes..."
    cd "$ROOT_DIR"
    docker compose down -v --remove-orphans
    print_done "Infrastructure cleaned"
}

case "$TARGET" in
    all)
        clean_backend
        clean_frontend
        clean_infra
        print_done "Full cleanup complete"
        ;;
    backend|be)
        clean_backend
        ;;
    frontend|fe)
        clean_frontend
        ;;
    infra)
        clean_infra
        ;;
    *)
        echo "Usage: $0 [all|backend|be|frontend|fe|infra]"
        exit 1
        ;;
esac
