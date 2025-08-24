package com.InventoryManagement.Product.Controller;

import com.InventoryManagement.Product.Client.InventoryClient;
import com.InventoryManagement.Product.DTO.InventoryRequest;
import com.InventoryManagement.Product.DTO.ProductRequest;
import com.InventoryManagement.Product.Service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;
    private final InventoryClient inventoryClient;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping("/createProduct")
    public void createProduct(@RequestBody ProductRequest productRequest){
        productService.createProduct(productRequest);
        InventoryRequest inventoryRequest = new InventoryRequest(productRequest.id(),productRequest.name());
        inventoryClient.addProduct(inventoryRequest);
    }

    @DeleteMapping
    @RequestMapping("/deleteProduct")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProduct(@RequestParam String id){
        productService.deleteProduct(id);
        inventoryClient.deleteProduct(id);
    }
}
