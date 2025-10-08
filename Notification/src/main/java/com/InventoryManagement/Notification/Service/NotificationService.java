package com.InventoryManagement.Notification.Service;

import com.InventoryManagement.Notification.DTO.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final JavaMailSender javaMailSender;
    @KafkaListener(topics = "order-topic",groupId = "notification-service")
    public void consume(OrderRequest orderRequest){
        log.info("Received order for notification: {}", orderRequest);
        String subject = "Order Confirmation - " + orderRequest.id();
        String body = "Hello,\n\nYour order has been placed successfully!\n\n"
                + "Order Details:\n"
                + "Order ID: " + orderRequest.id() + "\n"
                + "Products: " + String.join(", ", orderRequest.productId()) + "\n"
                + "Quantities: " + String.join(", ", orderRequest.quantity()) + "\n"
                + "Shipping Address: " + orderRequest.shippingDetails() + "\n"
                + "Payment Status: " + (orderRequest.payment() ? "Paid" : "Pending") + "\n\n"
                + "We will notify you once your order is shipped.\n\n"
                + "Thank you for shopping with us!";

        sendSimpleMail(orderRequest.customerContact(), subject, body);
    }
    public void sendSimpleMail(String to,String subject,String body){
        SimpleMailMessage simpleMailMessage = new SimpleMailMessage();
        simpleMailMessage.setFrom("krishnendubasak065@gmail.com");
        simpleMailMessage.setTo(to);
        simpleMailMessage.setText(body);
        simpleMailMessage.setSubject(subject);
        javaMailSender.send(simpleMailMessage);
    }
}
