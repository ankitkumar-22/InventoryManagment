package com.InventoryManagement.Order.Repository;

import com.InventoryManagement.Order.Entity.OrderEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<OrderEntity, String> {
}
