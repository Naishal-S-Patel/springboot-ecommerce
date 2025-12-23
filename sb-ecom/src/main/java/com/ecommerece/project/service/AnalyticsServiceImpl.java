package com.ecommerece.project.service;

import com.ecommerece.project.payload.AnalyticsResponse;
import com.ecommerece.project.repositories.OrderRepository;
import com.ecommerece.project.repositories.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsServiceImpl implements  AnalyticsService {

    @Autowired
    private ProductRepository productRepository;


    @Autowired
    private OrderRepository orderRepository;

    @Override
    public AnalyticsResponse getAnalyticsData() {
        AnalyticsResponse analyticsResponse = new AnalyticsResponse();
        long productCount = productRepository.count();
        long totalOrders = orderRepository.count();
        Double orderRevenue = orderRepository.getTotalRevenue();
        analyticsResponse.setProductCount(String.valueOf(productCount));
        analyticsResponse.setTotalOrders(String.valueOf(totalOrders));
        analyticsResponse.setOrderRevenue(String.valueOf(orderRevenue!=null ? orderRevenue : 0));
        return analyticsResponse;
    }
}
