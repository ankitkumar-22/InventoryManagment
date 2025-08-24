package com.InventoryManagement.Product.Client;

import com.InventoryManagement.Product.DTO.InventoryRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "inventory", url = "${inventory.url}")
public interface InventoryClient {

    @RequestMapping(method = RequestMethod.POST, value = "/api/inventory/addProduct")
    public void addProduct(@RequestBody InventoryRequest inventoryRequest);
    @RequestMapping(method = RequestMethod.DELETE, value = "/api/inventory/deleteProduct")
    public void deleteProduct(@RequestParam String id);
}
