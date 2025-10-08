package com.InventoryManagement.Order.DTO;

import java.util.List;

public record OrderResponse(String id, List<String> productId, List<String> quantity, String shippingDetails, boolean payment, String customerId, String customerContact) {
}
