.PHONY: install install-be install-fe install-infra \
       clean clean-be clean-fe clean-infra \
       start start-be start-fe start-infra \
       stop status

# ─── Install ────────────────────────────────────────────
install:          ## Install everything (infra + backend + frontend)
	@./scripts/install.sh all

install-be:       ## Install backend only (Gradle build, skip tests)
	@./scripts/install.sh backend

install-fe:       ## Install frontend only (npm install)
	@./scripts/install.sh frontend

install-infra:    ## Start PostgreSQL + Kafka containers
	@./scripts/install.sh infra

# ─── Clean ──────────────────────────────────────────────
clean:            ## Clean everything (build artifacts + node_modules + containers)
	@./scripts/clean.sh all

clean-be:         ## Clean backend build artifacts
	@./scripts/clean.sh backend

clean-fe:         ## Clean frontend (node_modules, dist, .angular)
	@./scripts/clean.sh frontend

clean-infra:      ## Stop containers and remove volumes
	@./scripts/clean.sh infra

# ─── Start ──────────────────────────────────────────────
start:            ## Start all services (infra + backend + frontend)
	@./scripts/start.sh all

start-be:         ## Start both backend services (visualizer + fake-caller)
	@./scripts/start.sh backend

start-fe:         ## Start Angular dev server
	@./scripts/start.sh frontend

start-infra:      ## Start PostgreSQL + Kafka only
	@./scripts/start.sh infra

# ─── Stop ───────────────────────────────────────────────
stop:             ## Stop all running containers
	docker compose down

# ─── Status ─────────────────────────────────────────────
status:           ## Show container status and ports
	@docker compose ps

# ─── Help ───────────────────────────────────────────────
help:             ## Show this help
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'
