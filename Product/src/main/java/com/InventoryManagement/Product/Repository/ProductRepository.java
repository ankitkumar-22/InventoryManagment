package com.InventoryManagement.Product.Repository;

import com.InventoryManagement.Product.Entity.ProductEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductRepository extends MongoRepository<ProductEntity,String> {
}
