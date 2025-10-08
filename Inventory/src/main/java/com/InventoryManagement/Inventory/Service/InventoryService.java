package com.InventoryManagement.Inventory.Service;

import com.InventoryManagement.Inventory.DTO.InventoryRequest;
import com.InventoryManagement.Inventory.Entity.InventoryEntity;
import com.InventoryManagement.Inventory.Repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

@Service
@Slf4j
@RequiredArgsConstructor
public class InventoryService {
    private final InventoryRepository inventoryRepository;

    public void addProduct(InventoryRequest inventoryRequest){
        InventoryEntity inventory = InventoryEntity.builder()
                .id(inventoryRequest.id())
                .name(inventoryRequest.name())
                .quantity(0)
                .build();
        inventoryRepository.save(inventory);
    }

    public void updateQuantity(String id,Integer quantity){
        InventoryEntity inventory = inventoryRepository.findById(id).orElseThrow(() -> new RuntimeException("No Product with such ID exists"));
        inventory.setQuantity(quantity);
        inventoryRepository.save(inventory);
    }

    public void deleteProduct(String id){
        if(!inventoryRepository.existsById(id)){
            throw new RuntimeException("No such Product exists with given ID");
        }
        inventoryRepository.deleteById(id);
    }

    public Integer getQuantity(String id){
        InventoryEntity inventory = inventoryRepository.findById(id).orElseThrow(() -> new RuntimeException("No such Product exists with given ID"));
        return inventory.getQuantity();
    }

    public boolean validateQuantity(String id, Integer quantity){
        InventoryEntity inventory = inventoryRepository.findById(id).orElseThrow(() -> new RuntimeException("No such Product exists with given ID"));
        return inventory.getQuantity() >= quantity;
    }
}
