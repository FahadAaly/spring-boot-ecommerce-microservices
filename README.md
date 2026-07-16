# E‑Commerce Microservices

Monorepo for a simple e‑commerce system implemented with Spring Boot microservices. The project demonstrates a Config Server–backed configuration, separate services per domain, and containerized infrastructure for local development.

Current local date/time: 2026‑07‑16 23:23

## Architecture Overview

- configserver (Spring Cloud Config Server)
  - Centralized externalized configuration, served from a native classpath source (`classpath:/config`).
  - Runs on port `8084`.
- product-service (Spring Boot)
  - Product catalog backed by PostgreSQL.
  - Default port: `8081`.
- order-service (Spring Boot)
  - Order domain backed by PostgreSQL.
  - Default port: `8083`.
- user-service (Spring Boot)
  - User domain; currently configured to use MongoDB (Atlas) via `spring.mongodb.uri`.
  - Default port: `8082`.
- Infrastructure (Docker Compose)
  - PostgreSQL 14 and pgAdmin 4 for local development.
  - Exposed ports: Postgres `5432`, pgAdmin `5050`.

### Service Configuration

All business services import configuration from the Config Server:

- `spring.config.import: configserver:http://localhost:8084`
- Active profile: `dev`

The Config Server loads files from `configserver/src/main/resources/config`:

- `product-service-dev.yml`
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/product`
  - `spring.datasource.username=devcode`
  - `spring.datasource.password=devcode`
- `order-service-dev.yml`
  - `spring.datasource.url=jdbc:postgresql://localhost:5432/order`
  - `spring.datasource.username=devcode`
  - `spring.datasource.password=devcode`
- `user-service-dev.yml`
  - `spring.mongodb.uri` (MongoDB connection string)
  - `spring.mongodb.database=userdb`

Important: Do not commit real credentials. The sample MongoDB URI in this repo is for demo purposes only. For real use, set secrets via environment variables or a secure vault and reference them from your config.

## Tech Stack

- Java 21
- Spring Boot 4.1.x
- Spring Cloud 2025.x (Config Server / Config Client)
- Datastores:
  - PostgreSQL 14 (product, order)
  - MongoDB (user)
- Build: Maven
- Containers: Docker / Docker Compose

## Prerequisites

- Java 21 (verify with `java -version`)
- Maven 3.9+
- Docker Desktop or Docker Engine + Docker Compose
- (Optional) MongoDB Atlas account or a local MongoDB instance if you want to run `user-service`

## Getting Started (Local Development)

1) Clone the repo

```bash
git clone <this-repo-url>
cd ecom-microservices
```

2) Start infrastructure (PostgreSQL + pgAdmin)

```bash
docker compose up -d
# Postgres ready on localhost:5432 (user: devcode, password: devcode)
# pgAdmin on http://localhost:5050 (email: padmin4@domain.org, password: admin)
```

3) Create application databases in Postgres

You need two databases: `product` and `order`.

- Option A: Using psql

```bash
# macOS/Linux example
docker exec -it postgres_container psql -U devcode -h localhost -p 5432 -c "CREATE DATABASE product;"
docker exec -it postgres_container psql -U devcode -h localhost -p 5432 -c "CREATE DATABASE \"order\";"
```

- Option B: Using pgAdmin (UI)
  - Open http://localhost:5050 and log in
  - Register the server pointing to `postgres` container or `localhost:5432`
  - Create databases `product` and `order`

4) Configure MongoDB for `user-service`

- Easiest path: Use MongoDB Atlas and obtain a connection string, e.g. `mongodb+srv://...`
- Set environment variable before starting `user-service` to avoid committing secrets:

```bash
export SPRING_DATA_MONGODB_URI='your-mongodb-uri-here'
export SPRING_DATA_MONGODB_DATABASE='userdb'
```

Then either:
- Override via Spring properties on run: `-Dspring.data.mongodb.uri=$SPRING_DATA_MONGODB_URI -Dspring.data.mongodb.database=$SPRING_DATA_MONGODB_DATABASE`
- Or update `configserver/src/main/resources/config/user-service-dev.yml` locally (do NOT commit real credentials)

5) Build the project

```bash
mvn -q -DskipTests clean package
```

6) Run the Config Server first

```bash
cd configserver
mvn spring-boot:run
# Available at http://localhost:8084
```

7) Run services (new terminals or background)

```bash
# product-service on :8081
cd product-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# order-service on :8083
cd order-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev

# user-service on :8082 (requires MongoDB)
cd user-service && mvn spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments="-Dspring.data.mongodb.uri=$SPRING_DATA_MONGODB_URI -Dspring.data.mongodb.database=$SPRING_DATA_MONGODB_DATABASE"
```

Notes:
- Each service has its own `server.port` set in `src/main/resources/application.yml`.
- With profile `dev`, services fetch configuration from the Config Server at `http://localhost:8084`.

## Verifying Configuration

You can check what the Config Server serves via the actuator endpoint patterns, for example:

```
GET http://localhost:8084/product-service/dev
GET http://localhost:8084/order-service/dev
GET http://localhost:8084/user-service/dev
```

These should return the merged configuration for the given application and profile.

## Typical Development Workflow

- Update configuration in `configserver/src/main/resources/config/*.yml`
- Restart affected services or enable Spring Cloud Bus/refresh scope (not configured by default here)
- Make code changes in the corresponding service module and rebuild/run just that module

## Testing

Run all tests:

```bash
mvn test
```

Or per module, for example:

```bash
cd product-service && mvn test
```

## Project Layout

```
/ (repo root)
├─ configserver/                       # Spring Cloud Config Server (port 8084)
│  ├─ src/main/resources/application.yml
│  └─ src/main/resources/config/
│     ├─ product-service-dev.yml
│     ├─ order-service-dev.yml
│     └─ user-service-dev.yml
├─ product-service/                    # Product microservice (port 8081)
│  └─ src/main/resources/application.yml
├─ order-service/                      # Order microservice (port 8083)
│  └─ src/main/resources/application.yml
├─ user-service/                       # User microservice (port 8082)
│  └─ src/main/resources/application.yml
├─ additional/ecom-application/        # Additional example app (if present)
└─ docker-compose.yml                  # Postgres + pgAdmin for local dev
```

## Security & Secrets

- Never commit real credentials. Use environment variables or a secrets manager.
- Prefer overriding sensitive properties at runtime: `-Dspring.data.mongodb.uri=...` etc.
- If you fork this repo, rotate any leaked demo credentials immediately.

## Troubleshooting

- Config not loading:
  - Ensure Config Server is running on port 8084 before starting services.
  - Verify `spring.config.import: configserver:http://localhost:8084` is present.
- DB connection failures:
  - Confirm Postgres container is up: `docker ps`, and databases `product` and `order` exist.
  - Check credentials (`devcode/devcode`) and JDBC URLs in config server files.
- MongoDB issues in `user-service`:
  - Ensure `SPRING_DATA_MONGODB_URI` points to a reachable MongoDB and your IP is whitelisted (Atlas).
- Port already in use:
  - Adjust `server.port` in each service’s `application.yml` or free the port.

## Future Enhancements (ideas)

- API Gateway & Service Discovery (e.g., Spring Cloud Gateway, Eureka)
- Centralized logging/metrics (ELK/EFK, Prometheus + Grafana)
- Dockerfiles per service and a full `docker-compose` to run all services
- Testcontainers for integration tests
- CI pipeline and dependency scanning

---

© 2026 E‑Commerce Microservices Example. For educational/demo use.
