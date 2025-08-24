package com.InventoryManagement.Inventory.Entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "Inventory")
public class InventoryEntity {
    @Id
    private String id;
    private String name;
    private Integer quantity;
}
