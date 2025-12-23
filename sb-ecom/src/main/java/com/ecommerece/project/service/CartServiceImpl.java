package com.ecommerece.project.service;

import com.ecommerece.project.exceptions.APIException;
import com.ecommerece.project.exceptions.ResourceNotFoundException;
import com.ecommerece.project.model.Cart;
import com.ecommerece.project.model.CartItem;
import com.ecommerece.project.model.Product;
import com.ecommerece.project.payload.CartDTO;
import com.ecommerece.project.payload.CartItemDTO;
import com.ecommerece.project.payload.ProductDTO;
import com.ecommerece.project.repositories.CartItemRepository;
import com.ecommerece.project.repositories.CartRepository;
import com.ecommerece.project.repositories.ProductRepository;
import com.ecommerece.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService{

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {

//        find existing cart associated to user or create one
            Cart userCart=createCart();
//        retrieve Product details
        Product product=productRepository.findById(productId).
                orElseThrow(()->new ResourceNotFoundException("Product","productId",productId));
//        perform validations
        // 1) check if item already exists in the cart

          CartItem cartItem=  cartItemRepository.findCartItemByProductIdAndCartId(
                userCart.getCartId(),productId
        );
          if(product.getQuantity()==0){
              throw new APIException(product.getProductName()+" is not available");
          }
          if(product.getQuantity()<quantity){
              throw new APIException("Please, make an order of the "+product.getProductName()+
                      " less than or equal to "+product.getQuantity()+".");
          }

          // If product already exists in cart, update quantity instead of throwing error
          if(cartItem!=null){
              cartItem.setQuantity(cartItem.getQuantity() + quantity);
              cartItem.setProductPrice(product.getSpecialPrice());
              cartItem.setDiscount(product.getDiscount());
              cartItemRepository.save(cartItem);
              userCart.setTotalPrice(userCart.getTotalPrice()+(product.getSpecialPrice()*quantity));
              Cart savedCart= cartRepository.save(userCart);
              CartDTO cartDTO=modelMapper.map(savedCart,CartDTO.class);
              List<CartItem> cartItems=userCart.getCartItems();
              Stream<ProductDTO> productStream=cartItems.stream().map(
                      item->{ProductDTO map=modelMapper.map(item.getProduct(),ProductDTO.class);
                      map.setQuantity(item.getQuantity());
                      return map;
                      });
              cartDTO.setProducts(productStream.toList());
              return cartDTO;
          }

//        create Cart Item
        CartItem newCartItem=new CartItem();
          newCartItem.setProduct(product);
          newCartItem.setCart(userCart);
          newCartItem.setQuantity(quantity);
          newCartItem.setDiscount(product.getDiscount());
          newCartItem.setProductPrice(product.getSpecialPrice());

//        save cart Item
        cartItemRepository.save(newCartItem);

        userCart.getCartItems().add(newCartItem);

//        product.setQuantity(product.getQuantity()-quantity);   upto ur logic
        product.setQuantity(product.getQuantity());
        userCart.setTotalPrice(userCart.getTotalPrice()+(product.getSpecialPrice()*quantity));
           Cart savedCart= cartRepository.save(userCart);

//        return updated cart
        CartDTO cartDTO=modelMapper.map(savedCart,CartDTO.class);

        List<CartItem> cartItems=userCart.getCartItems();
        Stream<ProductDTO> productStream=cartItems.stream().map(
                item->{ProductDTO map=modelMapper.map(item.getProduct(),ProductDTO.class);
                map.setQuantity(item.getQuantity());
                return map;
                });
        cartDTO.setProducts(productStream.toList());
        return cartDTO;
    }

    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.size() == 0) {
            throw new APIException("No cart exists");
        }

        List<CartDTO> cartDTOs = carts.stream().map(cart -> {
            CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

            List<ProductDTO> products = cart.getCartItems().stream().map(cartItem -> {
                ProductDTO productDTO = modelMapper.map(cartItem.getProduct(), ProductDTO.class);
                productDTO.setQuantity(cartItem.getQuantity()); // Set the quantity from CartItem
                return productDTO;
            }).collect(Collectors.toList());


            cartDTO.setProducts(products);

            return cartDTO;

        }).collect(Collectors.toList());

        return cartDTOs;

    }

    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart=cartRepository.findCartByEmailAndCartId(emailId,cartId);
        if(cart==null){
            throw new ResourceNotFoundException("Cart","cartId",cartId);
        }
        CartDTO cartDTO=modelMapper.map(cart,CartDTO.class);
        cart.getCartItems().forEach(c->c.getProduct().setQuantity(c.getQuantity()));
        List<ProductDTO> products=cart.getCartItems().stream()
                .map(p->modelMapper.map(p.getProduct(),ProductDTO.class)).toList();
        cartDTO.setProducts(products);
        return cartDTO;
    }
    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {

        String emailId = authUtil.loggedInEmail();
        Cart userCart = cartRepository.findCartByEmail(emailId);
        Long cartId  = userCart.getCartId();

        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        // Calculate new quantity
        int newQuantity = cartItem.getQuantity() + quantity;

        // Validation to prevent negative quantities
        if (newQuantity < 0) {
            throw new APIException("The resulting quantity cannot be negative.");
        }

        if (newQuantity == 0){
            deleteProductFromCart(cartId, productId);
        } else {
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setQuantity(cartItem.getQuantity() + quantity);
            cartItem.setDiscount(product.getDiscount());
            cart.setTotalPrice(cart.getTotalPrice() + (cartItem.getProductPrice() * quantity));
            cartRepository.save(cart);
        }

        CartItem updatedItem = cartItemRepository.save(cartItem);
        if(updatedItem.getQuantity() == 0){
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }


        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO prd = modelMapper.map(item.getProduct(), ProductDTO.class);
            prd.setQuantity(item.getQuantity());
            return prd;
        });


        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        cart.setTotalPrice(cart.getTotalPrice() -
                (cartItem.getProductPrice() * cartItem.getQuantity()));
        if (cart.getTotalPrice() < 0) {
           throw new APIException("Total price cannot be negative.");
      }

        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";
    }

    @Override
    public void updateProductInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        double cartPrice=cart.getTotalPrice()-(cartItem.getProductPrice() * cartItem.getQuantity());
        cartItem.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(cartPrice+(cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItem=cartItemRepository.save(cartItem);
    }

    @Override
    @Transactional
    public String createOrUpdateItemsInCart(List<CartItemDTO> cartItemDTOS) {
        String email=authUtil.loggedInEmail();
        Cart existingCart=cartRepository.findCartByEmail(email);
        if (existingCart == null) {
            existingCart = new Cart();
            existingCart.setTotalPrice(0.00);
            existingCart.setUser(authUtil.loggedInUser());
            existingCart=cartRepository.save(existingCart);
        }
        else{
            cartItemRepository.deleteAllByCartId(existingCart.getCartId());
        }
        double totalPrice=0.00;
        for(CartItemDTO cartItemDTO:cartItemDTOS){
            Long productId=cartItemDTO.getProductId();
            Integer quantity=cartItemDTO.getQuantity();
            Product product=productRepository.findById(productId).
                    orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
            product.setQuantity(product.getQuantity()-quantity);
            totalPrice+=product.getSpecialPrice()*quantity;
            CartItem cartItem=new CartItem();
            cartItem.setProduct(product);
            cartItem.setCart(existingCart);
            cartItem.setQuantity(quantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }
        existingCart.setTotalPrice(totalPrice);
        cartRepository.save(existingCart);
        return "Cart created/updated with the new items successfully!!!";
    }

    public Cart createCart(){
        Cart userCart=cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if(userCart!=null){
            return userCart;
        }
        Cart cart=new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());
        Cart newCart=cartRepository.save(cart);
        return newCart;
    }
}
