package com.InventoryManagement.Notification.DTO;

import java.util.List;

public record OrderRequest(String id, List<String> productId, List<String> quantity, String shippingDetails, boolean payment, String customerId, String customerContact) {
}
