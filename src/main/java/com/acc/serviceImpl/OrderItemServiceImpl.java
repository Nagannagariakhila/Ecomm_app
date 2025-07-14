package com.acc.serviceImpl;

import com.acc.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
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
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private OrderRepository orderRepository;

    private OrderItemDTO convertToDTO(OrderItem orderItem) {
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
        OrderItem orderItem = new OrderItem();
        orderItem.setQuantity(orderItemDTO.getQuantity());
        return orderItem;
    }

    @Override
    @Transactional
    public OrderItemDTO saveOrderItem(OrderItemDTO orderItemDTO) {
        ProductDTO productDto = orderItemDTO.getProductDetails();
        if (productDto == null || productDto.getId() == null) {
            throw new IllegalArgumentException("Product ID is required for an order item.");
        }
        if (orderItemDTO.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Product product = productRepository.findById(productDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", productDto.getId()));

        if (product.getStockQuantity() < orderItemDTO.getQuantity()) {
            throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() + ". Available: " + product.getStockQuantity() + ", Requested: " + orderItemDTO.getQuantity());
        }

        if (orderItemDTO.getOrderId() == null) {
            throw new IllegalArgumentException("Order ID is required for an order item.");
        }
        Order order = orderRepository.findById(orderItemDTO.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", orderItemDTO.getOrderId()));

        OrderItem orderItem = convertToEntity(orderItemDTO);
        orderItem.setProduct(product);
        orderItem.setPrice(product.getPrice());
        orderItem.setOrder(order);

        product.setStockQuantity(product.getStockQuantity() - orderItemDTO.getQuantity());
        productRepository.save(product);
        OrderItem savedOrderItem = orderItemRepository.save(orderItem);

        return convertToDTO(savedOrderItem);
    }

    @Override
    @Transactional
    public OrderItemDTO updateOrderItem(Long id, OrderItemDTO orderItemDTO) {
        OrderItem existingOrderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "Id", id));

        Integer oldQuantity = existingOrderItem.getQuantity();
        Integer newQuantity = orderItemDTO.getQuantity();

        if (newQuantity <= 0) {
            throw new IllegalArgumentException("Quantity must be at least 1.");
        }

        Product product = existingOrderItem.getProduct();

        int quantityDifference = oldQuantity - newQuantity;

        if (quantityDifference < 0) {
            int stockToReduce = Math.abs(quantityDifference);
            if (product.getStockQuantity() < stockToReduce) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName() + ". Available: " + product.getStockQuantity() + ", Requested increase: " + stockToReduce);
            }
            product.setStockQuantity(product.getStockQuantity() - stockToReduce);
        } else if (quantityDifference > 0) {
            product.setStockQuantity(product.getStockQuantity() + quantityDifference);
        }
        productRepository.save(product);

        existingOrderItem.setQuantity(newQuantity);
        existingOrderItem.setPrice(product.getPrice());

        OrderItem updatedOrderItem = orderItemRepository.save(existingOrderItem);

        return convertToDTO(updatedOrderItem);
    }

    @Override
    @Transactional
    public void deleteOrderItem(Long id) {
        OrderItem existingOrderItem = orderItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "Id", id));

        Product product = existingOrderItem.getProduct();
        product.setStockQuantity(product.getStockQuantity() + existingOrderItem.getQuantity());
        productRepository.save(product);

        if (existingOrderItem.getOrder() != null) {
            
        }

        orderItemRepository.delete(existingOrderItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderItemDTO> getAllOrderItems() {
        return orderItemRepository.findAllWithOrderAndProduct().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderItemDTO getOrderItemById(Long id) {
        OrderItem orderItem = orderItemRepository.findByIdWithOrderAndProduct(id)
                .orElseThrow(() -> new ResourceNotFoundException("OrderItem", "Id", id));
        return convertToDTO(orderItem);
    }
}