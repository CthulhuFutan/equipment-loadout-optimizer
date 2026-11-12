# DVM Rune Management System

Single-user-oriented web application for managing Dragon Village M dragons and runes, optimizing rune loadouts, and tracking equipment.

## Components

- `rune_server_frontend`: Vue 3, TypeScript, Vite, Element Plus and Pinia client.
- `rune_server_backend`: Spring Boot 3 / Java 21 REST API with MyBatis-Plus, MySQL and JWT authentication.
- `../dvm_mysql.sql`: MySQL schema bootstrap script.

## Main workflows

1. Register or sign in; the client stores the JWT in `localStorage` and sends it as a Bearer token.
2. Manage dragons and their skills, treasure, images and optional rune-set requirements.
3. Add and edit rune inventory; the current form supports manual entry. OCR is a UI placeholder and does not recognize rune images.
4. Use the optimizer to score rune choices by weight, set effects and optional grind potential.
5. Use the equipment workspace to compare and apply rune loadouts. A rune is uniquely equipped through the backend equipment relation.

## Run locally

Requirements: Node.js `^22.18.0 || >=24.12.0`, Java 21, Maven and MySQL 8.

1. Create the database using `../dvm_mysql.sql`.
2. Configure backend environment variables: `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET` and optionally `DEEPSEEK_API_KEY`.
3. Start backend from `rune_server_backend` with `mvn spring-boot:run` (API port `8088`).
4. Start frontend from `rune_server_frontend` with `npm install` then `npm run dev` (Vite port `5173`). The dev server proxies `/api` and `/uploads` to `http://localhost:8088`; set `VITE_API_BASE` for another API base URL.
5. For a production deployment, serve the frontend build and configure the reverse proxy for both `/api` and `/uploads`.

## Build and checks

- Frontend production build and type-check: `npm run build`.
- Backend tests: `mvn test`.

The current backend test suite contains only a Spring context-load smoke test; a successful build does not establish business-flow coverage. See `../files/problem.txt` for tracked implementation and deployment gaps.
