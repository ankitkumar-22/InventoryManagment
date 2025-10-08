package com.InventoryManagement.Order.Service;

import com.InventoryManagement.Order.Client.InventoryClient;
import com.InventoryManagement.Order.DTO.OrderRequest;
import com.InventoryManagement.Order.DTO.OrderResponse;
import com.InventoryManagement.Order.Entity.OrderEntity;
import com.InventoryManagement.Order.Repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.aggregation.ArrayOperators;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final KafkaTemplate<String,OrderRequest> kafkaTemplate;
    public void placeOrder(OrderRequest orderRequest){
        OrderEntity orderEntity = OrderEntity.builder()
                .id(orderRequest.id())
                .quantity(orderRequest.quantity())
                .payment(orderRequest.payment())
                .productId(orderRequest.productId())
                .shippingDetails(orderRequest.shippingDetails())
                .customerId(orderRequest.customerId())
                .customerContact(orderRequest.customerContact())
                .build();
        if(orderEntity.getProductId().size() != orderEntity.getQuantity().size()) {
            throw new RuntimeException("Order validation failed!");
        }
        // validation for enough quantity there in inventory to be added
        for(int i=0;i<orderEntity.getProductId().size();i++){
            String productId=orderEntity.getProductId().get(i);
            Integer productQuantity = Integer.parseInt(orderEntity.getQuantity().get(i));
            if(!inventoryClient.validateQuantity(productId,productQuantity)){
                throw new RuntimeException("Invalid Quantity");
            }
        }
        orderRepository.save(orderEntity);
        kafkaTemplate.send("order-topic",orderRequest);
    }

    public OrderResponse getOrderDetailsById(String id){
        OrderEntity orderEntity = orderRepository.findById(id).orElseThrow(() -> new RuntimeException("No Order found with given ID"));
        //make order response
        return new OrderResponse(orderEntity.getId(),orderEntity.getProductId(),orderEntity.getQuantity(),orderEntity.getShippingDetails(),orderEntity.isPayment(), orderEntity.getCustomerId(), orderEntity.getCustomerContact());
    }

    public void cancelOrder(String id){
        orderRepository.deleteById(id);
    }
}
