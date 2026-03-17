#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
TARGET="${1:-all}"

print_step() { echo -e "\n\033[1;34m▶ $1\033[0m"; }
print_done() { echo -e "\033[1;32m✔ $1\033[0m"; }

install_backend() {
    print_step "Installing backend dependencies & building..."
    cd "$ROOT_DIR/backend"

    if [ ! -f gradlew ]; then
        print_step "Generating Gradle wrapper..."
        gradle wrapper --gradle-version 8.10
    fi

    chmod +x gradlew
    ./gradlew build -x test --parallel --console=plain
    print_done "Backend installed"
}

install_frontend() {
    print_step "Installing frontend dependencies..."
    cd "$ROOT_DIR/frontend"
    npm install
    print_done "Frontend installed"
}

install_infra() {
    print_step "Starting infrastructure (PostgreSQL + Kafka)..."
    cd "$ROOT_DIR"
    docker compose up -d postgres kafka
    print_step "Waiting for PostgreSQL to be ready..."
    until docker compose exec -T postgres pg_isready -U db_user -q 2>/dev/null; do
        sleep 1
    done
    print_done "Infrastructure ready"
}

case "$TARGET" in
    all)
        install_infra
        install_backend
        install_frontend
        print_done "Full install complete"
        ;;
    backend|be)
        install_backend
        ;;
    frontend|fe)
        install_frontend
        ;;
    infra)
        install_infra
        ;;
    *)
        echo "Usage: $0 [all|backend|be|frontend|fe|infra]"
        exit 1
        ;;
esac
