package com.InventoryManagement.Supplier.Repository;

import com.InventoryManagement.Supplier.Entity.SupplierEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SupplierRepository extends MongoRepository<SupplierEntity, String> {
}
