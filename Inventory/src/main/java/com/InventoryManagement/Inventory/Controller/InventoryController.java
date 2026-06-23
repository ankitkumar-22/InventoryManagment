package com.InventoryManagement.Inventory.Controller;

import com.InventoryManagement.Inventory.DTO.InventoryRequest;
import com.InventoryManagement.Inventory.Entity.InventoryEntity;
import com.InventoryManagement.Inventory.Service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService inventoryService;

    @PostMapping("/addProduct")
    @ResponseStatus(HttpStatus.CREATED)
    public void addProduct(@RequestBody InventoryRequest inventoryRequest){
        inventoryService.addProduct(inventoryRequest);
    }

    @PostMapping("/updateQuantity")
    @ResponseStatus(HttpStatus.OK)
    public void updateQuantity(@RequestParam String id, @RequestParam Integer quantity){
        inventoryService.updateQuantity(id,quantity);
    }

    @DeleteMapping("/deleteProduct")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProduct(@RequestParam String id){
        inventoryService.deleteProduct(id);
    }

    @GetMapping("/getQuantity")
    @ResponseStatus(HttpStatus.OK)
    public Integer getQuantity(@RequestParam String id){
        return inventoryService.getQuantity(id);
    }

    @GetMapping("/validateQuantity")
    @ResponseStatus(HttpStatus.OK)
    public boolean validateQuantity(@RequestParam String id, @RequestParam Integer quantity){
        return inventoryService.validateQuantity(id,quantity);
    }
}
