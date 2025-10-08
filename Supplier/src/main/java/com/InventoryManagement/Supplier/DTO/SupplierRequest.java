package com.InventoryManagement.Supplier.DTO;

import com.InventoryManagement.Supplier.Entity.ProductEntity;

import java.util.List;

public record SupplierRequest(String id, String name, String email, List<ProductEntity> products) {
}
