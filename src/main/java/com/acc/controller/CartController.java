package com.acc.controller;

import com.acc.dto.AddItemToCartRequestDTO;
import com.acc.dto.CartDTO;
import com.acc.service.CartService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<CartDTO> getOrCreateCart(@PathVariable Long customerId) {
        CartDTO cartDTO = cartService.getOrCreateCart(customerId);
        return ResponseEntity.ok(cartDTO);
    }

   
    @PostMapping("/{customerId}/items")
    public ResponseEntity<CartDTO> addProductToCart(
            @PathVariable Long customerId,
            @Validated @RequestBody AddItemToCartRequestDTO addItemToCartDTO) {
        CartDTO updatedCart = cartService.addProductToCart(customerId, addItemToCartDTO);
        return ResponseEntity.ok(updatedCart); 
    }

    
    @PutMapping("/customer/{customerId}/items/{productId}")
    public ResponseEntity<CartDTO> updateProductQuantityInCart(
            @PathVariable Long customerId,
            @PathVariable Long productId,
            @RequestParam Integer newQuantity) {
        CartDTO updatedCart = cartService.updateProductQuantityInCart(customerId, productId, newQuantity);
        return ResponseEntity.ok(updatedCart);
    }

    
    @DeleteMapping("/{customerId}/items/{productId}")
    public ResponseEntity<CartDTO> removeProductFromCart(
            @PathVariable Long customerId,
            @PathVariable Long productId) {
        CartDTO updatedCart = cartService.removeProductFromCart(customerId, productId);
        return ResponseEntity.ok(updatedCart);
    }

    
    @DeleteMapping("/{customerId}/clear")
    public ResponseEntity<Void> clearCart(@PathVariable Long customerId) {
        cartService.clearCart(customerId);
        return ResponseEntity.noContent().build(); 
    }

    
    @GetMapping("/{cartId}")
    public ResponseEntity<CartDTO> getCartById(@PathVariable Long cartId) {
        CartDTO cartDTO = cartService.getCartById(cartId);
        return ResponseEntity.ok(cartDTO);
    }

    
    @GetMapping
    public ResponseEntity<List<CartDTO>> getAllCarts() {
        List<CartDTO> carts = cartService.getAllCarts();
        return ResponseEntity.ok(carts);
    }
}
