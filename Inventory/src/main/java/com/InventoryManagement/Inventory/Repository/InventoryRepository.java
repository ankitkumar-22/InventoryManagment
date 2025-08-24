package com.InventoryManagement.Inventory.Repository;

import com.InventoryManagement.Inventory.Entity.InventoryEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryRepository extends MongoRepository<InventoryEntity,String> {
}
