package com.InventoryManagement.Supplier.Controller;


import com.InventoryManagement.Supplier.DTO.SupplierRequest;
import com.InventoryManagement.Supplier.Entity.ProductEntity;
import com.InventoryManagement.Supplier.Service.SupplierService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/supplier")
public class SupplierController {
    private final SupplierService supplierService;

    @PostMapping
    @RequestMapping("/addSupplier")
    @ResponseStatus(HttpStatus.CREATED)
    public void addSupplier(@RequestBody SupplierRequest supplierRequest){
        supplierService.addSupplier(supplierRequest);
    }

    @RequestMapping("/deleteSupplier")
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void deleteSupplier(@RequestParam String id){
        supplierService.deleteSupplier(id);
    }

    @RequestMapping("/addProductBySupplier")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void addProductBySupplier(@RequestParam String id,@RequestBody ProductEntity productEntity){
        supplierService.addProductBySupplier(id,productEntity);
    }

    @RequestMapping("/deleteProductBySupplier")
    @DeleteMapping
    @ResponseStatus(HttpStatus.OK)
    public void deleteProductBySupplier(@RequestParam String supplierId,@RequestParam String productId){
        supplierService.deleteProductBySupplier(supplierId,productId);
    }

    @RequestMapping("/updateQuantity")
    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void updateQuantity(@RequestParam String id, @RequestParam Integer quantity){
        supplierService.updateQuantity(id,quantity);
    }
}
