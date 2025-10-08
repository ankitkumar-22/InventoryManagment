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

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping("/addProduct")
    public void addProduct(@RequestBody InventoryRequest inventoryRequest){
        inventoryService.addProduct(inventoryRequest);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(("/updateQuantity"))
    public void updateQuantity(@RequestParam String id, @RequestParam Integer quantity){
        inventoryService.updateQuantity(id,quantity);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping("/deleteProduct")
    public void deleteProduct(@RequestParam String id){
        inventoryService.deleteProduct(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping("/getQuantity")
    public Integer getQuantity(@RequestParam String id){
        return inventoryService.getQuantity(id);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping("/validateQuantity")
    public boolean validateQuantity(@RequestParam String id, @RequestParam Integer quantity){
        return inventoryService.validateQuantity(id,quantity);
    }
}
