# Microservices for AWS DevOps Learning

3 Spring Boot microservices that communicate with each other — built for deploying on AWS and learning VPC, EC2, Security Groups, ALB, CloudWatch, and other DevOps concepts.

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│              Docker Network / AWS VPC                     │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │
│  │ User Service │  │   Product    │  │    Order     │   │
│  │   :8081      │  │   Service    │  │   Service    │   │
│  │              │  │   :8082      │  │   :8083      │   │
│  │ • Register   │  │ • CRUD       │  │ • Create     │   │
│  │ • Login      │  │ • Reserve    │  │   Orders     │   │
│  │ • Get User   │  │   Stock      │  │ • List/Get   │   │
│  └──────────────┘  └──────────────┘  └──────┬───────┘   │
│        ▲                  ▲                  │           │
│        │                  │                  │           │
│        └──── GET /api/users/{id} ────────────┤           │
│                                              │           │
│        └──── POST /api/products/{id}/reserve─┘           │
└──────────────────────────────────────────────────────────┘
```

**Every request gets a correlation ID (`X-Correlation-Id`)** that flows through all 3 services for end-to-end tracing.

---

## Quick Start

### Using Docker Compose (recommended)

```bash
docker-compose up --build
```

All 3 services start on a shared network. Order Service automatically knows how to reach User and Product services.

### Running Individually (for local dev)

```bash
# Terminal 1
cd user-service && mvn spring-boot:run

# Terminal 2
cd product-service && mvn spring-boot:run

# Terminal 3
cd order-service && mvn spring-boot:run
```

---

## API Reference

### 1. Register a User
```bash
curl -X POST http://localhost:8081/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"name": "Rohit", "email": "rohit@example.com", "password": "pass123"}'
```

### 2. Create a Product
```bash
curl -X POST http://localhost:8082/api/products \
  -H "Content-Type: application/json" \
  -d '{"name": "Laptop", "description": "MacBook Pro M3", "price": 1299.99, "stock": 50}'
```

### 3. Create an Order (triggers inter-service calls)
```bash
curl -X POST http://localhost:8083/api/orders \
  -H "Content-Type: application/json" \
  -d '{"userId": 1, "productId": 1, "quantity": 2}'
```

This single call triggers:
1. **Order Service** → calls **User Service** to validate user
2. **Order Service** → calls **Product Service** to reserve stock
3. **Order Service** → saves the order

### 4. Health Checks
```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### 5. Other Endpoints
```bash
# List all users
curl http://localhost:8081/api/users

# Get a specific product
curl http://localhost:8082/api/products/1

# List all orders
curl http://localhost:8083/api/orders
```

---

## Logs

All services output **structured JSON logs** with correlation IDs:

```json
{
  "level": "INFO",
  "message": "Order created",
  "correlationId": "abc-123-def",
  "service": "order-service",
  "@timestamp": "2026-03-19T12:00:00Z"
}
```

View combined logs:
```bash
docker-compose logs -f
```

Filter by service:
```bash
docker-compose logs -f order-service
```

---

## AWS Deployment Hints

| Concept | How This Project Maps |
|---------|----------------------|
| **VPC** | The Docker bridge network mimics a VPC. On AWS, create a VPC with public + private subnets |
| **EC2 Instances** | Each service runs on its own port → deploy each to a separate EC2 instance |
| **Security Groups** | Order Service needs to reach User (8081) and Product (8082). Configure ingress rules accordingly |
| **ALB** | Use Actuator `/actuator/health` as the health check path for target groups |
| **CloudWatch Logs** | JSON logs are CloudWatch-ready. Use CloudWatch agent to ship container logs |
| **ECS/EKS** | Dockerfiles are ready for ECS task definitions or Kubernetes deployments |
| **RDS** | Swap H2 for RDS by changing `spring.datasource.*` in `application.yml` |
| **Environment Variables** | `USER_SERVICE_URL` and `PRODUCT_SERVICE_URL` can point to private IPs or DNS in AWS |

---

## Tech Stack

- Java 17 + Spring Boot 3.2.5
- Spring Web, Spring Data JPA, Spring Actuator
- H2 (in-memory database)
- Logback + Logstash encoder (structured JSON logs)
- RestTemplate (inter-service communication)
- Docker + Docker Compose
