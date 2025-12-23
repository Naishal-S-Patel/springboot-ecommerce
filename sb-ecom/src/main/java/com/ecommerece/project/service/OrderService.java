package com.ecommerece.project.service;

import com.ecommerece.project.payload.OrderDTO;
import com.ecommerece.project.payload.OrderResponse;
import jakarta.transaction.Transactional;

public interface OrderService {
    @Transactional
    OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage);

    OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);

    OrderDTO updateOrder( Long orderId, String status);

    OrderResponse getSellerAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder);
}
