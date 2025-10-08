package com.InventoryManagement.Supplier.Client;

import com.InventoryManagement.Supplier.DTO.ProductRequest;
import com.InventoryManagement.Supplier.Entity.ProductEntity;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "product" , url = "${product.url}")
public interface ProductClient {
    @RequestMapping(method = RequestMethod.POST, value = "/api/product/createProduct")
    public void addProduct(@RequestBody ProductRequest productRequest);

    @RequestMapping(method = RequestMethod.DELETE, value = "/api/product/deleteProduct")
    public void deleteProduct(@RequestParam String id);
}
