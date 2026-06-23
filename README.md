# Inventory Management System - Microservices Architecture

## 📋 Project Overview

**Inventory Management System** is a production-ready, scalable microservices application designed to manage products, inventory, orders, and supplier relationships. Built with **Spring Boot 3.5.5**, **Kafka**, and **MongoDB**, this system demonstrates enterprise-level distributed architecture patterns including service discovery, API gateway routing, asynchronous event processing, and inter-service communication.

### 🎯 Key Features
- **Distributed Microservices**: 7 independent, scalable services
- **Service Discovery & Load Balancing**: Eureka-based service registry with automatic load balancing
- **Asynchronous Messaging**: Kafka-powered event-driven architecture for order notifications
- **REST API Gateway**: Centralized request routing with path-based predicates
- **Document Database**: MongoDB with service-specific data isolation
- **Production-Ready**: Docker containerization, health checks, and multi-stage builds

---

## 🚀 Quick Start - Deployment

### Prerequisites
- **Docker** & **Docker Compose** (recommended)
- **OR** Java 21 JDK, Maven 3.9+, MongoDB, and Kafka (for local development)

### Deployment Method 1: Docker Compose (Recommended - 2 minutes)

```bash
cd /home/ankit/IdeaProjects/InventoryManagment/docker
docker-compose up --build
```

**What gets deployed:**
- 7 Spring Boot microservices (auto-built from Dockerfiles)
- Zookeeper (Kafka coordination)
- Apache Kafka (message broker)
- MongoDB (document database)
- Internal Docker network for service communication

**Verify deployment:**
```
- Open http://localhost:8761 (Eureka Dashboard)
- All services should show "UP" status
- Ready for API testing at http://localhost:8080
```

**Stop all services:**
```bash
docker-compose down
```

### Deployment Method 2: Local Maven Build (Development)

```bash
# Build all modules
cd /home/ankit/IdeaProjects/InventoryManagment
mvn clean install

# Start Eureka Server first
cd EurekaServer && mvn spring-boot:run

# In separate terminals, start other services:
cd ApiGateway && mvn spring-boot:run
cd Product && mvn spring-boot:run
cd Inventory && mvn spring-boot:run
cd Order && mvn spring-boot:run
cd Notification && mvn spring-boot:run
cd Supplier && mvn spring-boot:run
```

---

## 📦 System Architecture & Services

### Service Topology

```
┌─────────────────────────────────────────────────────────────┐
│                    Client Applications                       │
└────────────────────────┬────────────────────────────────────┘
                         │ HTTP Requests
                         ▼
         ┌───────────────────────────────┐
         │   API Gateway (Port 8080)      │
         │   • Route: /api/product/**     │
         │   • Route: /api/inventory/**   │
         │   • Route: /api/order/**       │
         │   • Route: /api/supplier/**    │
         └───────┬───────────────────────┘
                 │ Load Balanced Routing
        ┌────────┼────────┬──────────┬──────────┐
        ▼        ▼        ▼          ▼          ▼
    ┌────────┐┌──────────┐┌───────┐┌────────┐┌──────┐
    │Product ││Inventory ││ Order ││Supplier││Notif.│
    │(8082)  ││(8081)    ││(8083) ││(8085)  ││(8084)│
    └───┬────┘└──────────┘└───┬───┘└────────┘└──┬───┘
        │  OpenFeign Calls    │   Kafka Events  │
        │  (Synchronous)      │   (Asynchronous)│
        └─────────────────────┴─────────────────┘
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
    ┌──────────────┐    ┌───────────┐
    │   MongoDB    │    │   Kafka   │
    │   Service 1  │    │   Topic   │
    │   Service 2  │    │   Broker  │
    │     ...      │    └───────────┘
    └──────────────┘

Plus: Eureka Registry (8761) for Service Discovery
```

---

## 🔧 Service Components

| Service | Port | Purpose | Database | Key Technologies |
|---------|------|---------|----------|------------------|
| **API Gateway** | 8080 | Entry point, routing, load balancing | N/A | Spring Cloud Gateway, Eureka Client |
| **Eureka Server** | 8761 | Service registry & discovery | N/A | Netflix Eureka |
| **Product Service** | 8082 | Product CRUD, Feign to Inventory | product-service-v2 | Spring Data MongoDB, OpenFeign |
| **Inventory Service** | 8081 | Stock management, quantity validation | inventory-service-v2 | Spring Data MongoDB |
| **Order Service** | 8083 | Order placement, Kafka producer | order-service-v2 | Spring Data MongoDB, Kafka Producer |
| **Notification Service** | 8084 | Email notifications, Kafka consumer | notification-service-v2 | Spring Mail, Kafka Listener |
| **Supplier Service** | 8085 | Supplier management, orchestration | supplier-service-v2 | Spring Data MongoDB, OpenFeign |

### Infrastructure Components

| Component | Version | Purpose |
|-----------|---------|---------|
| **Zookeeper** | 7.6.0 | Kafka coordination and broker management |
| **Kafka** | 7.6.0 | Asynchronous event streaming (order events → notifications) |
| **MongoDB** | 7.0 | NoSQL document storage (one DB per service) |

---

## 📡 Communication Patterns

### 1. **Synchronous Communication (OpenFeign + Eureka)**
Services call each other directly for immediate responses:
- **Product Service** → calls **Inventory Service** to validate stock
- **Order Service** → calls **Inventory Service** to check quantity
- **Supplier Service** → calls **Product Service** to add/remove products

**Example:**
```java
@FeignClient(name = "INVENTORY")
public interface InventoryClient {
    @GetMapping("/getInventory")
    List<InventoryResponse> getInventory();
    
    @PostMapping("/validateQuantity")
    boolean validateQuantity(@RequestBody InventoryRequest request);
}
```

### 2. **Asynchronous Communication (Kafka)**
Services publish events for eventual consistency:
- **Order Service** publishes "OrderCreated" event to Kafka topic
- **Notification Service** consumes event and sends confirmation email
- Decouples services; enables retry logic and event auditing

**Example Flow:**
```
1. Order placed: POST /api/order/placeOrder
2. Order Service saves to MongoDB
3. Order Service publishes to Kafka: "order-topic"
4. Notification Service @KafkaListener receives event
5. Sends confirmation email to customer
```

---

## 🔐 Security & Configuration

### Environment Variables (Docker Compose)

Each service receives configuration via environment variables:

```yaml
environment:
  SPRING_DATA_MONGODB_URI: mongodb://mongodb:27017/service-db
  EUREKA_CLIENT_SERVICEURL_DEFAULTZONE: http://eureka-server:8761/eureka
  SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9092  # For Kafka consumers/producers
```

### Key Security Notes

1. **MongoDB Access**: No authentication required in development (containers on internal network)
   - Production: Enable MongoDB authentication with credentials
   - Production: Use network policies to restrict access

2. **Kafka**: No authentication enabled
   - Production: Enable SASL/SSL for Kafka brokers
   - Production: Use ACLs for topic access control

3. **Notification Service Gmail Configuration**
   - Location: `Notification/src/main/resources/application.properties`
   - Currently uses placeholder credentials
   - **Production**: Use environment variables or Spring Cloud Config for sensitive data

4. **API Gateway**: No request validation/security currently
   - Production: Add Spring Security, JWT token validation
   - Production: Implement rate limiting and API key management

---

## 📊 Database Schema Overview

### MongoDB Collections (per service)

**Product Service** (`product-service-v2`):
```javascript
{
  "_id": ObjectId,
  "productName": "Laptop",
  "productDescription": "Dell XPS 13",
  "price": 999.99,
  "createdAt": ISODate,
  "updatedAt": ISODate
}
```

**Inventory Service** (`inventory-service-v2`):
```javascript
{
  "_id": ObjectId,
  "productId": "PROD001",
  "quantity": 150,
  "warehouseLocation": "A-3-5",
  "lastUpdated": ISODate
}
```

**Order Service** (`order-service-v2`):
```javascript
{
  "_id": ObjectId,
  "customerId": "CUST001",
  "orderDate": ISODate,
  "items": [
    { "productId": "PROD001", "quantity": 2, "price": 999.99 }
  ],
  "totalAmount": 1999.98,
  "status": "CONFIRMED",
  "createdAt": ISODate
}
```

**Notification Service** (`notification-service-v2`):
```javascript
{
  "_id": ObjectId,
  "orderId": "ORD001",
  "customerId": "CUST001",
  "email": "customer@example.com",
  "status": "SENT",
  "sentAt": ISODate
}
```

---

## 🧪 Testing Strategy

### 1. **Unit Tests**
```bash
cd Product && mvn test
```

Run service-level tests (repository, service logic):
- Mocked dependencies
- Fast execution (~100ms per test)
- Current status: Basic context loading tests

### 2. **Integration Tests**
Test multiple layers (controller → service → database):
```bash
mvn test -Dtest=ProductControllerIntegrationTest
```

### 3. **API Testing** (Manual/Postman)
```bash
# Create Product
curl -X POST http://localhost:8080/api/product/createProduct \
  -H "Content-Type: application/json" \
  -d '{"productName":"Laptop","productDescription":"Dell","price":999.99}'

# Get all products
curl http://localhost:8080/api/product/getAllProduct

# Place Order
curl -X POST http://localhost:8080/api/order/placeOrder \
  -H "Content-Type: application/json" \
  -d '{"customerId":"CUST001","productId":"PROD001","quantity":2}'
```

### 4. **Kafka Message Testing**
```bash
# View messages in order-topic
docker exec kafka kafka-console-consumer.sh \
  --topic order-topic \
  --from-beginning \
  --bootstrap-server localhost:9092
```

---

## 📈 Monitoring & Logging

### Health Checks

All services expose health endpoints via Spring Actuator:
```bash
curl http://localhost:8082/actuator/health  # Product Service
```

**Response:**
```json
{
  "status": "UP",
  "components": {
    "mongodb": { "status": "UP" },
    "eureka": { "status": "UP" },
    "kafka": { "status": "UP" }
  }
}
```

### Service Discovery Monitoring

**Eureka Dashboard**: http://localhost:8761
- View all registered services
- Check service status (UP/DOWN)
- Monitor instance health
- View available instance URLs

### Logging

All services log via SLF4J to console:
```bash
# View logs from specific service
docker logs -f product

# Search for errors
docker logs api-gateway 2>&1 | grep ERROR
```

### Recommended Enhancements

- **Distributed Tracing**: Spring Cloud Sleuth + Zipkin
- **Metrics**: Micrometer + Prometheus
- **Centralized Logging**: ELK Stack (Elasticsearch, Logstash, Kibana)
- **Alerts**: Grafana with Prometheus data source

---

## ⚠️ Troubleshooting

### Service Not Registering with Eureka
```bash
# Check if Eureka is running first
curl http://localhost:8761

# Check service logs for Eureka connection errors
docker logs product | grep -i eureka

# Verify Eureka endpoint in service config
docker exec product cat /application.properties | grep eureka
```

### MongoDB Connection Failed
```bash
# Verify MongoDB is running
docker ps | grep mongodb

# Check MongoDB is accessible
docker exec mongodb mongo --eval "db.adminCommand('ping')"

# Verify connection string in service
docker logs inventory | grep mongodb
```

### Kafka Message Not Consumed
```bash
# Check if Kafka broker is running
docker logs kafka | grep "started"

# Verify topic exists
docker exec kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Check consumer group status
docker exec kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --group notification-service \
  --describe
```

### Port Already in Use
```bash
# Find process using port
lsof -i :8080

# Kill process
kill -9 <PID>

# Or change port in application.properties
```

---

## 📚 Key Features Explained

### Service Discovery (Eureka)
- Services self-register when they start
- API Gateway discovers services dynamically
- Client-side load balancing via Eureka metadata
- Health checks every 30 seconds

### API Gateway Routing
- Path-based routing: `/api/product/**` → PRODUCT service
- Load balancer URI: `lb://PRODUCT-SERVICE-NAME`
- Automatic retry logic for failed requests
- Service-to-service calls still use Eureka

### Kafka for Asynchronous Processing
- **Topics**: `order-topic` for order events
- **Consumer Groups**: `notification-service` for notification handler
- **Partition**: Single partition (scales with replication)
- **Retention**: Messages retained for replay capability

### Multi-Stage Docker Builds
Optimized image sizes:
1. **Build Stage**: Maven builds JAR (heavy dependencies)
2. **Runtime Stage**: Only JRE + JAR (lightweight final image)

---

## 🎓 Interview Preparation Notes

This system demonstrates:
1. **Microservices Architecture**: Independent services, separate databases
2. **Service Discovery Pattern**: Dynamic service registration + load balancing
3. **API Gateway Pattern**: Centralized routing, single entry point
4. **Saga Pattern**: Distributed transaction via Kafka events
5. **Feign Client**: Declarative HTTP client for sync calls
6. **Event-Driven Architecture**: Decoupled services via Kafka
7. **Circuit Breaker Candidates**: Resilience4j for Feign calls
8. **Scalability**: Horizontal scaling via containerization

---

## 📞 Support & Contributing

For questions, issues, or contributions, refer to `guide.md` for comprehensive system understanding.

---

**Last Updated**: 2026-06-23  
**Java Version**: 21  
**Spring Boot**: 3.5.5  
**Spring Cloud**: 2025.0.0
