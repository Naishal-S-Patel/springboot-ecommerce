package com.ecommerece.project.service;

import com.ecommerece.project.exceptions.APIException;
import com.ecommerece.project.exceptions.ResourceNotFoundException;
import com.ecommerece.project.model.*;
import com.ecommerece.project.payload.OrderDTO;
import com.ecommerece.project.payload.OrderItemDTO;
import com.ecommerece.project.payload.OrderResponse;
import com.ecommerece.project.repositories.*;
import com.ecommerece.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {
    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AuthUtil authUtil;

    @Override
    @Transactional
    public OrderDTO placeOrder(String emailId, Long addressId, String paymentMethod, String pgName, String pgPaymentId, String pgStatus, String pgResponseMessage) {

//        getting user cart
        Cart cart=cartRepository.findCartByEmail(emailId);
        if(cart==null){
            throw new ResourceNotFoundException("cart","email",emailId);
        }
        Address address=addressRepository.findById(addressId)
                .orElseThrow(()->new ResourceNotFoundException("address","addressId",addressId));

//        create a new order with payment info
        Order order=new Order();
        order.setEmail(emailId);
        order.setOrderDate(LocalDate.now());
        order.setTotalAmount(cart.getTotalPrice());
        order.setOrderStatus("Order accepted");
        order.setAddress(address);

        Payment payment=new Payment(paymentMethod,pgPaymentId,pgStatus,pgResponseMessage,pgName);
        payment.setOrder(order);
        payment=paymentRepository.save(payment);
        order.setPayment(payment);
        Order savedOrder=orderRepository.save(order);

//        get items from the cart into the order items
        List<CartItem> cartItems=cart.getCartItems();
        if(cartItems.isEmpty()){
            throw new APIException("cart is empty");
        }
        List<OrderItem> orderItems=new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem=new OrderItem();
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setDiscount(cartItem.getDiscount());
            orderItem.setOrderedProductPrice(cartItem.getProductPrice());
            orderItem.setOrder(savedOrder);
            orderItems.add(orderItem);
        }
       orderItems= orderItemRepository.saveAll(orderItems);

//        post order tasks

//        update product stock
        cart.getCartItems().forEach(item->{
            int quantity=item.getQuantity();
            Product product=item.getProduct();
            product.setQuantity(product.getQuantity()-quantity);
            productRepository.save(product);

            //        clear the cart
            cartService.deleteProductFromCart(cart.getCartId(),item.getProduct().getProductId());
        });
//        send back the order summary
        OrderDTO orderDTO=modelMapper.map(savedOrder,OrderDTO.class);
        orderItems.forEach(item->orderDTO.getOrderItems().
                add(modelMapper.map(item, OrderItemDTO.class)));
        orderDTO.setAddressId(addressId);

        return orderDTO;
    }

    @Override
    public OrderResponse getAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending():
                Sort.by(sortBy).descending();
        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);
        Page<Order> orderPage=orderRepository.findAll(pageDetails);
        List<Order> orders=orderPage.getContent();

        List<OrderDTO> orderDTOS=orders.stream()
                .map(order->modelMapper.map(order,OrderDTO.class))
                .toList();
        OrderResponse orderResponse=new OrderResponse();
        orderResponse.setContent(orderDTOS);
        orderResponse.setPageNumber(orderPage.getNumber());
        orderResponse.setPageSize(orderPage.getSize());
        orderResponse.setTotalElements(orderPage.getTotalElements());
        orderResponse.setTotalPages(orderPage.getTotalPages());
        orderResponse.setLastPage(orderPage.isLast());
        return orderResponse;
    }

    @Override
    public OrderDTO updateOrder( Long orderId, String status) {
        Order order=orderRepository.findById(orderId).
                orElseThrow(()->new ResourceNotFoundException("order","orderId",orderId));
        order.setOrderStatus(status);
        order=orderRepository.save(order);
      OrderDTO orderDTO= modelMapper.map(order,OrderDTO.class);
        return orderDTO;
    }

    @Override
    public OrderResponse getSellerAllOrders(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Sort sortByAndOrder=sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending():
                Sort.by(sortBy).descending();
        Pageable pageDetails= PageRequest.of(pageNumber,pageSize,sortByAndOrder);

        User seller=authUtil.loggedInUser();

        Page<Order> orderPage=orderRepository.findAll(pageDetails);

        List<Order> sellerOrders = orderPage.getContent().stream()
                .filter(order -> order.getOrderItems().stream()
                        .anyMatch(orderItem -> {
                            var product = orderItem.getProduct();
                            if (product == null || product.getUser() == null) {
                                return false;
                            }
                            return product.getUser().getUserId().equals(
                                    seller.getUserId());
                        }))
                .toList();


        List<OrderDTO> orderDTOS=sellerOrders.stream()
                .map(order->modelMapper.map(order,OrderDTO.class))
                .toList();
        OrderResponse orderResponse=new OrderResponse();
        orderResponse.setContent(orderDTOS);
        orderResponse.setPageNumber(orderPage.getNumber());
        orderResponse.setPageSize(orderPage.getSize());
        orderResponse.setTotalElements(orderPage.getTotalElements());
        orderResponse.setTotalPages(orderPage.getTotalPages());
        orderResponse.setLastPage(orderPage.isLast());
        return orderResponse;
    }
}
