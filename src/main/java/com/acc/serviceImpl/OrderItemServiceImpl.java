package com.acc.serviceImpl;

import com.acc.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acc.dto.OrderItemDTO;
import com.acc.dto.ProductDTO;
import com.acc.entity.Order;
import com.acc.entity.OrderItem;
import com.acc.entity.Product;
import com.acc.repository.OrderItemRepository;
import com.acc.repository.OrderRepository;
import com.acc.repository.ProductRepository;
import com.acc.service.OrderItemService;

@Service
public class OrderItemServiceImpl implements OrderItemService {

    private static final Logger log = LoggerFactory.getLogger(OrderItemServiceImpl.class);

    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    private OrderItemDTO convertToDTO(OrderItem orderItem) {
        log.debug("Converting OrderItem entity to DTO for ID: {}", orderItem.getId());
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());

        if (orderItem.getProduct() != null) {
            ProductDTO productDto = new ProductDTO();
            productDto.setId(orderItem.getProduct().getId());
            productDto.setName(orderItem.getProduct().getName());
            productDto.setDescription(orderItem.getProduct().getDescription());
            productDto.setCategory(orderItem.getProduct().getCategory());
            if (orderItem.getProduct().getImageUrlsList() != null) {
                productDto.setImages(orderItem.getProduct().getImageUrlsList());
            } else {
                productDto.setImages(List.of());
            }
            productDto.setStockQuantity(orderItem.getProduct().getStockQuantity());
            productDto.setPrice(orderItem.getProduct().getPrice());
            dto.setProductDetails(productDto);
        }

        if (orderItem.getOrder() != null) {
            dto.setOrderId(orderItem.getOrder().getId());
        }
        return dto;
    }

    private OrderItem convertToEntity(OrderItemDTO orderItemDTO) {
        log.debug("Converting OrderItem DTO to entity.");
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(orderItemDTO.getQuantity());
        return orderItem;
    }

    @Override
    @Transactional
    public OrderItemDTO saveOrderItem(OrderItemDTO orderItemDTO) {
        log.info("Attempting to save new order item for order ID: {}", orderItemDTO.getOrderId());
        ProductDTO productDto = orderItemDTO.getProductDetails();
        if (productDto == null || productDto.getId() == null) {
            log.error("Product ID is missing in the order item DTO.");
            throw new IllegalArgumentException("Product ID is required for an order item.");
        }
        if (orderItemDTO.getQuantity() <= 0) {
            log.error("Invalid quantity provided: {}", orderItemDTO.getQuantity());
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Product product = productRepository.findById(productDto.getId())
                .orElseThrow(() -> {
                    log.error("Product not found with ID: {}", productDto.getId());
                    return new ResourceNotFoundException("Product", "Id", productDto.getId());
                });

        if (product.getStockQuantity() < orderItemDTO.getQuantity()) {
            log.error("Insufficient stock for product: {}. Available: {}, Requested: {}",
                    product.getName(), product.getStockQuantity(), orderItemDTO.getQuantity());
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() + ". Available: " + product.getStockQuantity() + ", Requested: " + orderItemDTO.getQuantity());
        }

        if (orderItemDTO.getOrderId() == null) {
            log.error("Order ID is missing for the order item.");
            throw new IllegalArgumentException("Order ID is required for an order item.");
        }
        Order order = orderRepository.findById(orderItemDTO.getOrderId())
                .orElseThrow(() -> {
                    log.error("Order not found with ID: {}", orderItemDTO.getOrderId());
                    return new ResourceNotFoundException("Order", "Id", orderItemDTO.getOrderId());
                });

        OrderItem orderItem = convertToEntity(orderItemDTO);
        orderItem.setProduct(product);
        orderItem.setPrice(product.getPrice());
        orderItem.setOrder(order);

        product.setStockQuantity(product.getStockQuantity() - orderItemDTO.getQuantity());
        productRepository.save(product);
        log.info("Stock updated for product ID {}. New quantity: {}", product.getId(), product.getStockQuantity());
        
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);
        log.info("Order item saved successfully with ID: {}", savedOrderItem.getId());

        return convertToDTO(savedOrderItem);
    }

    @Override
    @Transactional
    public OrderItemDTO updateOrderItem(Long id, OrderItemDTO orderItemDTO) {
        log.info("Attempting to update order item with ID: {}", id);
        OrderItem existingOrderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("OrderItem not found with ID: {}", id);
                    return new ResourceNotFoundException("OrderItem", "Id", id);
                });

        Integer oldQuantity = existingOrderItem.getQuantity();
        Integer newQuantity = orderItemDTO.getQuantity();

        if (newQuantity <= 0) {
            log.error("Invalid new quantity provided for order item ID {}: {}", id, newQuantity);
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Product product = existingOrderItem.getProduct();

        int quantityDifference = oldQuantity - newQuantity;

        if (quantityDifference < 0) {
            int stockToReduce = Math.abs(quantityDifference);
            if (product.getStockQuantity() < stockToReduce) {
                log.error("Insufficient stock to update order item ID {}. Product: {}. Available: {}, Requested increase: {}",
                        id, product.getName(), product.getStockQuantity(), stockToReduce);
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() + ". Available: " + product.getStockQuantity() + ", Requested increase: " + stockToReduce);
            }
            product.setStockQuantity(product.getStockQuantity() - stockToReduce);
        } else if (quantityDifference > 0) {
            product.setStockQuantity(product.getStockQuantity() + quantityDifference);
        }
        productRepository.save(product);
        log.info("Stock updated for product ID {} during update of order item {}. New quantity: {}", product.getId(), id, product.getStockQuantity());

        existingOrderItem.setQuantity(newQuantity);
        existingOrderItem.setPrice(product.getPrice());

        OrderItem updatedOrderItem = orderItemRepository.save(existingOrderItem);
        log.info("Order item with ID {} updated successfully.", updatedOrderItem.getId());

        return convertToDTO(updatedOrderItem);
    }

    @Override
    @Transactional
    public void deleteOrderItem(Long id) {
        log.info("Attempting to delete order item with ID: {}", id);
        OrderItem existingOrderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("OrderItem not found with ID: {}", id);
                    return new ResourceNotFoundException("OrderItem", "Id", id);
                });

        Product product = existingOrderItem.getProduct();
        product.setStockQuantity(product.getStockQuantity() + existingOrderItem.getQuantity());
        productRepository.save(product);
        log.info("Restored {} units to stock for product ID {} after deleting order item {}.", existingOrderItem.getQuantity(), product.getId(), id);

        if (existingOrderItem.getOrder() != null) {
            // Note: Cascade remove from order should handle this, but it's good to be aware.
            log.debug("The order item belongs to order ID {}. Deleting this item will impact the order.", existingOrderItem.getOrder().getId());
        }

        orderItemRepository.delete(existingOrderItem);
        log.info("Order item with ID {} deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDTO> getAllOrderItems() {
        log.info("Fetching all order items.");
        List<OrderItemDTO> orderItems = orderItemRepository.findAllWithOrderAndProduct().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} order items.", orderItems.size());
        return orderItems;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItemDTO getOrderItemById(Long id) {
        log.info("Fetching order item with ID: {}", id);
        OrderItem orderItem = orderItemRepository.findByIdWithOrderAndProduct(id)
                .orElseThrow(() -> {
                    log.error("OrderItem not found with ID: {}", id);
                    return new ResourceNotFoundException("OrderItem", "Id", id);
                });
        log.info("Order item with ID {} fetched successfully.", id);
        return convertToDTO(orderItem);
    }
}