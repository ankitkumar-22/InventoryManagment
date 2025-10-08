package com.InventoryManagement.Supplier.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@Document(collection = "Product")
public class ProductEntity {
    @Id
    private String productId;
    private String productName;
    private String productDescription;
}
