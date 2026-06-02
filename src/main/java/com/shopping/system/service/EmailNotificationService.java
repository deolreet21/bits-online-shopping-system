package com.shopping.system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    @Autowired
    private JavaMailSender mailSender;

    @Async
    public void sendOrderPlaced(String email, String username, Long orderId, String total) {
        send(email,
                "Order Placed – #" + orderId,
                "Hi " + username + ",\n\n" +
                "Your order #" + orderId + " has been placed successfully.\n" +
                "Total: Rs. " + total + "\n\n" +
                "We'll notify you when it's confirmed and shipped.\n\n" +
                "Thank you for shopping with e-Kiosk!");
    }

    @Async
    public void sendOrderConfirmed(String email, String username, Long orderId) {
        send(email,
                "Order Confirmed – #" + orderId,
                "Hi " + username + ",\n\n" +
                "Your order #" + orderId + " has been confirmed and is being prepared.\n\n" +
                "Thank you for shopping with e-Kiosk!");
    }

    @Async
    public void sendOrderShipped(String email, String username, Long orderId, String address) {
        send(email,
                "Order Shipped – #" + orderId,
                "Hi " + username + ",\n\n" +
                "Great news! Your order #" + orderId + " has been shipped.\n" +
                "Shipping address: " + address + "\n\n" +
                "Thank you for shopping with e-Kiosk!");
    }

    @Async
    public void sendOrderDelivered(String email, String username, Long orderId) {
        send(email,
                "Order Delivered – #" + orderId,
                "Hi " + username + ",\n\n" +
                "Your order #" + orderId + " has been delivered. We hope you enjoy your purchase!\n\n" +
                "Thank you for shopping with e-Kiosk!");
    }

    @Async
    public void sendOrderCancelled(String email, String username, Long orderId) {
        send(email,
                "Order Cancelled – #" + orderId,
                "Hi " + username + ",\n\n" +
                "Your order #" + orderId + " has been cancelled.\n" +
                "If you did not request this, please contact support.\n\n" +
                "Thank you for shopping with e-Kiosk!");
    }

    private void send(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            System.out.println("[EMAIL] Sent '" + subject + "' to " + to);
        } catch (Exception e) {
            System.err.println("[EMAIL ERROR] Failed to send '" + subject + "' to " + to + ": " + e.getMessage());
        }
    }
}
