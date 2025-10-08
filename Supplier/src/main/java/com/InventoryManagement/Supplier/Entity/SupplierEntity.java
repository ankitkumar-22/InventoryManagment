package com.InventoryManagement.Supplier.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@Document(collection = "Supplier")
public class SupplierEntity {
    @Id
    private String id;
    private String name;
    private String email;
    private List<ProductEntity> products;
}
