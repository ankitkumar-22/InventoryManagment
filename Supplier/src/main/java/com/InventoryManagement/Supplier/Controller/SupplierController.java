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

    @PostMapping("/addSupplier")
    @ResponseStatus(HttpStatus.CREATED)
    public void addSupplier(@RequestBody SupplierRequest supplierRequest){
        supplierService.addSupplier(supplierRequest);
    }

    @DeleteMapping("/deleteSupplier")
    @ResponseStatus(HttpStatus.OK)
    public void deleteSupplier(@RequestParam String id){
        supplierService.deleteSupplier(id);
    }

    @PostMapping("/addProductBySupplier")
    @ResponseStatus(HttpStatus.CREATED)
    public void addProductBySupplier(@RequestParam String id,@RequestBody ProductEntity productEntity){
        supplierService.addProductBySupplier(id,productEntity);
    }

    @DeleteMapping("/deleteProductBySupplier")
    @ResponseStatus(HttpStatus.OK)
    public void deleteProductBySupplier(@RequestParam String supplierId,@RequestParam String productId){
        supplierService.deleteProductBySupplier(supplierId,productId);
    }

    @PostMapping("/updateQuantity")
    @ResponseStatus(HttpStatus.OK)
    public void updateQuantity(@RequestParam String id, @RequestParam Integer quantity){
        supplierService.updateQuantity(id,quantity);
    }
}
