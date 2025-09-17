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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private CartItemRepository cartItemRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private ProductRepository productRepository;

    private Cart getOrCreateCartEntity(Long customerId) {
        log.debug("Attempting to find or create a cart for customer ID: {}", customerId);
        return cartRepository.findByCustomer_Id(customerId)
                .orElseGet(() -> {
                    log.info("No cart found for customer ID: {}. Creating a new one.", customerId);
                    Customer customer = customerRepository.findById(customerId)
                            .orElseThrow(() -> {
                                log.error("Customer not found with ID: {}", customerId);
                                return new ResourceNotFoundException("Customer", "Id", customerId);
                            });
                    Cart cart = new Cart();
                    cart.setCustomer(customer);
                    cart.setCreatedAt(LocalDateTime.now());
                    cart.setUpdatedAt(LocalDateTime.now());
                    cart.setTotalAmount(BigDecimal.ZERO);
                    cart.setCartItems(new ArrayList<>());
                    Cart savedCart = cartRepository.save(cart);
                    log.info("New cart created with ID: {} for customer ID: {}", savedCart.getId(), customerId);
                    return savedCart;
                });
    }

    @Override
    @Transactional
    public CartDTO getOrCreateCart(Long customerId) {
        log.info("Fetching or creating cart for customer ID: {}", customerId);
        return convertToDTO(getOrCreateCartEntity(customerId));
    }

    @Override
    @Transactional
    public CartDTO addProductToCart(Long customerId, AddItemToCartRequestDTO addItemToCartDTO) {
        log.info("Adding product ID: {} with quantity: {} to cart for customer ID: {}",
                addItemToCartDTO.getProductId(), addItemToCartDTO.getQuantity(), customerId);

        if (addItemToCartDTO.getQuantity() <= 0) {
            log.error("Invalid quantity provided: {}", addItemToCartDTO.getQuantity());
            throw new IllegalArgumentException("Quantity must be greater than zero.");
        }

        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(addItemToCartDTO.getProductId())
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", addItemToCartDTO.getProductId());
                    return new ResourceNotFoundException("Product", "Id", addItemToCartDTO.getProductId());
                });

        if (!product.isActive()) {
            log.error("Attempted to add inactive product ID: {} to cart.", product.getId());
            throw new IllegalArgumentException("Product is not available for purchase.");
        }

        Optional<CartItem> existingCartItem = cartItemRepository.findByCartAndProduct(cart, product);
        int quantityToAdd = addItemToCartDTO.getQuantity();
        CartItem cartItem;
        int newQuantity = quantityToAdd;

        if (existingCartItem.isPresent()) {
            cartItem = existingCartItem.get();
            newQuantity = cartItem.getQuantity() + quantityToAdd;
            log.debug("Product ID {} already in cart. Old quantity: {}, New quantity: {}",
                    product.getId(), cartItem.getQuantity(), newQuantity);
        } else {
            cartItem = new CartItem();
            cartItem.setCart(cart);
            cartItem.setProduct(product);
            cartItem.setPrice(product.getPrice());
            cart.addCartItem(cartItem);
            log.debug("Adding new product ID {} to cart.", product.getId());
        }

        if (product.getStockQuantity() < quantityToAdd) {
            log.error("Insufficient stock for product: {}. Available: {}, Requested: {}",
                    product.getName(), product.getStockQuantity(), quantityToAdd);
            throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
        }

        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);
        log.debug("Saved cart item with ID: {}", cartItem.getId());

        // Update product stock (decrement)
        product.setStockQuantity(product.getStockQuantity() - quantityToAdd);
        productRepository.save(product);
        log.debug("Updated stock for product ID {}. New quantity: {}", product.getId(), product.getStockQuantity());

        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Product ID: {} added to cart successfully. Cart ID: {}", product.getId(), cart.getId());
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO updateProductQuantityInCart(Long customerId, Long productId, Integer newQuantity) {
        log.info("Updating quantity for product ID: {} to {} in cart for customer ID: {}", productId, newQuantity, customerId);

        if (newQuantity < 0) {
            log.error("Invalid new quantity provided: {}", newQuantity);
            throw new IllegalArgumentException("Quantity cannot be negative.");
        }

        if (newQuantity == 0) {
            log.info("New quantity is 0, removing product ID: {} from cart.", productId);
            return removeProductFromCart(customerId, productId);
        }

        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product", "Id", productId);
                });

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> {
                    log.error("Cart item not found for product ID: {} in cart ID: {}", productId, cart.getId());
                    return new ResourceNotFoundException("CartItem", "Product Not Found in Cart", productId);
                });
        
        // This is the correct logic. The stock is the source of truth,
        // and cart item quantity should not exceed it.
        if (newQuantity > product.getStockQuantity()) {
            log.error("Requested quantity ({}) exceeds available stock ({}) for product: {}",
                    newQuantity, product.getStockQuantity(), product.getName());
            throw new IllegalArgumentException("Not enough stock for product: " + product.getName());
        }
        
        // Update the quantity in the cart item
        cartItem.setQuantity(newQuantity);
        cartItemRepository.save(cartItem);

        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Product quantity updated successfully for product ID: {} in cart ID: {}", productId, cart.getId());
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public CartDTO removeProductFromCart(Long customerId, Long productId) {
        log.info("Removing product ID: {} from cart for customer ID: {}", productId, customerId);
        Cart cart = getOrCreateCartEntity(customerId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", productId);
                    return new ResourceNotFoundException("Product", "Id", productId);
                });

        CartItem cartItem = cartItemRepository.findByCartAndProduct(cart, product)
                .orElseThrow(() -> {
                    log.error("Cart item not found for product ID: {} in cart ID: {}", productId, cart.getId());
                    return new ResourceNotFoundException("CartItem", "Product Not Found in Cart", productId);
                });

        product.setStockQuantity(product.getStockQuantity() + cartItem.getQuantity());
        productRepository.save(product);
        log.info("Restored {} units to stock for product ID: {} after removing from cart.", cartItem.getQuantity(), product.getId());

        cart.removeCartItem(cartItem);
        cartItemRepository.delete(cartItem);

        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);

        log.info("Product ID: {} removed from cart ID: {} successfully.", productId, cart.getId());
        return convertToDTO(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long customerId) {
        log.info("Clearing cart for customer ID: {}", customerId);
        Cart cart = getOrCreateCartEntity(customerId);
        List<CartItem> itemsToClear = new ArrayList<>(cart.getCartItems());
        log.debug("Found {} items to clear from cart ID: {}", itemsToClear.size(), cart.getId());

        for (CartItem item : itemsToClear) {
            Product product = item.getProduct();
            product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            productRepository.save(product);
            log.debug("Restored {} units to stock for product ID: {}", item.getQuantity(), product.getId());
            cart.removeCartItem(item);
            cartItemRepository.delete(item);
        }

        updateCartTotal(cart);
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        log.info("Cart ID: {} cleared successfully. All items removed and stock restored.", cart.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public CartDTO getCartById(Long cartId) {
        log.info("Fetching cart with ID: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> {
                    log.error("Cart not found with ID: {}", cartId);
                    return new ResourceNotFoundException("Cart", "Id", cartId);
                });
        return convertToDTO(cart);
    }

    @Override
    public List<CartDTO> getAllCarts() {
        log.info("Fetching all carts.");
        List<CartDTO> carts = cartRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} carts in total.", carts.size());
        return carts;
    }

    private void updateCartTotal(Cart cart) {
        log.debug("Recalculating total amount for cart ID: {}", cart.getId());
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cart.getCartItems()) {
            Double discountPercent = item.getProduct().getDiscountPercentage();
            BigDecimal price = item.getPrice();
            BigDecimal finalPrice = price;

            if (discountPercent != null && discountPercent > 0.0) {
                BigDecimal discountAmount = price.multiply(BigDecimal.valueOf(discountPercent))
                        .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
                finalPrice = price.subtract(discountAmount);
                log.trace("Applying {}% discount to product ID {}. Original price: {}, Discounted price: {}",
                        discountPercent, item.getProduct().getId(), price, finalPrice);
            }

            total = total.add(finalPrice.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        cart.setTotalAmount(total);
        log.debug("New total amount for cart ID {} is: {}", cart.getId(), total);
    }

    private CartDTO convertToDTO(Cart cart) {
        log.debug("Converting Cart entity to DTO for ID: {}", cart.getId());
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

        log.debug("Converted cart ID {} to DTO with {} items.", cart.getId(), cartDTO.getCartItems().size());
        return cartDTO;
    }

    private CartItemDTO convertCartItemToDTO(CartItem cartItem) {
        log.trace("Converting CartItem entity to DTO for ID: {}", cartItem.getId());
        CartItemDTO dto = new CartItemDTO();
        dto.setId(cartItem.getId());
        dto.setProductId(cartItem.getProduct().getId());
        dto.setProductName(cartItem.getProduct().getName());
        dto.setQuantity(cartItem.getQuantity());
        dto.setPrice(cartItem.getPrice());

        Double discountPercent = cartItem.getProduct().getDiscountPercentage();
        dto.setDiscountPercentage(discountPercent);

        if (discountPercent != null && discountPercent > 0.0) {
            BigDecimal discountAmount = cartItem.getPrice()
                    .multiply(BigDecimal.valueOf(discountPercent))
                    .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
            dto.setDiscountedPrice(cartItem.getPrice().subtract(discountAmount));
        } else {
            dto.setDiscountedPrice(cartItem.getPrice());
        }

        return dto;
    }
}