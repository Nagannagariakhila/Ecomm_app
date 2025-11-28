
package com.acc.serviceImpl;
import com.acc.dto.OrderDTO;
import com.acc.dto.OrderItemDTO;
import com.acc.dto.ProductDTO;
import com.acc.entity.*;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.*;
import com.acc.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter; // Import the Date formatter
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired private PaymentRepository paymentRepository;

    @Override
    @Transactional
    public OrderDTO createOrderFromCart(Long cartId) {
        log.info("Attempting to create an order from cart with ID: {}", cartId);
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> {
                    log.error("Cart not found with ID: {}", cartId);
                    return new ResourceNotFoundException("Cart", "Id", cartId);
                });

        if (cart.getCartItems().isEmpty()) {
            log.warn("Cannot create an order from an empty cart for cart ID: {}", cartId);
            throw new IllegalArgumentException("Cannot create an order from an empty cart.");
        }

        Order order = new Order();
        order.setCustomer(cart.getCustomer());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        BigDecimal totalDiscountedAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<CartItem> itemsToBeRemoved = new ArrayList<>();

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            if (product == null) {
                log.warn("Skipping cart item with no associated product in cart ID: {}", cartId);
                continue;
            }

            if (product.getStockQuantity() >= cartItem.getQuantity()) {
                log.debug("Processing product '{}' (ID: {}) with quantity {} from cart.",
                        product.getName(), product.getId(), cartItem.getQuantity());

                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(product.getPrice());
                
                Double discountPercentage = product.getDiscountPercentage() != null ? product.getDiscountPercentage() : 0.0;
                orderItem.setDiscountPercentage(discountPercentage);
                
                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()));
                BigDecimal itemDiscountedPrice = itemTotal.subtract(itemTotal.multiply(BigDecimal.valueOf(discountPercentage)).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP));
                orderItem.setDiscountedPrice(itemDiscountedPrice);

                orderItem.setOrder(order);
                orderItems.add(orderItem);

                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);
                log.debug("Updated stock for product ID {}. New quantity: {}", product.getId(), product.getStockQuantity());

                totalOrderAmount = totalOrderAmount.add(itemTotal);
                totalDiscountedAmount = totalDiscountedAmount.add(itemDiscountedPrice);

                itemsToBeRemoved.add(cartItem);
            } else {
                log.warn("Insufficient stock for product '{}' (ID: {}). Available: {}, Requested: {}. Skipping.",
                        product.getName(), product.getId(), product.getStockQuantity(), cartItem.getQuantity());
            }
        }

        if (orderItems.isEmpty()) {
            log.error("No available products in cart {} to place an order.", cartId);
            throw new IllegalArgumentException("No available products in cart to place an order.");
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalOrderAmount);
        order.setDiscountAmount(totalOrderAmount.subtract(totalDiscountedAmount));
        order.setDiscountedAmount(totalDiscountedAmount);

        Order savedOrder = orderRepository.save(order);
        
        
        generateAndSetOrderCode(savedOrder);
        
        log.info("Order created successfully with ID: {} from cart ID: {}", savedOrder.getId(), cartId);

        cart.getCartItems().removeAll(itemsToBeRemoved);
        cart.setTotalAmount(cart.getCartItems().stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        cartRepository.save(cart);
        log.info("Removed {} items from cart ID: {}. Remaining cart total: {}", itemsToBeRemoved.size(), cartId, cart.getTotalAmount());

        return convertToDTO(savedOrder);
    }

    @Override
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) {
        log.info("Fetching all orders for customer with ID: {}", customerId);
        List<OrderDTO> orders = orderRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} orders for customer ID: {}", orders.size(), customerId);
        return orders;
    }

    @Override
    @Transactional
    public OrderDTO savePartialOrder(Long customerId, OrderDTO orderDTO) {
        log.info("Attempting to save partial order for customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", customerId);
                    return new ResourceNotFoundException("Customer", "Id", customerId);
                });
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> {
                    log.error("Cart not found for customer ID: {}", customerId);
                    return new ResourceNotFoundException("Cart", "CustomerId", customerId);
                });

        List<OrderItemDTO> selectedItems = orderDTO.getOrderItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            log.error("Order must contain at least one item.");
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        if (orderDTO.getAddressId() != null) {
            Address address = addressRepository.findById(orderDTO.getAddressId())
                    .orElseThrow(() -> {
                        log.error("Address not found with ID: {}", orderDTO.getAddressId());
                        return new ResourceNotFoundException("Address", "Id", orderDTO.getAddressId());
                    });
            order.setShippingAddress(address);
        }

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal totalDiscountedAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<CartItem> toBeRemoved = new ArrayList<>();

        for (OrderItemDTO itemDTO : selectedItems) {
            Long productId = itemDTO.getProductDetails().getId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> {
                        log.error("Product not found with ID: {}", productId);
                        return new ResourceNotFoundException("Product", "Id", productId);
                    });

            CartItem matchingCartItem = cart.getCartItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Product ID {} not found in cart for customer ID {}.", productId, customerId);
                        return new ResourceNotFoundException("CartItem", "ProductId", productId);
                    });

            if (product.getStockQuantity() < itemDTO.getQuantity()) {
                log.error("Insufficient stock for product: {} (ID: {}). Available: {}, Requested: {}",
                        product.getName(), productId, product.getStockQuantity(), itemDTO.getQuantity());
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(product.getPrice());

            Double discountPercentage = product.getDiscountPercentage() != null ? product.getDiscountPercentage() : 0.0;
            orderItem.setDiscountPercentage(discountPercentage);
            
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity()));
            BigDecimal itemDiscountedPrice = itemTotal.subtract(itemTotal.multiply(BigDecimal.valueOf(discountPercentage)).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP));
            orderItem.setDiscountedPrice(itemDiscountedPrice);

            orderItem.setOrder(order);
            orderItems.add(orderItem);

            product.setStockQuantity(product.getStockQuantity() - itemDTO.getQuantity());
            productRepository.save(product);
            log.debug("Updated stock for product ID {}. New quantity: {}", product.getId(), product.getStockQuantity());

            total = total.add(itemTotal);
            totalDiscountedAmount = totalDiscountedAmount.add(itemDiscountedPrice);
            toBeRemoved.add(matchingCartItem);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(total);
        order.setDiscountAmount(total.subtract(totalDiscountedAmount));
        order.setDiscountedAmount(totalDiscountedAmount);

        Order savedOrder = orderRepository.save(order);
        
        // Generate and set the order code after saving for the first time
        generateAndSetOrderCode(savedOrder);
        
        log.info("Partial order created successfully with ID: {}", savedOrder.getId());

        cart.getCartItems().removeAll(toBeRemoved);
        cart.setTotalAmount(cart.getCartItems().stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        cartRepository.save(cart);
        log.debug("Removed {} items from cart for customer ID {}.", toBeRemoved.size(), customerId);

        return convertToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO saveOrder(OrderDTO orderDTO) {
        log.info("Attempting to save a new order for customer ID: {}", orderDTO.getCustomerId());
        Order order = new Order();
        Customer customer = customerRepository.findById(orderDTO.getCustomerId())
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", orderDTO.getCustomerId());
                    return new ResourceNotFoundException("Customer", "Id", orderDTO.getCustomerId());
                });
        order.setCustomer(customer);
        order.setOrderDate(orderDTO.getOrderDate() != null ? orderDTO.getOrderDate() : LocalDateTime.now());
        order.setStatus(orderDTO.getStatus() != null ? orderDTO.getStatus() : "PENDING");

        if (orderDTO.getAddressId() != null) {
            Address address = addressRepository.findById(orderDTO.getAddressId())
                    .orElseThrow(() -> {
                        log.error("Address not found with ID: {}", orderDTO.getAddressId());
                        return new ResourceNotFoundException("Address", "Id", orderDTO.getAddressId());
                    });
            order.setShippingAddress(address);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal totalDiscountedAmount = BigDecimal.ZERO;

        if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
            log.error("Order must contain at least one item.");
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        for (OrderItemDTO itemDto : orderDTO.getOrderItems()) {
            Product product = productRepository.findById(itemDto.getProductDetails().getId())
                    .orElseThrow(() -> {
                        log.error("Product not found with ID: {}", itemDto.getProductDetails().getId());
                        return new ResourceNotFoundException("Product", "Id", itemDto.getProductDetails().getId());
                    });

            if (product.getStockQuantity() < itemDto.getQuantity()) {
                log.error("Insufficient stock for product: {} (ID: {}). Available: {}, Requested: {}",
                        product.getName(), product.getId(), product.getStockQuantity(), itemDto.getQuantity());
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(product.getPrice());
            
            Double discountPercentage = product.getDiscountPercentage() != null ? product.getDiscountPercentage() : 0.0;
            orderItem.setDiscountPercentage(discountPercentage);
            
            BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            BigDecimal itemDiscountedPrice = itemTotal.subtract(itemTotal.multiply(BigDecimal.valueOf(discountPercentage)).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP));
            orderItem.setDiscountedPrice(itemDiscountedPrice);

            order.addOrderItem(orderItem);

            product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());
            productRepository.save(product);
            log.debug("Updated stock for product ID {}. New quantity: {}", product.getId(), product.getStockQuantity());

            totalAmount = totalAmount.add(itemTotal);
            totalDiscountedAmount = totalDiscountedAmount.add(itemDiscountedPrice);
        }

        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(totalAmount.subtract(totalDiscountedAmount));
        order.setDiscountedAmount(totalDiscountedAmount);
        
        Order savedOrder = orderRepository.save(order);
        generateAndSetOrderCode(savedOrder);
        
        log.info("Order saved successfully with ID: {}", savedOrder.getId());
        return convertToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        log.info("Fetching all orders.");
        List<OrderDTO> orders = orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} orders.", orders.size());
        return orders;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", id);
                    return new ResourceNotFoundException("Order", "Id", id);
                });
        
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(Long id, OrderDTO orderDTO) {
        log.info("Attempting to update order with ID: {}", id);
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", id);
                    return new ResourceNotFoundException("Order", "Id", id);
                });

        if (orderDTO.getCustomerId() != null && !existingOrder.getCustomer().getId().equals(orderDTO.getCustomerId())) {
            Customer newCustomer = customerRepository.findById(orderDTO.getCustomerId())
                    .orElseThrow(() -> {
                        log.error("New customer not found with ID: {}", orderDTO.getCustomerId());
                        return new ResourceNotFoundException("Customer", "Id", orderDTO.getCustomerId());
                    });
            existingOrder.setCustomer(newCustomer);
            log.debug("Updated customer for order ID {} to new customer ID {}.", id, orderDTO.getCustomerId());
        }

        if (orderDTO.getOrderDate() != null) {
            existingOrder.setOrderDate(orderDTO.getOrderDate());
        }

        if (orderDTO.getStatus() != null && !orderDTO.getStatus().isEmpty()) {
            log.info("Updating status for order ID {} from '{}' to '{}'.", id, existingOrder.getStatus(), orderDTO.getStatus());
            existingOrder.setStatus(orderDTO.getStatus());
            if ("DELIVERED".equalsIgnoreCase(orderDTO.getStatus())) {
                List<Payment> payments = paymentRepository.findByOrderId(existingOrder.getId());
                if (!payments.isEmpty()) {
                    Payment payment = payments.get(0);
                    if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                        payment.setStatus("COMPLETED");
                        paymentRepository.save(payment);
                        log.info("Payment status updated to COMPLETED for order ID: {}", existingOrder.getId());
                    } else {
                        log.debug("Payment for order ID {} is already COMPLETED.", existingOrder.getId());
                    }
                } else {
                    log.warn("No payment found for order ID: {}. Cannot update payment status.", existingOrder.getId());
                }
            }
        }

        if (orderDTO.getAddressId() != null) {
            Address address = addressRepository.findById(orderDTO.getAddressId())
                    .orElseThrow(() -> {
                        log.error("Address not found with ID: {}", orderDTO.getAddressId());
                        return new ResourceNotFoundException("Address", "Id", orderDTO.getAddressId());
                    });
            existingOrder.setShippingAddress(address);
        }

        log.debug("Reverting stock for old order items for order ID {}.", id);
        for (OrderItem oldItem : existingOrder.getOrderItems()) {
            Product product = oldItem.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + oldItem.getQuantity());
                productRepository.save(product);
                log.debug("Restored {} units to stock for product ID {}.", oldItem.getQuantity(), product.getId());
            }
        }

        existingOrder.getOrderItems().clear();
        BigDecimal newTotalAmount = BigDecimal.ZERO;
        BigDecimal newDiscountedAmount = BigDecimal.ZERO;

        if (orderDTO.getOrderItems() != null && !orderDTO.getOrderItems().isEmpty()) {
            for (OrderItemDTO itemDto : orderDTO.getOrderItems()) {
                Product product = productRepository.findById(itemDto.getProductDetails().getId())
                        .orElseThrow(() -> {
                            log.error("New product not found with ID: {}", itemDto.getProductDetails().getId());
                            return new ResourceNotFoundException("Product", "Id", itemDto.getProductDetails().getId());
                        });

                if (product.getStockQuantity() < itemDto.getQuantity()) {
                    log.error("Insufficient stock for new product: {} (ID: {}). Available: {}, Requested: {}",
                            product.getName(), product.getId(), product.getStockQuantity(), itemDto.getQuantity());
                    throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
                }

                OrderItem newItem = new OrderItem();
                newItem.setProduct(product);
                newItem.setQuantity(itemDto.getQuantity());
                newItem.setPrice(product.getPrice());

                Double discountPercentage = product.getDiscountPercentage() != null ? product.getDiscountPercentage() : 0.0;
                newItem.setDiscountPercentage(discountPercentage);
                
                BigDecimal itemTotal = product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
                BigDecimal itemDiscountedPrice = itemTotal.subtract(itemTotal.multiply(BigDecimal.valueOf(discountPercentage)).divide(BigDecimal.valueOf(100.0), 2, RoundingMode.HALF_UP));
                newItem.setDiscountedPrice(itemDiscountedPrice);

                existingOrder.addOrderItem(newItem);

                product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());
                productRepository.save(product);
                log.debug("Updated stock for new product ID {}. New quantity: {}", product.getId(), product.getStockQuantity());

                newTotalAmount = newTotalAmount.add(itemTotal);
                newDiscountedAmount = newDiscountedAmount.add(itemDiscountedPrice);
            }
        }

        existingOrder.setTotalAmount(newTotalAmount);
        existingOrder.setDiscountAmount(newTotalAmount.subtract(newDiscountedAmount));
        existingOrder.setDiscountedAmount(newDiscountedAmount);
        
        Order updatedOrder = orderRepository.save(existingOrder);
        log.info("Order with ID {} updated successfully.", updatedOrder.getId());
        return convertToDTO(updatedOrder);
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        log.info("Attempting to delete order with ID: {}", id);
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", id);
                    return new ResourceNotFoundException("Order", "Id", id);
                });
        log.debug("Restoring stock for products in order ID {}.", id);
        for (OrderItem item : existingOrder.getOrderItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
                log.debug("Restored {} units to stock for product ID {}.", item.getQuantity(), product.getId());
            }
        }
        orderRepository.delete(existingOrder);
        log.info("Order with ID {} deleted successfully.", id);
    }
    
    private void generateAndSetOrderCode(Order order) {
       
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        String idPart = String.format("%03d", order.getId()); 
       
        String orderCode = "ORD-" + datePart + "-" + idPart;
        
        order.setOrderCode(orderCode);
        orderRepository.save(order);
    }

    private OrderDTO convertToDTO(Order order) {
        log.debug("Converting Order entity to DTO for ID: {}", order.getId());
        OrderDTO dto = new OrderDTO();
        dto.setId(order.getId());
        dto.setOrderCode(order.getOrderCode()); 
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setDiscountAmount(order.getDiscountAmount());
        dto.setDiscountedAmount(order.getDiscountedAmount());
        dto.setStatus(order.getStatus());

        if (order.getCustomer() != null) {
            dto.setCustomerId(order.getCustomer().getId());
            dto.setCustomerUsername(order.getCustomer().getUsername());
            dto.setCustomerCode(order.getCustomer().getCustomerCode());
            if (order.getCustomer().getProfile() != null) {
                dto.setCustomerFirstName(order.getCustomer().getProfile().getFirstName());
                dto.setCustomerLastName(order.getCustomer().getProfile().getLastName());
            }
        }

        if (order.getShippingAddress() != null) {
            Address address = order.getShippingAddress();
            dto.setAddressId(address.getId());
            String fullAddress = address.getStreet() + ", " +
                    address.getCity() + ", " +
                    address.getState() + " - " +
                    address.getZipCode();
            dto.setShippingAddressString(fullAddress);
        }

        if (order.getOrderItems() != null && !order.getOrderItems().isEmpty()) {
            dto.setOrderItems(order.getOrderItems().stream()
                    .map(this::convertOrderItemToDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setOrderItems(new ArrayList<>());
        }
        log.debug("Converted Order entity to DTO for ID: {}. Total items: {}", order.getId(), dto.getOrderItems().size());
        return dto;
    }

    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        log.debug("Converting OrderItem entity to DTO for ID: {}", orderItem.getId());
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());
        dto.setDiscountPercentage(orderItem.getDiscountPercentage());
        dto.setDiscountedPrice(orderItem.getDiscountedPrice());

        if (orderItem.getProduct() != null) {
            ProductDTO productDto = new ProductDTO();
            productDto.setId(orderItem.getProduct().getId());
            productDto.setName(orderItem.getProduct().getName());
            productDto.setPrice(orderItem.getProduct().getPrice());
            productDto.setDescription(orderItem.getProduct().getDescription());
            productDto.setStockQuantity(orderItem.getProduct().getStockQuantity());
            productDto.setImages(orderItem.getProduct().getImageUrlsList());
            productDto.setCategory(orderItem.getProduct().getCategory());
            dto.setProductDetails(productDto);
        }

        return dto;
    }
}