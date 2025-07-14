package com.acc.service;
import com.acc.dto.AddItemToCartRequestDTO;
import com.acc.dto.CartDTO;
import java.math.BigDecimal;
import java.util.List;
public interface CartService {
    CartDTO getOrCreateCart(Long customerId); 
    CartDTO addProductToCart(Long customerId, AddItemToCartRequestDTO addItemToCartDTO);
    CartDTO updateProductQuantityInCart(Long customerId, Long productId, Integer newQuantity); 
    CartDTO removeProductFromCart(Long customerId, Long productId); 
    void clearCart(Long customerId);
    CartDTO getCartById(Long cartId);
	List<CartDTO> getAllCarts();
}