package com.acc.serviceImpl;

import com.acc.dto.AddItemToCartRequestDTO;
import com.acc.dto.CartDTO;
import com.acc.dto.CartItemDTO;
import com.acc.entity.Cart;
import com.acc.entity.CartItem;
import com.acc.entity.Customer;
import com.acc.entity.Product;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.CartItemRepository;
import com.acc.repository.CartRepository;
import com.acc.repository.CustomerRepository;
import com.acc.repository.ProductRepository;
import com.acc.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ProductRepository productRepository;

    private Cart getOrCreateCartEntity(Long customerId) {
        return cartRepository.findByCustomer_Id(customerId)
            .orElseGet(() -> {
                Customer customer = customerRepository.findById(customerId)
                        .orElseThrow(() -> new ResourceNotFoundException("Customer", "Id", customerId));
                Cart cart = new Cart();
                cart.setCustomer(customer);
                cart.setCreatedAt(LocalDateTime.now());
                cart.setUpdatedAt(LocalDateTime.now());
                cart.setTotalAmount(BigDecimal.ZERO);
                cart.setCartItems(new HashSet<>());
                return cartRepository.save(cart);
            });
    }

    @Override
    @Transactional
    public CartDTO getOrCreateCart(Long customerId) {
        return convertToDTO(getOrCreateCartEntity(customerId));
    }

    @Override
    @Transactional
    public CartDTO addProductToCart(Long customerId, AddItemToCartRequestDTO addItemToCartDTO) {
        if (addItemToCartDTO.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(addItemToCartDTO.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", addItemToCartDTO.getProductId()));

        Optional<CartItem> existingCartItem = cartItemRepository.findByCartAndProduct(cart, product);
        CartItem cartItem;
        int quantityToAdd = addItemToCartDTO.getQuantity();

        if (existingCartItem.isPresent()) {
            cartItem = existingCartItem.get();
            int newTotalQuantity = cartItem.getQuantity() + quantityToAdd;
            if (product.getStockQuantity() < newTotalQuantity) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
            }
            cartItem.setQuantity(newTotalQuantity);
        } else {
            if (product.getStockQuantity() < quantityToAdd) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
            }
            cartItem = new CartItem();
            cartItem.setProduct(product);
            cartItem.setQuantity(quantityToAdd);
            cartItem.setPrice(product.getPrice());
            cartItem.setCart(cart);
            cart.addCartItem(cartItem);
        }

        cartItemRepository.save(cartItem);
        product.setStockQuantity(product.getStockQuantity() - quantityToAdd);
        productRepository.save(product);
        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long customerId, Long productId, Integer newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        if (newQuantity == 0) {
            return removeProductFromCart(customerId, productId);
        }

        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", productId));
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "Product Not Found in Cart", productId));

        int oldQuantity = cartItem.getQuantity();
        int diff = newQuantity - oldQuantity;

        if (diff > 0) {
            if (product.getStockQuantity() < diff) {
                throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
            }
            product.setStockQuantity(product.getStockQuantity() - diff);
        } else if (diff < 0) {
            product.setStockQuantity(product.getStockQuantity() + Math.abs(diff));
        }

        productRepository.save(product);
        cartItem.setQuantity(newQuantity);
        cartItem.setPrice(product.getPrice());
        cartItemRepository.save(cartItem);
        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO removeProductFromCart(Long customerId, Long productId) {
        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", productId));
        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "Product Not Found in Cart", productId));

        product.setStockQuantity(product.getStockQuantity() + cartItem.getQuantity());
        productRepository.save(product);
        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);
        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long customerId) {
        Cart cart = getOrCreateCartEntity(customerId);
        List<CartItem> itemsToClear = new ArrayList<>(cart.getCartItems());

        for (CartItem item : itemsToClear) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
            cart.removeCartItem(item);
            cartItemRepository.delete(item);
        }

        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartById(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "Id", cartId));
        return convertToDTO(cart);
    }

    @Override
    public List<CartDTO> getAllCarts() {
        return cartRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private void updateCartTotal(Cart cart) {
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getCartItems()) {
            total = total.add(item.getItemTotal());
        }
        cart.setTotalAmount(total);
    }

    private CartDTO convertToDTO(Cart cart) {
    CartDTO cartDTO = new CartDTO();
    cartDTO.setId(cart.getId());
    cartDTO.setCustomerId(cart.getCustomer().getId());
    cartDTO.setCreatedAt(cart.getCreatedAt());
    cartDTO.setUpdatedAt(cart.getUpdatedAt());
    cartDTO.setTotalAmount(cart.getTotalAmount());

    cartDTO.setCartItems(
        cart.getCartItems()
            .stream()
            .filter(item -> item.getProduct() != null && item.getProduct().isActive()) 
            .map(this::convertCartItemToDTO)
            .collect(Collectors.toList())
    );

    return cartDTO;
}


   
    private CartItemDTO convertCartItemToDTO(CartItem cartItem) {
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName()); 
        dto.setQuantity(cartItem.getQuantity());
        dto.setPrice(cartItem.getPrice());
        return dto;
    }
}
