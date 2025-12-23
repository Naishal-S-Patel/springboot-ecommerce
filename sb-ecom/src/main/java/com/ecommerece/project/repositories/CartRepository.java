package com.ecommerece.project.repositories;

import com.ecommerece.project.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CartRepository extends JpaRepository<Cart,Long> {
    @Query("Select c from Cart c WHERE c.user.email=?1")
    Cart findCartByEmail(String email);

    @Query("Select c from Cart c WHERE c.user.email=?1 AND c.cartId=?2")
    Cart findCartByEmailAndCartId(String emailId, Long cartId);

    @Query("SELECT c from Cart c JOIN FETCH c.cartItems ci JOIN FETCH ci.product p WHERE p.productId=?1")
    List<Cart> findCartByProductId(Long productId);

}
