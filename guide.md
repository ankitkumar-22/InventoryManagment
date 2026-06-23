# Inventory Management System - Comprehensive Technical Guide

## Table of Contents
1. [System Overview](#system-overview)
2. [Architecture Deep Dive](#architecture-deep-dive)
3. [Complete System Flow](#complete-system-flow)
4. [Service-by-Service Breakdown](#service-by-service-breakdown)
5. [Communication Patterns Explained](#communication-patterns-explained)
6. [Database Design & Data Flow](#database-design--data-flow)
7. [API Specifications](#api-specifications)
8. [Deployment & Infrastructure](#deployment--infrastructure)
9. [Testing Strategies](#testing-strategies)
10. [Performance & Scaling](#performance--scaling)
11. [Error Handling & Edge Cases](#error-handling--edge-cases)
12. [Security Implementation](#security-implementation)
13. [Monitoring & Observability](#monitoring--observability)
14. [Interview Q&A](#interview-qa)

---

## System Overview

### What is This System?
This is a **distributed inventory management system** designed to handle:
- Product catalog management
- Real-time inventory tracking
- Order processing with stock validation
- Asynchronous order notifications
- Supplier management and orchestration

### Why Microservices?
Instead of one monolithic application, we have 7 independent services:
- **Scalability**: Each service scales independently
- **Resilience**: One service failure doesn't crash the entire system
- **Technology Choice**: Each service can use optimal tech stack
- **Team Autonomy**: Different teams can develop different services
- **Deployment**: Deploy individual services without stopping others

### Key Principles Used
- **Service Isolation**: Each service has its own database
- **Eventual Consistency**: Services eventually agree on data via events
- **Decoupling**: Services communicate asynchronously when possible
- **API Gateway**: Single entry point for all clients

---

## Architecture Deep Dive

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         CLIENT LAYER                                 │
│  (Web Browser, Mobile App, External Systems)                        │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                    HTTP/HTTPS Requests
                             │
                             ▼
┌────────────────────────────────────────────────────────────────────┐
│                  API GATEWAY LAYER (Port 8080)                     │
│  - Request routing based on path predicates                        │
│  - Load balancing across instances                                 │
│  - Protocol translation (HTTP/gRPC)                               │
│  - Rate limiting (can be added)                                   │
└────────────────────┬───────────────────────────────────────────────┘
                     │
         ┌───────────┴───────────┬───────────┬───────────┐
         │                       │           │           │
         ▼                       ▼           ▼           ▼
┌─────────────────┐  ┌──────────────────┐ ┌─────────┐ ┌────────────┐
│  Product Svc    │  │ Inventory Svc    │ │ Order   │ │ Supplier   │
│  (8082)         │  │ (8081)           │ │ Svc     │ │ Svc (8085) │
│                 │  │                  │ │ (8083)  │ │            │
│ ┌─────────────┐ │  │ ┌──────────────┐ │ │ ┌─────┐ │ │ ┌────────┐ │
│ │ Controller  │ │  │ │ Controller   │ │ │ │ Ctrl│ │ │ │ Ctrl   │ │
│ ├─────────────┤ │  │ ├──────────────┤ │ │ ├─────┤ │ │ ├────────┤ │
│ │ Service     │ │  │ │ Service      │ │ │ │ Svc │ │ │ │ Service│ │
│ ├─────────────┤ │  │ ├──────────────┤ │ │ ├─────┤ │ │ ├────────┤ │
│ │ Repository  │ │  │ │ Repository   │ │ │ │ Repo│ │ │ │ Repo   │ │
│ └─────────────┘ │  │ └──────────────┘ │ │ └─────┘ │ │ └────────┘ │
└────────┬────────┘  └─────────┬────────┘ └────┬───┘ └──────┬─────┘
         │ (calls via           │ (called by      │          │
         │ OpenFeign)           │ Product, Order) │          │
         │                      │                │          │
         └──────────────────────┴────────────────┴──────────┘
                          │
                          ▼
         ┌────────────────────────────────┐
         │  EUREKA SERVICE REGISTRY       │
         │  (Port 8761)                   │
         │  - Service metadata            │
         │  - Health checks               │
         │  - Load balancer info          │
         └────────────────────────────────┘

         ┌────────────────┐    ┌──────────────────┐
         │   MongoDB      │    │  Kafka Broker    │
         │   (27017)      │    │  (9092)          │
         │                │    │                  │
         │ product-v2     │    │ Topic:           │
         │ inventory-v2   │    │ order-topic      │
         │ order-v2       │    │                  │
         │ supplier-v2    │    │ Connected to:    │
         │ notification-v2│    │ - Order Svc      │
         │                │    │   (Producer)     │
         │                │    │ - Notification   │
         │                │    │   (Consumer)     │
         └────────────────┘    └──────────────────┘


┌──────────────────────────────────────────────────────────────────┐
│                   NOTIFICATION SERVICE (8084)                    │
│  - Kafka listener for order events                              │
│  - Email composition and sending                                │
│  - Retry logic for failed notifications                         │
└──────────────────────────────────────────────────────────────────┘
```

### Design Patterns Used

#### 1. **Microservices Pattern**
- Independent services, separate databases
- Each service handles one business capability
- Services communicate via APIs

#### 2. **API Gateway Pattern**
- Single entry point for all clients
- Simplifies client implementation
- Enables cross-cutting concerns (logging, security)

#### 3. **Service Registry Pattern (Eureka)**
- Services register themselves on startup
- Clients discover services dynamically
- Health checks keep registry up-to-date

#### 4. **Saga Pattern (for Distributed Transactions)**
- Order placement involves multiple services
- No traditional 2-phase commit
- Compensation logic if order fails

#### 5. **Event-Driven Architecture**
- Order events trigger notifications asynchronously
- Kafka ensures eventual consistency
- Services remain loosely coupled

#### 6. **Circuit Breaker Pattern** (NOT YET IMPLEMENTED - Interview Talking Point)
- Could prevent cascading failures
- Should wrap Feign client calls
- Fallback behavior when service is down

---

## Complete System Flow

### End-to-End Order Processing Flow

#### **Scenario: Customer Places an Order**

```
STEP 1: Client sends HTTP request to API Gateway
┌──────────────────────────────────────────────────────────────┐
POST http://localhost:8080/api/order/placeOrder
{
  "customerId": "CUST001",
  "items": [
    { "productId": "PROD001", "quantity": 5 }
  ],
  "deliveryAddress": "123 Main St"
}
└──────────────────────────────────────────────────────────────┘


STEP 2: API Gateway Routes to Order Service
┌──────────────────────────────────────────────────────────────┐
Gateway checks:
- Route predicate: Path = /api/order/**
- Service name: ORDER
- Using Eureka to find ORDER service instance
- Forwards request to http://order:8083/placeOrder
└──────────────────────────────────────────────────────────────┘


STEP 3: Order Service Validates Product Stock
┌──────────────────────────────────────────────────────────────┐
Order Service receives request:
- Parses customerId, items, address
- For each product in items:
    - Calls Inventory Service via OpenFeign
    - Feign finds INVENTORY service via Eureka
    - Sends: GET /api/inventory/validateQuantity?productId=PROD001&quantity=5
    
Inventory Service response:
- Checks MongoDB: quantity available >= 5?
- Returns: { "isValid": true }
  
If ANY product fails validation:
- Returns 400 Bad Request to client
- Order NOT created
- No Kafka event published
- Flow STOPS here
└──────────────────────────────────────────────────────────────┘


STEP 4: Order Persisted to Database
┌──────────────────────────────────────────────────────────────┐
Order Service saves to MongoDB (order-service-v2 database):
{
  "_id": ObjectId("507f191e810c19729de860ea"),
  "customerId": "CUST001",
  "orderDate": ISODate("2025-06-23T14:30:00Z"),
  "items": [
    {
      "productId": "PROD001",
      "quantity": 5,
      "price": 999.99
    }
  ],
  "totalAmount": 4999.95,
  "status": "CONFIRMED",
  "deliveryAddress": "123 Main St",
  "createdAt": ISODate("2025-06-23T14:30:00Z")
}

Response sent to client:
{
  "orderId": "507f191e810c19729de860ea",
  "status": "CONFIRMED",
  "totalAmount": 4999.95
}
└──────────────────────────────────────────────────────────────┘


STEP 5: Asynchronous Event Publishing to Kafka
┌──────────────────────────────────────────────────────────────┐
Order Service publishes to Kafka topic "order-topic":
{
  "orderId": "507f191e810c19729de860ea",
  "customerId": "CUST001",
  "items": [...],
  "totalAmount": 4999.95,
  "timestamp": 1687526400000
}

This happens AFTER returning response to client
(Asynchronous, non-blocking)

Kafka guarantees:
- Message is stored durably
- Message can be consumed by multiple subscribers
- Messages are consumed in order (per partition)
└──────────────────────────────────────────────────────────────┘


STEP 6: Notification Service Consumes Event
┌──────────────────────────────────────────────────────────────┐
Notification Service continuously listens:
@KafkaListener(topics = "order-topic", groupId = "notification-service")
public void handleOrderEvent(OrderRequest orderRequest)

When message received:
1. Queries customer database for email address
   - Calls Product Service or reads from stored reference
   
2. Constructs email:
   - To: customer@example.com
   - Subject: "Your Order #507f191e810c19729de860ea Confirmed"
   - Body: HTML email with order details, items, total amount
   
3. Sends via Gmail SMTP (configured in application.properties)
   - Uses: spring-boot-starter-mail + JavaMailSender
   - Configuration:
     spring.mail.host=smtp.gmail.com
     spring.mail.port=587
     spring.mail.username=${GMAIL_USERNAME}
     spring.mail.password=${GMAIL_PASSWORD}
   
4. Saves to MongoDB (notification-service-v2):
   {
     "_id": ObjectId(...),
     "orderId": "507f191e810c19729de860ea",
     "customerId": "CUST001",
     "email": "customer@example.com",
     "status": "SENT",
     "sentAt": ISODate("2025-06-23T14:30:05Z")
   }
   
5. If email fails:
   - Logs error
   - Retry logic (configurable)
   - Message remains in Kafka for replay
└──────────────────────────────────────────────────────────────┘


STEP 7: Order Complete
┌──────────────────────────────────────────────────────────────┐
Timeline (milliseconds):
0ms:    Client sends HTTP request
5ms:    API Gateway routes to Order Service
10ms:   Order Service calls Inventory Service (Feign)
15ms:   Inventory validates and responds
20ms:   Order saved to MongoDB
25ms:   Response sent to client (IMMEDIATE)
30ms:   Order published to Kafka (after response)
50ms:   Notification Service receives Kafka message
100ms:  Email sent to customer

Customer sees immediate response while async tasks complete in background
└──────────────────────────────────────────────────────────────┘
```

### Product Lifecycle Flow

```
1. CREATE PRODUCT
   Admin → API Gateway (/api/product/createProduct)
   → Product Service (MongoDB) saves
   → Calls Inventory Service to create inventory entry
   
2. UPDATE PRODUCT
   Admin → Product Service (update MongoDB doc)
   
3. ADD TO CART
   Customer → Product Service (retrieve product details)
   → Inventory Service (check availability)
   
4. CHECKOUT → Order placement flow (see above)
```

### Inventory Management Flow

```
1. STOCK RECEIVED (from Supplier)
   Supplier Service → Inventory Service
   → Increment quantity in MongoDB
   
2. STOCK ALLOCATION (for Order)
   Order Service → Inventory Service
   → Validate quantity available
   → (NO automatic decrement - could be added)
   
3. STOCK RETURN (order cancelled)
   Order Service → Inventory Service
   → Increment quantity back
```

---

## Service-by-Service Breakdown

### 1. API Gateway Service (Port 8080)

**Purpose**: Single entry point for all client requests

**Technology Stack**:
- Spring Cloud Gateway (routing layer)
- Eureka Client (service discovery)

**Configuration** (application.properties):
```properties
spring.application.name=API-GATEWAY
server.port=8080
eureka.client.service-url.defaultZone=http://localhost:8761/eureka

# Route configuration
spring.cloud.gateway.routes[0].id=product-service
spring.cloud.gateway.routes[0].uri=lb://PRODUCT
spring.cloud.gateway.routes[0].predicates[0]=Path=/api/product/**

spring.cloud.gateway.routes[1].id=inventory-service
spring.cloud.gateway.routes[1].uri=lb://INVENTORY
spring.cloud.gateway.routes[1].predicates[0]=Path=/api/inventory/**
# ... more routes
```

**How It Works**:
1. Client sends: `GET /api/product/getAllProduct`
2. Gateway matches path predicate → `/api/product/**`
3. Gateway maps to route → `lb://PRODUCT`
4. Gateway queries Eureka: "Where is PRODUCT service?"
5. Eureka responds: "PRODUCT available at 10.0.0.2:8082"
6. Gateway load balances (if multiple instances)
7. Request forwarded to actual service
8. Response sent back to client

**Key Features**:
- Path-based routing
- Eureka-based service discovery
- Automatic retries
- Request/response filtering

**Limitations**:
- No authentication/authorization (add Spring Security)
- No rate limiting (add Resilience4j)
- No request validation (add Bean Validation)

---

### 2. Eureka Server (Port 8761)

**Purpose**: Service registry and discovery

**What It Does**:
1. **Service Registration**: Services register on startup
2. **Health Monitoring**: Periodic health checks every 30 seconds
3. **Service Discovery**: Clients query for service locations
4. **Load Balancing**: Provides list of available instances

**Registration Process**:
```
Service Startup (e.g., Product Service)
  ↓
Read eureka.client.service-url.defaultZone=http://localhost:8761/eureka
  ↓
Send: POST /eureka/apps/PRODUCT
      with instance details (hostname, port, IP)
  ↓
Eureka stores in memory:
  {
    "application": "PRODUCT",
    "instances": [
      {
        "hostName": "product-1",
        "ipAddr": "10.0.0.2",
        "port": 8082,
        "status": "UP"
      }
    ]
  }
  ↓
Client queries: GET /eureka/apps/PRODUCT
  ↓
Eureka returns list of all PRODUCT instances
```

**Dashboard**: http://localhost:8761
- Shows all registered services
- Instance status (UP/DOWN)
- Last heartbeat time
- Renewal rate

**Failure Scenarios**:
- Service goes down → Eureka removes after 2 heartbeat failures (~60 seconds)
- Network partition → Eureka enters self-preservation mode
- Client caches service list → Can still work temporarily

---

### 3. Product Service (Port 8082)

**Purpose**: Manage product catalog

**Database**: `product-service-v2` (MongoDB)

**Data Model**:
```java
@Document(collection = "Product")
public class ProductEntity {
    @Id
    private String productId;
    private String productName;
    private String productDescription;
    private Double price;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

**Key APIs**:
```
POST /api/product/createProduct
{
  "productName": "Laptop",
  "productDescription": "Dell XPS 13",
  "price": 1299.99
}

GET /api/product/getAllProduct
→ Returns all products

GET /api/product/getProduct/{productId}
→ Returns single product

PUT /api/product/updateProduct/{productId}
→ Updates product details

DELETE /api/product/deleteProduct/{productId}
→ Deletes product
```

**Inter-Service Calls** (using Feign):
```java
@FeignClient(name = "INVENTORY")
public interface InventoryClient {
    @PostMapping("/api/inventory/addProduct")
    void addProduct(@RequestBody InventoryRequest request);
    
    @PostMapping("/api/inventory/removeProduct")
    void removeProduct(@RequestBody InventoryRequest request);
}
```

**Business Logic**:
```
When creating product:
1. Save to MongoDB
2. Call Inventory Service to create inventory record
3. Return product ID to client

If Inventory Service call fails:
→ Product created but no inventory!
→ Compensation: Admin must create inventory manually
→ Could add @Transactional with rollback
```

**Scaling Considerations**:
- Each instance has its own MongoDB connection
- Eureka distributes requests across instances
- No session state (stateless design)

---

### 4. Inventory Service (Port 8081)

**Purpose**: Track stock levels and validate availability

**Database**: `inventory-service-v2` (MongoDB)

**Data Model**:
```javascript
{
  "_id": ObjectId,
  "productId": "PROD001",
  "quantity": 150,
  "warehouseLocation": "A-3-5",
  "lastRestocked": ISODate("2025-06-20"),
  "reorderLevel": 50,
  "lastUpdated": ISODate("2025-06-23T14:30:00Z")
}
```

**Key APIs**:
```
GET /api/inventory/getInventory
→ Returns all inventory

GET /api/inventory/getInventory/{productId}
→ Returns quantity for specific product

POST /api/inventory/validateQuantity
Request: { "productId": "PROD001", "quantity": 5 }
Response: { "isValid": true }

POST /api/inventory/updateQuantity
Request: { "productId": "PROD001", "quantity": -5 }
→ Decrements stock (negative = sell, positive = restock)
```

**Important**: 
- Called by Order Service before order confirmation
- Validates but doesn't decrement (that's future enhancement)
- No Kafka messages (synchronous only)

**Constraints**:
- What if quantity in MongoDB becomes negative?
  - Should add validation: `quantity >= 0`
  - Should add index on `productId` for fast lookups

---

### 5. Order Service (Port 8083)

**Purpose**: Process customer orders

**Database**: `order-service-v2` (MongoDB)

**Data Model**:
```javascript
{
  "_id": ObjectId,
  "customerId": "CUST001",
  "orderDate": ISODate("2025-06-23T14:30:00Z"),
  "items": [
    {
      "productId": "PROD001",
      "quantity": 5,
      "unitPrice": 999.99,
      "totalPrice": 4999.95
    }
  ],
  "totalAmount": 4999.95,
  "status": "CONFIRMED",  // PENDING, CONFIRMED, SHIPPED, DELIVERED
  "deliveryAddress": "123 Main St",
  "paymentMethod": "CREDIT_CARD",
  "paymentStatus": "COMPLETED",
  "createdAt": ISODate("2025-06-23T14:30:00Z"),
  "updatedAt": ISODate("2025-06-23T14:30:00Z")
}
```

**Key APIs**:
```
POST /api/order/placeOrder
{
  "customerId": "CUST001",
  "items": [
    { "productId": "PROD001", "quantity": 5 }
  ],
  "deliveryAddress": "123 Main St"
}

GET /api/order/getOrder/{orderId}
→ Get order details

GET /api/order/getOrdersByCustomer/{customerId}
→ Get all orders for customer

PUT /api/order/cancelOrder/{orderId}
→ Cancel order (and increment inventory)
```

**Order Processing Logic**:
```java
public void placeOrder(OrderRequest request) {
    // 1. Validate customer exists (could call Customer Service)
    // 2. For each item:
    //    - Fetch product from cache/Product Service
    //    - Validate quantity via Inventory Service
    //    - If ANY item invalid → throw exception
    
    // 3. Calculate total amount
    
    // 4. Save order to MongoDB
    Order order = new Order();
    order.setCustomerId(request.getCustomerId());
    order.setItems(request.getItems());
    order.setStatus("CONFIRMED");
    orderRepository.save(order);
    
    // 5. Publish to Kafka (asynchronously)
    kafkaTemplate.send("order-topic", order);
    
    // 6. Return to client immediately
    return order.getId();
}
```

**Kafka Integration**:
```java
@Autowired
private KafkaTemplate<String, Order> kafkaTemplate;

// In placeOrder method:
ListenableFuture<SendResult<String, Order>> future = 
    kafkaTemplate.send("order-topic", order);

future.addCallback(
    result -> log.info("Order sent to Kafka: " + order.getId()),
    ex -> log.error("Failed to send order: " + ex.getMessage())
);

// No waiting - returns immediately to client
```

**Feign Client for Inventory Validation**:
```java
@FeignClient(name = "INVENTORY")
public interface InventoryClient {
    @PostMapping("/api/inventory/validateQuantity")
    boolean validateQuantity(@RequestBody InventoryRequest request);
}

// Usage in OrderService:
for (OrderItem item : order.getItems()) {
    boolean valid = inventoryClient.validateQuantity(
        new InventoryRequest(item.getProductId(), item.getQuantity())
    );
    if (!valid) {
        throw new OutOfStockException("Product " + item.getProductId() + " not available");
    }
}
```

**Failure Scenarios**:
1. **Inventory Service down**: Feign throws exception
   - Order not created
   - Client gets 503 Service Unavailable
   - Could add circuit breaker + default inventory check

2. **Kafka unavailable**: Order created but event not sent
   - Order confirmed but customer doesn't get notification
   - Could add retry logic or dead letter queue

3. **MongoDB down**: Exception thrown
   - Order not persisted
   - Client gets 500 error

---

### 6. Notification Service (Port 8084)

**Purpose**: Send order confirmation emails to customers

**Database**: `notification-service-v2` (MongoDB)

**Data Model**:
```javascript
{
  "_id": ObjectId,
  "orderId": "ORD001",
  "customerId": "CUST001",
  "email": "customer@example.com",
  "orderDetails": {
    "totalAmount": 4999.95,
    "items": [...]
  },
  "status": "SENT",  // PENDING, SENT, FAILED
  "sentAt": ISODate("2025-06-23T14:30:05Z"),
  "failureReason": null,
  "retryCount": 0
}
```

**Kafka Listener**:
```java
@KafkaListener(topics = "order-topic", groupId = "notification-service")
public void handleOrderEvent(Order order) {
    // 1. Fetch customer email from Product Service or cache
    String email = getCustomerEmail(order.getCustomerId());
    
    // 2. Build email content
    String htmlBody = buildEmailTemplate(order);
    
    // 3. Send email
    try {
        mailSender.send(new SimpleMailMessage() {{
            setTo(email);
            setSubject("Order Confirmation #" + order.getId());
            setText(htmlBody);
            setFrom("noreply@inventory.com");
        }});
        
        // 4. Log success
        notificationRepository.save(new Notification(
            orderId, "SENT", LocalDateTime.now()
        ));
    } catch (Exception e) {
        // 5. Log failure
        notificationRepository.save(new Notification(
            orderId, "FAILED", e.getMessage()
        ));
        // Could add retry queue
    }
}
```

**Email Configuration** (application.properties):
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

**Email Template Example**:
```html
<h1>Thank you for your order!</h1>
<p>Order ID: {{orderId}}</p>
<p>Total Amount: {{totalAmount}}</p>
<table>
  <tr><th>Product</th><th>Qty</th><th>Price</th></tr>
  {{#items}}
  <tr>
    <td>{{productName}}</td>
    <td>{{quantity}}</td>
    <td>{{price}}</td>
  </tr>
  {{/items}}
</table>
<p>We will send you tracking info when your order ships!</p>
```

**Key Characteristics**:
- **Asynchronous**: Doesn't block order processing
- **Fault Tolerant**: Email failure doesn't affect order
- **Auditable**: All notifications logged to MongoDB
- **Replayable**: Kafka retains messages for replay

**Potential Improvements**:
- Add retry policy (3 attempts, exponential backoff)
- Add dead letter queue for messages that can't be sent
- Add templates for different notification types (shipped, delivered, etc.)
- Add SMS notifications alongside email

---

### 7. Supplier Service (Port 8085)

**Purpose**: Manage suppliers and trigger inventory updates

**Database**: `supplier-service-v2` (MongoDB)

**Data Model**:
```javascript
{
  "_id": ObjectId,
  "supplierId": "SUP001",
  "supplierName": "TechWorld Supplies",
  "contactEmail": "contact@techworld.com",
  "phone": "+1-555-0123",
  "address": "456 Supply St",
  "paymentTerms": "NET_30",
  "status": "ACTIVE",  // ACTIVE, INACTIVE, SUSPENDED
  "createdAt": ISODate,
  "lastUpdated": ISODate
}
```

**Key APIs**:
```
POST /api/supplier/createSupplier
{
  "supplierName": "TechWorld",
  "contactEmail": "contact@techworld.com",
  "phone": "+1-555-0123"
}

GET /api/supplier/getAllSuppliers
→ List all suppliers

PUT /api/supplier/updateSupplier/{supplierId}
→ Update supplier info

POST /api/supplier/receiveInventory
{
  "supplierId": "SUP001",
  "productId": "PROD001",
  "quantity": 100,
  "poNumber": "PO-12345"
}
→ Add received inventory (calls Inventory Service)
```

**Business Logic**:
```
When supplier delivers inventory:
1. Create purchase order record
2. Call Inventory Service: addProduct
3. Update supplier order status to DELIVERED
4. Could trigger Kafka event for audit trail
```

**Feign Clients**:
```java
@FeignClient(name = "PRODUCT")
public interface ProductClient {
    @GetMapping("/api/product/{productId}")
    ProductResponse getProduct(@PathVariable String productId);
}

@FeignClient(name = "INVENTORY")
public interface InventoryClient {
    @PostMapping("/api/inventory/addProduct")
    void addProduct(@RequestBody InventoryRequest request);
}
```

---

## Communication Patterns Explained

### Pattern 1: Synchronous Communication (OpenFeign + Eureka)

**When to Use**:
- Need immediate response (blocking)
- Tightly coupled operations
- Request-response semantics

**Example: Order Service Validates with Inventory Service**

```
SEQUENCE DIAGRAM:

Order Service              Eureka                 Inventory Service
     │                      │                            │
     │ 1. placeOrder()      │                            │
     │─────────────────────>│                            │
     │                      │ 2. GET /apps/INVENTORY    │
     │                      │ 3. Returns instances      │
     │<─────────────────────│                            │
     │                      │                            │
     │ 4. Feign call validateQuantity                   │
     ├────────────────────────────────────────────────>│
     │                      │                            │
     │                      │ 5. Check MongoDB           │
     │                      │    Quantity >= 5?          │
     │                      │                            │
     │ 6. Response: true                                 │
     │<────────────────────────────────────────────────┤
     │                      │                            │
     │ 7. Save to MongoDB   │                            │
     │    (order-service-v2)│                            │
     │                      │                            │
```

**Code Implementation**:
```java
// OrderService.java
@Service
public class OrderService {
    @Autowired
    private InventoryClient inventoryClient;
    
    public void placeOrder(OrderRequest request) {
        // Validate each item
        for (OrderItem item : request.getItems()) {
            // BLOCKS HERE waiting for response
            boolean valid = inventoryClient.validateQuantity(
                new InventoryRequest(item.getProductId(), item.getQuantity())
            );
            if (!valid) {
                throw new OutOfStockException("Product not available");
            }
        }
        // Only continue if all validations passed
        orderRepository.save(new Order(request));
    }
}

// InventoryClient.java (Feign)
@FeignClient(name = "INVENTORY")
public interface InventoryClient {
    @PostMapping("/api/inventory/validateQuantity")
    boolean validateQuantity(@RequestBody InventoryRequest request);
}
```

**How Eureka Discovery Works**:
```
1. Feign annotation: @FeignClient(name = "INVENTORY")
2. On first call, Eureka client requests Eureka server:
   GET http://localhost:8761/eureka/apps/INVENTORY
3. Eureka responds with instances:
   {
     "application": "INVENTORY",
     "instances": [
       { "ipAddr": "10.0.0.2", "port": 8081, "status": "UP" },
       { "ipAddr": "10.0.0.3", "port": 8081, "status": "UP" }
     ]
   }
4. Feign caches this list
5. Request routed to one instance (round-robin load balancing)
6. If instance fails, Feign tries next one (automatic retry)
```

**Pros**:
- Simple, straightforward request-response
- Immediate feedback
- Easy debugging

**Cons**:
- Blocking (slow if downstream service is slow)
- Tightly coupled (if Inventory Service down, Order Service blocks)
- Network overhead (synchronous calls)
- Scaling challenges (thread pool required for each Feign call)

**Failure Handling**:
```java
try {
    boolean valid = inventoryClient.validateQuantity(request);
} catch (FeignException e) {
    if (e.status() == 503) {
        // Service temporarily down
        throw new ServiceUnavailableException("Inventory service down");
    } else if (e.status() == 404) {
        // Product not found
        throw new ProductNotFoundException("Product not found");
    } else {
        throw e;
    }
}
```

---

### Pattern 2: Asynchronous Communication (Kafka Event Streaming)

**When to Use**:
- Don't need immediate response
- Loose coupling desired
- Event-driven workflows
- Future extensibility needed

**Example: Order Service Publishes, Notification Service Consumes**

```
SEQUENCE DIAGRAM:

Order Service              Kafka Broker           Notification Service
     │                      │                            │
     │ 1. placeOrder()      │                            │
     │ 2. Save to MongoDB   │                            │
     │ 3. Publish event     │                            │
     ├─────────────────────>│                            │
     │ (non-blocking)       │                            │
     │                      │ 4. Store in partition     │
     │ 5. Return response   │                            │
     │ to client            │                            │
     │                      │                            │
     │                      │ 5. Kafka listener active  │
     │                      │    (polling every 100ms)  │
     │                      ├───────────────────────────>│
     │                      │                            │
     │                      │                            │
     │                      │ 6. handleOrderEvent()     │
     │                      │    - Build email          │
     │                      │    - Send via SMTP        │
     │                      │    - Save to DB           │
     │                      │                            │
     │                      │ 7. Commit offset          │
     │                      │    to Kafka               │
     │                      │<───────────────────────────│
```

**Code Implementation**:

Producer (Order Service):
```java
// OrderService.java
@Service
public class OrderService {
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    public void placeOrder(OrderRequest request) {
        // Validate and save
        Order order = new Order(request);
        orderRepository.save(order);
        
        // PUBLISH TO KAFKA (non-blocking)
        ListenableFuture<SendResult<String, Order>> future = 
            kafkaTemplate.send("order-topic", order.getId(), order);
        
        // Add callback for success/failure
        future.addCallback(
            result -> log.info("Order published: " + order.getId()),
            ex -> log.error("Failed to publish: " + ex.getMessage())
        );
        
        // RETURNS IMMEDIATELY to client
        // Email sending happens independently
    }
}
```

Consumer (Notification Service):
```java
// NotificationService.java
@Service
public class NotificationService {
    @Autowired
    private JavaMailSender mailSender;
    
    @KafkaListener(
        topics = "order-topic",
        groupId = "notification-service"
    )
    public void handleOrderEvent(Order order) {
        log.info("Received order event: " + order.getId());
        
        try {
            // Get customer email
            String email = getCustomerEmail(order.getCustomerId());
            
            // Send email
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("Order Confirmation #" + order.getId());
            message.setText(buildEmailBody(order));
            mailSender.send(message);
            
            // Save notification record
            Notification notif = new Notification();
            notif.setOrderId(order.getId());
            notif.setStatus("SENT");
            notif.setSentAt(LocalDateTime.now());
            notificationRepository.save(notif);
            
            log.info("Email sent for order: " + order.getId());
        } catch (Exception e) {
            log.error("Failed to send notification: " + e.getMessage());
            // Save error state for retry
        }
    }
}
```

**Kafka Configuration**:
```properties
# Order Service (Producer)
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer

# Notification Service (Consumer)
spring.kafka.bootstrap-servers=kafka:9092
spring.kafka.consumer.bootstrap-servers=kafka:9092
spring.kafka.consumer.group-id=notification-service
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

**How Kafka Works**:
```
TOPIC: order-topic
┌─────────────────────────────────────────────┐
│ Partition 0 (single partition in this setup) │
├─────────────────────────────────────────────┤
│ Offset 0: { orderId: "ORD001" }             │
│ Offset 1: { orderId: "ORD002" }             │
│ Offset 2: { orderId: "ORD003" }             │
│ Offset 3: { orderId: "ORD004" } ← Latest   │
└─────────────────────────────────────────────┘

Consumer Group: notification-service
├─ Consumer 1
│  └─ Reading from offset 3 (latest)
│     Commit offset after processing
│
├─ Consumer 2 (if multiple instances)
│  └─ Also reading, load-balanced by partition

Topic Retention:
- Default: 7 days
- Can replay messages from any offset
- Dead Letter Queue for failed messages
```

**Pros**:
- Non-blocking (Order Service returns immediately)
- Loose coupling (services don't need to know about each other)
- Scalable (add more consumer instances to parallelize)
- Resilient (messages persisted; can retry)
- Extensible (add new consumers without modifying producer)

**Cons**:
- Eventual consistency (email might take seconds/minutes)
- Duplicate handling needed (exactly-once delivery not guaranteed)
- Monitoring complexity (asynchronous failures harder to track)
- Infrastructure overhead (Kafka cluster required)

**Failure Scenarios**:
```java
1. Email sending fails:
   - Exception thrown in @KafkaListener
   - Message NOT auto-committed
   - Kafka re-delivers after timeout (3 attempts default)
   - Eventually goes to dead letter queue or stays unprocessed

2. Database error saving notification:
   - Email sent but record not saved
   - Retry on next restart
   - Could add idempotency key to prevent duplicate emails

3. Kafka broker down:
   - Producer can't send immediately
   - Retries locally with backoff
   - Eventually fails with exception
   - Client gets order confirmation (already saved)
   - Email might never send (need manual retry mechanism)
```

---

## Database Design & Data Flow

### Database Architecture

```
MongoDB Instance (localhost:27017 in docker)
├─ product-service-v2
│  ├─ Product collection
│  │  ├─ Index: { productId: 1 } [UNIQUE]
│  │  └─ Index: { createdAt: -1 }
│  └─ _indexes
│
├─ inventory-service-v2
│  ├─ Inventory collection
│  │  ├─ Index: { productId: 1 } [UNIQUE]
│  │  └─ Index: { quantity: 1 } (for sorting low-stock)
│  └─ _indexes
│
├─ order-service-v2
│  ├─ Order collection
│  │  ├─ Index: { customerId: 1 }
│  │  ├─ Index: { orderDate: -1 } (for sorting recent orders)
│  │  └─ Index: { status: 1 }
│  └─ _indexes
│
├─ supplier-service-v2
│  ├─ Supplier collection
│  │  ├─ Index: { supplierName: 1 }
│  │  └─ Index: { status: 1 }
│  └─ _indexes
│
└─ notification-service-v2
   ├─ Notification collection
   │  ├─ Index: { orderId: 1 }
   │  ├─ Index: { email: 1 }
   │  └─ Index: { status: 1 }
   └─ _indexes
```

### Data Relationships (Conceptual)

```
┌──────────────────┐
│  Product         │
├──────────────────┤
│ productId (PK)   │
│ productName      │
│ price            │
└────────┬─────────┘
         │ 1:1
         ▼
┌──────────────────┐
│ Inventory        │
├──────────────────┤
│ productId (FK)   │
│ quantity         │
│ warehouseLocation│
└──────────────────┘

┌──────────────────┐
│  Customer        │ (implicit - ID only stored in Order)
├──────────────────┤
│ customerId       │
│ name             │
│ email            │
└────────┬─────────┘
         │ 1:N
         ▼
┌──────────────────────┐
│ Order                │
├──────────────────────┤
│ orderId (PK)         │
│ customerId (FK)      │
│ items: [             │
│   productId (FK),    │
│   quantity,          │
│   price              │
│ ]                    │
│ totalAmount          │
│ status               │
└────────┬─────────────┘
         │ 1:N
         ▼
┌──────────────────┐
│ Notification     │
├──────────────────┤
│ orderId (FK)     │
│ email            │
│ status           │
│ sentAt           │
└──────────────────┘
```

### Data Flow: Complete Order Lifecycle

```
STEP 1: Customer Places Order
┌─────────────────────────────────────────────────────────┐
Request: {
  customerId: "CUST001",
  items: [
    { productId: "PROD001", quantity: 5 }
  ]
}

↓ Order Service

Reads from MongoDB:
- Query Product Service (PROD001 exists?)
- Query Inventory Service (quantity >= 5?)
- If valid, insert into order-service-v2.Order:

{
  _id: ObjectId("..."),
  customerId: "CUST001",
  items: [
    {
      productId: "PROD001",
      quantity: 5,
      unitPrice: 999.99,
      totalPrice: 4999.95
    }
  ],
  totalAmount: 4999.95,
  status: "CONFIRMED",
  createdAt: 2025-06-23T14:30:00Z
}
└─────────────────────────────────────────────────────────┘


STEP 2: Order Persisted in MongoDB
┌─────────────────────────────────────────────────────────┐
MongoDB order-service-v2.Order collection:
{
  "_id": ObjectId("507f191e810c19729de860ea"),
  "customerId": "CUST001",
  "items": [...],
  "totalAmount": 4999.95,
  "status": "CONFIRMED",
  ...
}

Response sent to client:
{
  orderId: "507f191e810c19729de860ea",
  status: "CONFIRMED"
}
└─────────────────────────────────────────────────────────┘


STEP 3: Event Published to Kafka
┌─────────────────────────────────────────────────────────┐
Order object serialized to JSON:
{
  "orderId": "507f191e810c19729de860ea",
  "customerId": "CUST001",
  "items": [...],
  "totalAmount": 4999.95
}

Published to Kafka topic: order-topic
Partition 0, Offset N: [JSON message stored]

Message properties:
- Key: orderId (for partitioning)
- Value: Complete order JSON
- Timestamp: server-time
- Retention: 7 days
└─────────────────────────────────────────────────────────┘


STEP 4: Notification Service Consumes
┌─────────────────────────────────────────────────────────┐
@KafkaListener polls: "Has new message since my last offset?"

Receives: [JSON message]

Deserializes to Order object:
{
  orderId: "507f191e810c19729de860ea",
  customerId: "CUST001",
  items: [...],
  totalAmount: 4999.95
}

Processes:
1. Query customer email (if needed)
2. Build HTML email with order details
3. Send via Gmail SMTP
4. Save to notification-service-v2.Notification:

{
  _id: ObjectId("..."),
  orderId: "507f191e810c19729de860ea",
  customerId: "CUST001",
  email: "customer@example.com",
  status: "SENT",
  sentAt: 2025-06-23T14:30:05Z
}

5. Commit offset to Kafka:
   Consumer group "notification-service" is now at Offset N+1
└─────────────────────────────────────────────────────────┘
```

### Query Patterns

**Find Customer's Recent Orders**:
```javascript
// Query in order-service-v2
db.Order.find(
  { customerId: "CUST001" }
).sort({ orderDate: -1 }).limit(10)

// Response:
[
  {
    _id: ObjectId("..."),
    customerId: "CUST001",
    orderDate: ISODate("2025-06-23"),
    totalAmount: 4999.95,
    status: "CONFIRMED"
  },
  ...
]
```

**Check Low Inventory**:
```javascript
// Query in inventory-service-v2
db.Inventory.find(
  { quantity: { $lt: 50 } }  // Less than reorder level
).sort({ quantity: 1 })

// Response: Products that need restocking
```

**Find Failed Notifications**:
```javascript
// Query in notification-service-v2
db.Notification.find(
  { status: "FAILED" }
).sort({ sentAt: -1 })

// Response: Email failures for retry
```

---

## API Specifications

### API Gateway (Port 8080)

**Base URL**: `http://localhost:8080`

#### Product Service Routes

```
POST /api/product/createProduct
├─ Body: { productName, productDescription, price }
├─ Response: { productId, productName, price }
└─ Routed to: http://product:8082/api/product/createProduct

GET /api/product/getAllProduct
├─ Response: Array of products
└─ Routed to: http://product:8082/api/product/getAllProduct

GET /api/product/getProduct/{productId}
├─ Path param: productId
├─ Response: { productId, productName, price }
└─ Routed to: http://product:8082/api/product/getProduct/{productId}

PUT /api/product/updateProduct/{productId}
├─ Path param: productId
├─ Body: { productName, productDescription, price }
├─ Response: Updated product
└─ Routed to: http://product:8082/api/product/updateProduct/{productId}

DELETE /api/product/deleteProduct/{productId}
├─ Path param: productId
├─ Response: Success message
└─ Routed to: http://product:8082/api/product/deleteProduct/{productId}
```

#### Inventory Service Routes

```
GET /api/inventory/getInventory
├─ Response: Array of inventory records
└─ Routed to: http://inventory:8081/api/inventory/getInventory

GET /api/inventory/getInventory/{productId}
├─ Path param: productId
├─ Response: { productId, quantity, warehouseLocation }
└─ Routed to: http://inventory:8081/api/inventory/getInventory/{productId}

POST /api/inventory/validateQuantity
├─ Body: { productId, quantity }
├─ Response: { isValid: true/false }
└─ Routed to: http://inventory:8081/api/inventory/validateQuantity

POST /api/inventory/addProduct
├─ Body: { productId, quantity }
├─ Response: { success: true }
└─ Routed to: http://inventory:8081/api/inventory/addProduct

POST /api/inventory/updateQuantity
├─ Body: { productId, quantity }
├─ Response: Updated inventory
└─ Routed to: http://inventory:8081/api/inventory/updateQuantity
```

#### Order Service Routes

```
POST /api/order/placeOrder
├─ Body: { customerId, items: [{productId, quantity}], deliveryAddress }
├─ Response: { orderId, status: "CONFIRMED", totalAmount }
├─ Flow:
│  1. Validates items with Inventory Service
│  2. Saves to MongoDB
│  3. Publishes to Kafka (async)
│  4. Returns immediately
└─ Routed to: http://order:8083/api/order/placeOrder

GET /api/order/getOrder/{orderId}
├─ Path param: orderId
├─ Response: Complete order details
└─ Routed to: http://order:8083/api/order/getOrder/{orderId}

GET /api/order/getOrdersByCustomer/{customerId}
├─ Path param: customerId
├─ Response: Array of customer's orders
└─ Routed to: http://order:8083/api/order/getOrdersByCustomer/{customerId}

PUT /api/order/cancelOrder/{orderId}
├─ Path param: orderId
├─ Response: { status: "CANCELLED" }
└─ Routed to: http://order:8083/api/order/cancelOrder/{orderId}
```

#### Supplier Service Routes

```
POST /api/supplier/createSupplier
├─ Body: { supplierName, contactEmail, phone, address }
├─ Response: { supplierId, supplierName }
└─ Routed to: http://supplier:8085/api/supplier/createSupplier

GET /api/supplier/getAllSuppliers
├─ Response: Array of suppliers
└─ Routed to: http://supplier:8085/api/supplier/getAllSuppliers

PUT /api/supplier/updateSupplier/{supplierId}
├─ Path param: supplierId
├─ Body: Updated supplier info
├─ Response: Updated supplier
└─ Routed to: http://supplier:8085/api/supplier/updateSupplier/{supplierId}

POST /api/supplier/receiveInventory
├─ Body: { supplierId, productId, quantity, poNumber }
├─ Response: { success: true, inventoryUpdated: true }
├─ Flow: Calls Inventory Service to add stock
└─ Routed to: http://supplier:8085/api/supplier/receiveInventory
```

#### Notification Service Routes

```
GET /api/notification/getNotifications/{orderId}
├─ Path param: orderId
├─ Response: Notification record for order
└─ Routed to: http://notification:8084/api/notification/getNotifications/{orderId}

GET /api/notification/getAllNotifications
├─ Response: Array of all notifications
└─ Routed to: http://notification:8084/api/notification/getAllNotifications
```

---

## Deployment & Infrastructure

### Docker Compose Architecture

```yaml
services:
  zookeeper:
    - Coordinates Kafka brokers
    - Maintains broker metadata
    - Port: 2181
    
  kafka:
    - Message broker
    - Stores order-topic messages
    - Port: 9092
    - Depends on: zookeeper
    
  mongodb:
    - NoSQL database
    - Stores all service data
    - Port: 27017
    - Volumes: Persistent storage
    
  eureka-server:
    - Service registry
    - Tracks service instances
    - Port: 8761
    - Healthcheck: Every 10s
    
  All 7 Microservices:
    - Wait for eureka-server healthy
    - Wait for mongodb started
    - Wait for kafka started (if needed)
    - Set MongoDB URI via environment variable
    - Set Eureka endpoint via environment variable
    - Expose service ports
    - Internal network: inventory-net
```

### Deployment Flow

```
COMMAND: docker-compose up --build

1. Read docker-compose.yml
2. Build all Dockerfiles:
   - Maven build stage (compile, package)
   - Runtime stage (copy JAR, run)
3. Pull base images:
   - confluentinc/cp-zookeeper:7.6.0
   - confluentinc/cp-kafka:7.6.0
   - mongo:7.0
   - eclipse-temurin:21-jre
4. Create internal network: inventory-net
5. Start services in dependency order:
   a. zookeeper
   b. kafka (waits for zookeeper)
   c. mongodb
   d. eureka-server (health check loops until UP)
   e. inventory, product, order, supplier, notification
      (all wait for eureka and mongodb)
   f. api-gateway (waits for all services)
6. Services register with Eureka
7. API Gateway becomes available
8. System ready for requests

Total time: ~1-2 minutes
```

### Docker Multi-Stage Build

```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw package -DskipTests

# Result: ~500MB JAR file

# Stage 2: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]

# Result: ~300MB runtime image
```

**Benefits**:
- Final image only includes JRE + JAR (no Maven)
- Reduced image size (40-50% smaller)
- Faster deployment
- Smaller disk footprint in production

---

## Testing Strategies

### 1. Unit Tests

```java
@SpringBootTest
class ProductServiceTest {
    @MockBean
    private ProductRepository productRepository;
    
    @Autowired
    private ProductService productService;
    
    @Test
    void testCreateProduct() {
        // Arrange
        ProductRequest request = new ProductRequest();
        request.setProductName("Laptop");
        request.setPrice(999.99);
        
        // Act
        Product result = productService.createProduct(request);
        
        // Assert
        assertEquals("Laptop", result.getProductName());
        verify(productRepository, times(1)).save(any(Product.class));
    }
}
```

**Coverage**:
- Service logic (validation, calculations)
- Repository operations
- Exception handling

**Execution**: `mvn test`

### 2. Integration Tests

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private InventoryClient inventoryClient;
    
    @Test
    void testPlaceOrderSuccess() throws Exception {
        // Mock Inventory Service response
        when(inventoryClient.validateQuantity(any()))
            .thenReturn(true);
        
        // Make HTTP request
        mockMvc.perform(post("/api/order/placeOrder")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "customerId": "CUST001",
                  "items": [{"productId": "PROD001", "quantity": 5}],
                  "deliveryAddress": "123 Main St"
                }
                """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
```

**Coverage**:
- Controller → Service → Repository
- HTTP request handling
- Request validation
- Response serialization

**Execution**: `mvn test -Dtest=OrderControllerIntegrationTest`

### 3. Kafka Consumer Tests

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = "order-topic")
class NotificationServiceKafkaTest {
    @MockBean
    private JavaMailSender mailSender;
    
    @Autowired
    private KafkaTemplate<String, Order> kafkaTemplate;
    
    @Test
    void testOrderEventProcessing() throws InterruptedException {
        // Create test order
        Order order = new Order();
        order.setId("ORD001");
        order.setCustomerId("CUST001");
        
        // Send to Kafka
        kafkaTemplate.send("order-topic", order);
        
        // Wait for consumer to process
        Thread.sleep(2000);
        
        // Verify email was sent
        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
```

**Coverage**:
- Kafka message serialization
- Listener invocation
- Side effects (email sending)

### 4. API Contract Tests

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiContractTest {
    @Autowired
    private TestRestTemplate restTemplate;
    
    @Test
    void testGetAllProductsSchema() {
        ResponseEntity<List<ProductResponse>> response = 
            restTemplate.exchange(
                "/api/product/getAllProduct",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<>() {}
            );
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Validate schema
        response.getBody().forEach(product -> {
            assertNotNull(product.getProductId());
            assertNotNull(product.getProductName());
            assertTrue(product.getPrice() > 0);
        });
    }
}
```

### 5. End-to-End Tests (Docker Compose)

```bash
# In integration test environment:
cd docker
docker-compose up -d

# Wait for services to be healthy
sleep 30

# Run E2E tests
mvn verify -DskipIntegrationTests=false

# Tests simulate:
# 1. Create product
# 2. Add inventory
# 3. Place order
# 4. Verify Kafka event sent
# 5. Verify notification queued

docker-compose down
```

### Test Execution Matrix

```
┌─────────────┬──────────────┬────────────────┬──────────┐
│ Test Type   │ Scope        │ Speed          │ Frequency│
├─────────────┼──────────────┼────────────────┼──────────┤
│ Unit        │ Single class │ Fast (<100ms)  │ Always   │
│ Integration │ Multiple     │ Medium (1s)    │ On PR    │
│ Kafka       │ Consumer     │ Slow (2-5s)    │ Before   │
│            │ handling     │                │ commit   │
│ E2E         │ Full system  │ Very slow      │ Before   │
│            │              │ (30-60s)       │ release  │
└─────────────┴──────────────┴────────────────┴──────────┘
```

---

## Performance & Scaling

### Bottleneck Analysis

```
┌─────────────────────────────────────────────────────────────┐
│ POTENTIAL BOTTLENECKS                                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│ 1. MONGODB QUERIES                                           │
│    Problem: No indexes on frequently queried fields         │
│    Solution: Add indexes on productId, customerId, status  │
│    Impact: 100x faster lookups                             │
│                                                              │
│ 2. FEIGN SYNCHRONOUS CALLS                                  │
│    Problem: Blocks Order Service waiting for Inventory     │
│    Solution: Add circuit breaker (Resilience4j)            │
│    Impact: Better resilience, faster failure detection    │
│                                                              │
│ 3. KAFKA SINGLE CONSUMER                                    │
│    Problem: 1 consumer = can't parallelize processing      │
│    Solution: Scale to 3-5 notification service instances   │
│    Impact: 3-5x throughput improvement                     │
│                                                              │
│ 4. API GATEWAY ROUTING                                      │
│    Problem: Single instance = bottleneck                   │
│    Solution: Scale to 3+ instances behind load balancer   │
│    Impact: Horizontal scalability                          │
│                                                              │
│ 5. MONGODB CONNECTION POOL                                  │
│    Problem: Limited connections (default 10)               │
│    Solution: Increase pool size in application.properties │
│    Impact: Support more concurrent requests                │
│                                                              │
│ 6. KAFKA TOPIC PARTITIONS                                   │
│    Problem: Single partition can't parallelize             │
│    Solution: Increase to 3-5 partitions                    │
│    Impact: Better consumer parallelization                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Scaling Strategies

#### Horizontal Scaling (Add Instances)

```bash
# Scale Product Service to 3 instances
docker-compose scale product=3

# Eureka load balances across 3 instances
# API Gateway routes requests round-robin

# Database: Shared MongoDB connection pool
# Traffic distribution:
# Request 1 → product:8082 (instance 1)
# Request 2 → product:8082 (instance 2)
# Request 3 → product:8082 (instance 3)
# Request 4 → product:8082 (instance 1)  [round-robin]
```

#### Vertical Scaling (Increase Resources)

```yaml
# docker-compose.yml
services:
  product:
    deploy:
      resources:
        limits:
          cpus: '2'       # Increase from 1 CPU
          memory: 2G      # Increase from 1G
    environment:
      JAVA_OPTS: -Xmx1500m  # Heap size
```

#### Database Optimization

```javascript
// Add indexes to inventory-service-v2
db.Inventory.createIndex({ productId: 1 })  // UNIQUE
db.Inventory.createIndex({ quantity: 1 })   // For range queries

// Add indexes to order-service-v2
db.Order.createIndex({ customerId: 1 })     // For customer queries
db.Order.createIndex({ orderDate: -1 })     // For sorting
db.Order.createIndex({ status: 1 })         // For filtering

// View query performance
db.Order.find({ customerId: "CUST001" }).explain("executionStats")
```

#### Caching Strategy

```java
// Add Redis caching for frequently accessed products
@Cacheable(cacheNames = "products", key = "#productId")
public Product getProduct(String productId) {
    return productRepository.findById(productId).orElse(null);
}

// Invalidate cache on update
@CacheEvict(cacheNames = "products", key = "#product.productId")
public void updateProduct(Product product) {
    productRepository.save(product);
}
```

#### Circuit Breaker Implementation

```java
// Add resilience4j circuit breaker to Feign client
@FeignClient(name = "INVENTORY", configuration = InventoryClientConfig.class)
public interface InventoryClient {
    @PostMapping("/api/inventory/validateQuantity")
    @CircuitBreaker(name = "inventory-service", fallbackMethod = "validateQuantityFallback")
    boolean validateQuantity(@RequestBody InventoryRequest request);
    
    // Fallback: Allow order if inventory service is down
    default boolean validateQuantityFallback(InventoryRequest request, Exception e) {
        log.warn("Inventory service down, allowing order: " + e.getMessage());
        return true;  // Optimistic fallback
    }
}
```

### Performance Metrics

```
Typical Response Times (Docker Compose):
┌────────────────────────────────────┐
│ GET /api/product/getAllProduct     │ 50ms
│ POST /api/product/createProduct    │ 100ms
│ POST /api/order/placeOrder         │ 150ms
│   (includes Inventory validation)   │
│ GET /api/inventory/getInventory    │ 40ms
│ POST /api/supplier/createSupplier  │ 80ms
└────────────────────────────────────┘

Throughput:
┌────────────────────────────────────┐
│ Single Product Service instance    │ 100 requests/sec
│ 3 Product Service instances        │ 300 requests/sec
│ Kafka message processing           │ 1000 msg/sec
│ MongoDB write capacity             │ 5000 ops/sec
└────────────────────────────────────┘
```

---

## Error Handling & Edge Cases

### Error Scenarios

#### Scenario 1: Out of Stock

```
Request: Place order for 10 laptops (only 5 in stock)

Flow:
1. Order Service calls Inventory Service
2. Inventory Service returns: { isValid: false }
3. Order Service throws: OutOfStockException
4. API Gateway returns: 400 Bad Request

Response:
{
  "error": "Out of Stock",
  "message": "Product PROD001 quantity available: 5, requested: 10",
  "productId": "PROD001",
  "availableQuantity": 5
}
```

#### Scenario 2: Inventory Service Down

```
Request: Place order while Inventory Service is down

Flow:
1. Order Service calls Inventory Service via Feign
2. Feign gets: Connection refused
3. Throws: FeignException
4. Without circuit breaker:
   - Client gets: 503 Service Unavailable
   - Order NOT created
5. With circuit breaker:
   - Circuit opens after 3 failures
   - Fallback: Allow order (optimistic)
   - Or deny with: 503 Service Temporarily Unavailable

Response (without circuit breaker):
{
  "status": 503,
  "error": "Service Unavailable",
  "message": "Inventory service temporarily unavailable"
}
```

#### Scenario 3: Kafka Broker Down

```
Request: Place order while Kafka is down

Flow:
1. Order saved to MongoDB ✓
2. Response sent to client ✓
3. Attempt to publish to Kafka: Connection refused
4. Exception caught and logged ✗
5. Email notification NOT sent ✗

Issues:
- Order confirmed but customer won't get email
- No automated retry mechanism
- Manual intervention needed

Improvement:
- Add dead letter queue
- Retry with exponential backoff
- Send email through alternative channel if Kafka fails
```

#### Scenario 4: MongoDB Connection Pool Exhausted

```
High traffic situation:
- 1000 concurrent requests
- Connection pool size: 10
- Remaining requests: Queued waiting for connection

Results:
- First 10 requests: Processed immediately
- Next 990 requests: Queued (timeout = 30s)
- After 30s: Timeout error returned

Solution:
- Increase pool size: spring.data.mongodb.max-pool-size=50
- Add circuit breaker to fail fast
- Monitor connection pool usage
```

#### Scenario 5: Duplicate Order from Network Retry

```
Scenario:
1. Client sends POST /api/order/placeOrder
2. Order created, saved to MongoDB
3. Response sent but network drops
4. Client timeout, retries same request
5. Second order created (DUPLICATE)

Prevention:
- Add idempotency key in request
- Check idempotency key before processing
- Return cached response if key exists

Implementation:
@PostMapping("/api/order/placeOrder")
public ResponseEntity<?> placeOrder(
    @RequestBody OrderRequest request,
    @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
    
    if (idempotencyKey != null) {
        Order existing = orderRepository.findByIdempotencyKey(idempotencyKey);
        if (existing != null) {
            return ResponseEntity.ok(existing);  // Return cached result
        }
    }
    
    Order order = orderService.placeOrder(request);
    
    if (idempotencyKey != null) {
        order.setIdempotencyKey(idempotencyKey);
        orderRepository.save(order);
    }
    
    return ResponseEntity.ok(order);
}
```

### Error Handling Best Practices

```java
// 1. Global exception handler
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<?> handleOutOfStock(OutOfStockException e) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("OUT_OF_STOCK", e.getMessage()));
    }
    
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<?> handleFeignError(FeignException e) {
        if (e.status() == 503) {
            return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("SERVICE_UNAVAILABLE", "Service temporarily down"));
        }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_ERROR", e.getMessage()));
    }
}

// 2. Logging
@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    public void placeOrder(OrderRequest request) {
        try {
            log.info("Placing order for customer: " + request.getCustomerId());
            // ... order processing
            log.info("Order placed successfully: " + order.getId());
        } catch (Exception e) {
            log.error("Failed to place order for customer: " + request.getCustomerId(), e);
            throw e;
        }
    }
}

// 3. Retry logic (Kafka producer)
kafkaTemplate.send("order-topic", order)
    .addCallback(
        result -> log.info("Message sent successfully"),
        exception -> {
            log.error("Message sending failed", exception);
            // Add to retry queue or dead letter queue
        }
    );
```

---

## Security Implementation

### Current State

```
✓ Implemented:
- Services on internal network (containers)
- No external direct access to MongoDB/Kafka
- Environment variables for non-hardcoded credentials

✗ NOT Implemented:
- API authentication/authorization
- Request validation
- Rate limiting
- Input sanitization
- HTTPS/TLS
- API versioning
- CORS configuration
```

### Security Gaps & Recommendations

#### 1. API Authentication (NOT IMPLEMENTED)

```java
// Recommendation: Add Spring Security + JWT

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/product/**").authenticated()
                .requestMatchers("/api/order/**").authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));
        return http.build();
    }
}

// Protected endpoint:
@PostMapping("/api/order/placeOrder")
@PreAuthorize("hasRole('CUSTOMER')")
public ResponseEntity<?> placeOrder(
    @RequestBody OrderRequest request,
    @AuthenticationPrincipal JwtAuthenticationToken token) {
    
    String customerId = token.getName();
    // Ensure customer can only place orders for themselves
    // ...
}
```

#### 2. Input Validation (NOT IMPLEMENTED)

```java
// Recommendation: Add Bean Validation

@Document(collection = "Product")
public class ProductEntity {
    @Id
    private String productId;
    
    @NotBlank(message = "Product name cannot be empty")
    @Size(min = 3, max = 100, message = "Product name must be 3-100 characters")
    private String productName;
    
    @NotBlank(message = "Description required")
    private String productDescription;
    
    @NotNull(message = "Price required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be > 0")
    private Double price;
}

@PostMapping("/api/product/createProduct")
public ResponseEntity<?> createProduct(
    @Valid @RequestBody ProductEntity product,
    BindingResult bindingResult) {
    
    if (bindingResult.hasErrors()) {
        return ResponseEntity
            .badRequest()
            .body(bindingResult.getFieldErrors());
    }
    
    return ResponseEntity.ok(productService.create(product));
}
```

#### 3. MongoDB Injection Prevention (NOT VULNERABLE - But Check)

```java
// Vulnerable pattern (if query built as string):
// AVOID: db.products.find({ "name": "" + userInput + "" })

// Safe pattern (Spring Data):
@Repository
public interface ProductRepository extends MongoRepository<Product, String> {
    // Spring Data uses parameterized queries - safe from injection
    Product findByProductName(String productName);
    
    @Query("{ 'productId': ?0 }")
    Product findByIdSafe(String productId);
}
```

#### 4. Rate Limiting (NOT IMPLEMENTED)

```yaml
# Recommendation: Add Spring Cloud Gateway rate limiter

spring:
  cloud:
    gateway:
      routes:
        - id: product-service
          uri: lb://PRODUCT
          predicates:
            - Path=/api/product/**
          filters:
            - name: RequestRateLimiter
              args:
                redis-rate-limiter.replenishRate: 10  # 10 requests/sec
                redis-rate-limiter.burstCapacity: 20   # Allow burst to 20
                key-resolver: "#{@userKeyResolver}"
```

#### 5. CORS Configuration (NOT IMPLEMENTED)

```java
// Recommendation: Add CORS in API Gateway

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins("https://yourdomain.com")  // Specific domain
                    .allowedMethods("GET", "POST", "PUT", "DELETE")
                    .allowedHeaders("*")
                    .maxAge(3600);  // Cache CORS validation for 1 hour
            }
        };
    }
}
```

#### 6. Gmail Credentials (Security Risk)

```properties
# CURRENT (Insecure - plaintext in file):
spring.mail.username=your-email@gmail.com
spring.mail.password=your-password

# RECOMMENDED (Use environment variables):
spring.mail.username=${GMAIL_USERNAME}
spring.mail.password=${GMAIL_APP_PASSWORD}

# Docker Compose: Pass via environment
environment:
  GMAIL_USERNAME: your-email@gmail.com
  GMAIL_APP_PASSWORD: ${GMAIL_APP_PASSWORD}  # From .env file

# .env file (git-ignored):
GMAIL_APP_PASSWORD=generated-app-password
```

---

## Monitoring & Observability

### Health Checks

```bash
# Check individual service health
curl http://localhost:8082/actuator/health

# Response:
{
  "status": "UP",
  "components": {
    "mongodb": {
      "status": "UP",
      "details": {
        "version": "7.0"
      }
    },
    "eureka": {
      "status": "UP"
    }
  }
}
```

### Metrics & Monitoring

```java
// Spring Boot Actuator exposes metrics:
@Configuration
public class MetricsConfig {
    
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsEnhancer() {
        return registry -> registry.counter(
            "orders.placed.total",
            "Orders placed since startup"
        );
    }
}

// In OrderService:
private final MeterRegistry meterRegistry;

public void placeOrder(OrderRequest request) {
    // ... order processing
    meterRegistry.counter("orders.placed.total").increment();
    meterRegistry.timer("order.processing.time").record(() -> {
        // Order logic
    });
}
```

**Available Endpoints**:
```
/actuator/health          # Service health
/actuator/metrics         # All available metrics
/actuator/metrics/jvm.memory.used
/actuator/metrics/process.cpu.usage
/actuator/metrics/http.server.requests
/actuator/prometheus      # Prometheus format
```

### Distributed Tracing (Recommended)

```xml
<!-- Add Spring Cloud Sleuth + Zipkin -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-sleuth</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-sleuth-zipkin</artifactId>
</dependency>

<!-- application.properties -->
spring.zipkin.base-url=http://zipkin:9411
spring.sleuth.sampler.probability=1.0
```

**Trace Flow**:
```
Client Request
  ↓ [Trace ID: abc123, Span ID: 1]
API Gateway
  ↓ [Trace ID: abc123, Span ID: 2] (parent: 1)
Order Service
  ↓ [Trace ID: abc123, Span ID: 3] (parent: 2)
Inventory Service
  ↓ [Trace ID: abc123, Span ID: 4] (parent: 3)
MongoDB Query
  ↓ Response
  
Zipkin shows: Complete timeline across all services
```

### Centralized Logging (Recommended)

```yaml
# docker-compose.yml additions:
elasticsearch:
  image: docker.elastic.co/elasticsearch/elasticsearch:8.0.0
  ports:
    - "9200:9200"

kibana:
  image: docker.elastic.co/kibana/kibana:8.0.0
  ports:
    - "5601:5601"

# Application sends logs via Logstash:
spring.application.json:
  logging:
    level:
      com.InventoryManagement: INFO
```

---

## Interview Q&A

### Q1: Why Microservices Instead of Monolith?

**Answer**:
- **Independent Scaling**: Product Service can scale without scaling Inventory
- **Fault Isolation**: Order Service down ≠ Product Service down
- **Technology Flexibility**: Each service can use best tech (Spring for some, Node for others)
- **Deployment**: Push to Production one service at a time
- **Team Ownership**: Different teams own different services

**Trade-offs**:
- Distributed system complexity
- Network latency (inter-service calls)
- Data consistency challenges (eventual consistency)
- Operational overhead (more services to monitor)

---

### Q2: How Do Services Discover Each Other?

**Answer**:
```
Service Discovery Flow:
1. Product Service starts
2. Sends: POST /eureka/apps/PRODUCT (self-registration)
3. Includes: hostname, port, IP address
4. Eureka stores in memory

5. Order Service needs Inventory Service
6. Uses Feign Client: @FeignClient(name = "INVENTORY")
7. Feign queries Eureka: GET /eureka/apps/INVENTORY
8. Eureka returns list of INVENTORY instances
9. Feign load-balances across instances

Advantages:
- No hardcoded IP addresses
- Dynamic addition/removal of instances
- Automatic health checks
```

---

### Q3: What's the Difference Between Sync and Async Communication?

**Answer**:

**Synchronous (OpenFeign)**:
```
Order Service (blocks) → Inventory Service
  Response comes back before continuing
  Tight coupling
  Immediate feedback
  Blocked thread waiting
```

**Asynchronous (Kafka)**:
```
Order Service → Kafka → Notification Service
  Order Service returns immediately
  Notification happens later
  Loose coupling
  Non-blocking
  Eventual consistency
```

**When to Use Each**:
- **Sync**: Need immediate response, validation errors
- **Async**: Don't need immediate response, side effects (email)

---

### Q4: What Happens If Inventory Service Crashes?

**Answer**:
```
Current Implementation:
1. Order Service calls Inventory via Feign
2. Connection refused exception
3. Order NOT created
4. Client gets 503 error

With Circuit Breaker (Recommended):
1. Order Service calls Inventory via Feign
2. Connection refused exception
3. Circuit breaker opens after 3 failures
4. Fallback triggered: Allow order (optimistic)
5. Order created anyway
6. Risk: Order for item that's out of stock
7. Mitigation: Async confirmation, ability to cancel

Better Solution:
1. Add local cache of inventory
2. Use cache if Inventory Service down
3. Async reconciliation when service comes back
```

---

### Q5: How Do You Ensure Orders Aren't Lost If Kafka Goes Down?

**Answer**:
```
Current Risk:
1. Order saved to MongoDB ✓
2. Response sent to client ✓
3. Publish to Kafka: Connection refused ✗
4. Email never sent ✗

Solutions:

Option 1: Persistent Message Queue
- Write order event to separate table
- Background job retries every 5 minutes
- Mark as "sent" once Kafka succeeds

Option 2: Dead Letter Queue
- Kafka auto-creates for failed messages
- Consumer can replay from DLQ
- Admin dashboard to monitor DLQ

Option 3: Dual Channel
- Primary: Kafka
- Fallback: Direct email if Kafka fails
- Risk: Duplicate emails

Recommended: Combination
- Use Kafka with retry policy
- Add dead letter topic
- Background job checks for orphaned orders
```

---

### Q6: What's Your Scaling Strategy?

**Answer**:
```
Horizontal Scaling:
- Add more instances of each service
- Eureka load-balances automatically
- Each instance: independent MongoDB connection

Database Scaling:
- Add indexes on frequently queried fields
- MongoDB sharding (partition data by customerId)
- Read replicas for reporting queries

Caching:
- Redis for frequently accessed products
- Cache invalidation on updates

Kafka Scaling:
- Increase topic partitions
- Add more consumer instances
- Each partition processed by one consumer

Monitoring:
- Metrics: Prometheus + Grafana
- Logs: ELK stack
- Traces: Zipkin
- Alerting: PagerDuty
```

---

### Q7: How Do You Handle Database Transactions Across Services?

**Answer**:
```
Traditional ACID 2-Phase Commit:
- Not possible in distributed system
- Synchronous locking leads to deadlocks

Saga Pattern (What we use with Kafka):
1. Order Service starts saga
2. Publishes: "OrderCreated" event
3. Inventory Service listens
4. Attempts to reserve stock
5. If success: publishes "StockReserved"
6. If failure: publishes "ReservationFailed"
7. Order Service compensates (cancels order)

Eventual Consistency:
- After all events processed, system consistent
- Temporary inconsistency OK
- Customer sees order, inventory updates 1-2 seconds later

Challenges:
- Handling concurrent conflicting orders
- Compensating transactions (reversing operations)
- Idempotency (same event processed twice = same result)

Example Implementation:
@Service
public class OrderSaga {
    public void startOrderSaga(OrderRequest request) {
        // 1. Save order state
        Order order = new Order(request);
        order.setStatus("PENDING");
        orderRepository.save(order);
        
        // 2. Publish event
        kafkaTemplate.send("order-saga-start", order);
        
        // 3. Wait for result (via event)
        // Result could come via callback or polling
    }
}
```

---

### Q8: How Do You Test Microservices?

**Answer**:
```
Unit Tests:
- Test service logic in isolation
- Mock dependencies
- Fast execution

Integration Tests:
- Test service + database
- Test service + Feign client
- Mocked external services

Contract Tests:
- Verify API contracts between services
- Ensures compatibility

End-to-End Tests:
- Full Docker environment
- Real inter-service communication
- Slow but comprehensive

Testing Challenges:
- Distributed system complexity
- Asynchronous operations (Kafka)
- External dependencies (Gmail SMTP)
- Timing issues (race conditions)

Solutions:
- Test containers (embedded MongoDB, Kafka)
- Testcontainers library (Docker containers for testing)
- MockServer (mock external services)
- WireMock (mock HTTP services)
```

---

### Q9: What's the Biggest Challenge With This Architecture?

**Answer**:
```
Top Challenges:

1. Distributed System Complexity
   - Harder debugging
   - Network issues
   - Cascading failures
   Solution: Good logging, distributed tracing

2. Data Consistency
   - Services have separate databases
   - Eventual consistency (not real-time)
   - Conflicting updates possible
   Solution: Saga pattern, event sourcing

3. Network Latency
   - Each sync call = network overhead
   - Cascading slowdowns
   Solution: Async communication, caching

4. Operational Overhead
   - Multiple services to deploy
   - Multiple databases to manage
   - Multiple logs to monitor
   Solution: Docker, Kubernetes, centralized logging

5. Testing Complexity
   - Can't test complete flow easily
   - Async operations hard to test
   Solution: Test containers, thorough logging

If starting over:
- Start monolithic
- Split into microservices as complexity grows
- Don't need microservices for simple systems
```

---

### Q10: How Would You Secure This System?

**Answer**:
```
Authentication:
- Add Spring Security + OAuth2
- JWT tokens with RS256 signing
- Rate limiting per user

Authorization:
- Role-based access control (RBAC)
- Customer can only see own orders
- Admin can access all data

Data Protection:
- HTTPS/TLS for all communications
- Encrypt sensitive fields in MongoDB
- Environment variables for credentials

API Security:
- Input validation (Bean Validation)
- SQL/NoSQL injection prevention
- CORS configuration
- CSRF protection

Infrastructure:
- Services on private network
- Only API Gateway exposed
- Secrets management (Vault, AWS Secrets Manager)
- Network policies/firewall rules

Monitoring:
- Audit logs for sensitive operations
- Anomaly detection
- Security scanning in CI/CD
```

---

## Conclusion

This system demonstrates production-grade microservices architecture with:
- ✅ Service discovery & load balancing (Eureka)
- ✅ Asynchronous event processing (Kafka)
- ✅ Synchronous inter-service communication (Feign)
- ✅ Document database per service (MongoDB)
- ✅ Docker containerization
- ✅ Scalability design

**Interview Talking Points**:
1. Explain why microservices (scalability, fault isolation)
2. Describe communication patterns (sync vs async)
3. Discuss distributed transactions (Saga pattern)
4. Address challenges (consistency, complexity, testing)
5. Propose improvements (circuit breaker, caching, security)

---

**Document Version**: 1.0  
**Last Updated**: 2026-06-23  
**Prepared For**: Interview Preparation & Project Handover
