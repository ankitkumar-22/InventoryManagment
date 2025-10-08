package com.InventoryManagement.Order.Controller;

import com.InventoryManagement.Order.DTO.OrderRequest;
import com.InventoryManagement.Order.DTO.OrderResponse;
import com.InventoryManagement.Order.Service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/order")
public class OrderController {
    private final OrderService orderService;
    @PostMapping
    @RequestMapping("/placeOrder")
    @ResponseStatus(HttpStatus.CREATED)
    public void placeOrder(@RequestBody OrderRequest orderRequest){
        orderService.placeOrder(orderRequest);
    }

    @GetMapping
    @RequestMapping("/getOrderDetailsById")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrderDetailsById(@RequestParam String id){
        return orderService.getOrderDetailsById(id);
    }
    @DeleteMapping
    @RequestMapping("/cancelOrder")
    @ResponseStatus(HttpStatus.OK)
    public void cancelOrder(@RequestParam String id){
        orderService.cancelOrder(id);
    }
}
