package com.acc.serviceImpl;

import com.acc.dto.OrderDTO;
import com.acc.dto.OrderItemDTO;
import com.acc.dto.ProductDTO;
import com.acc.entity.*;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.*;
import com.acc.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private AddressRepository addressRepository;
    @Autowired
    private PaymentRepository paymentRepository;


    @Override
    @Transactional
    public OrderDTO createOrderFromCart(Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "Id", cartId));

        if (cart.getCartItems().isEmpty()) {
            throw new IllegalArgumentException("Cannot create an order from an empty cart.");
        }

        Order order = new Order();
        order.setCustomer(cart.getCustomer());
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        BigDecimal totalOrderAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<CartItem> itemsToBeRemoved = new ArrayList<>();

        for (CartItem cartItem : cart.getCartItems()) {
            Product product = cartItem.getProduct();
            if (product == null) continue;

            if (product.getStockQuantity() >= cartItem.getQuantity()) {
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(product.getPrice());
                orderItem.setOrder(order);
                orderItems.add(orderItem);

                product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
                productRepository.save(product);

                totalOrderAmount = totalOrderAmount.add(
                        product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity()))
                );

                itemsToBeRemoved.add(cartItem);
            }
        }

        if (orderItems.isEmpty()) {
            throw new IllegalArgumentException("No available products in cart to place an order.");
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(totalOrderAmount);
        Order savedOrder = orderRepository.save(order);

        cart.getCartItems().removeAll(itemsToBeRemoved);
        cart.setTotalAmount(cart.getCartItems().stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        cartRepository.save(cart);

        return convertToDTO(savedOrder);
    }

    @Override
    public List<OrderDTO> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderDTO savePartialOrder(Long customerId, OrderDTO orderDTO) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "Id", customerId));
        Cart cart = cartRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "CustomerId", customerId));

        List<OrderItemDTO> selectedItems = orderDTO.getOrderItems();
        if (selectedItems == null || selectedItems.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setStatus("PENDING");

        if (orderDTO.getAddressId() != null) {
            Address address = addressRepository.findById(orderDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "Id", orderDTO.getAddressId()));
            order.setShippingAddress(address);
        }

        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();
        List<CartItem> toBeRemoved = new ArrayList<>();

        for (OrderItemDTO itemDTO : selectedItems) {
            Long productId = itemDTO.getProductDetails().getId();
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", productId));

            CartItem matchingCartItem = cart.getCartItems().stream()
                    .filter(ci -> ci.getProduct().getId().equals(productId))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("CartItem", "ProductId", productId));

            if (product.getStockQuantity() < itemDTO.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDTO.getQuantity());
            orderItem.setPrice(product.getPrice());
            orderItem.setOrder(order);
            orderItems.add(orderItem);

            product.setStockQuantity(product.getStockQuantity() - itemDTO.getQuantity());
            productRepository.save(product);

            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(itemDTO.getQuantity())));
            toBeRemoved.add(matchingCartItem);
        }

        order.setOrderItems(orderItems);
        order.setTotalAmount(total);
        Order savedOrder = orderRepository.save(order);

        cart.getCartItems().removeAll(toBeRemoved);
        cart.setTotalAmount(cart.getCartItems().stream()
                .map(ci -> ci.getProduct().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        cartRepository.save(cart);

        return convertToDTO(savedOrder);
    }

    @Override
    @Transactional
    public OrderDTO saveOrder(OrderDTO orderDTO) {
        Order order = new Order();
        Customer customer = customerRepository.findById(orderDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "Id", orderDTO.getCustomerId()));
        order.setCustomer(customer);
        order.setOrderDate(orderDTO.getOrderDate() != null ? orderDTO.getOrderDate() : LocalDateTime.now());
        order.setStatus(orderDTO.getStatus() != null ? orderDTO.getStatus() : "PENDING");

        if (orderDTO.getAddressId() != null) {
            Address address = addressRepository.findById(orderDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "Id", orderDTO.getAddressId()));
            order.setShippingAddress(address);
        }

        BigDecimal totalAmount = BigDecimal.ZERO;

        if (orderDTO.getOrderItems() == null || orderDTO.getOrderItems().isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item.");
        }

        for (OrderItemDTO itemDto : orderDTO.getOrderItems()) {
            Product product = productRepository.findById(itemDto.getProductDetails().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", itemDto.getProductDetails().getId()));

            if (product.getStockQuantity() < itemDto.getQuantity()) {
                throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
            }

            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemDto.getQuantity());
            orderItem.setPrice(product.getPrice());
            order.addOrderItem(orderItem);

            product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());
            productRepository.save(product);

            totalAmount = totalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
        }

        order.setTotalAmount(totalAmount);
        Order savedOrder = orderRepository.save(order);
        return convertToDTO(savedOrder);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDTO> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDTO getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", id));
        return convertToDTO(order);
    }

    @Override
    @Transactional
    public OrderDTO updateOrder(Long id, OrderDTO orderDTO) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", id));

        if (orderDTO.getCustomerId() != null && !existingOrder.getCustomer().getId().equals(orderDTO.getCustomerId())) {
            Customer newCustomer = customerRepository.findById(orderDTO.getCustomerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Customer", "Id", orderDTO.getCustomerId()));
            existingOrder.setCustomer(newCustomer);
        }

        if (orderDTO.getOrderDate() != null) {
            existingOrder.setOrderDate(orderDTO.getOrderDate());
        }

        if (orderDTO.getStatus() != null && !orderDTO.getStatus().isEmpty()) {
            existingOrder.setStatus(orderDTO.getStatus());
            if ("DELIVERED".equalsIgnoreCase(orderDTO.getStatus())) {
                List<Payment> payments = paymentRepository.findByOrderId(existingOrder.getId());
                if (!payments.isEmpty()) {
                    Payment payment = payments.get(0); 
                    if (!"COMPLETED".equalsIgnoreCase(payment.getStatus())) {
                        payment.setStatus("COMPLETED");
                        paymentRepository.save(payment);
                        System.out.println("✅ Payment status updated to COMPLETED for order ID: " + existingOrder.getId());
                    }
                } else {
                    System.out.println("⚠️ No payment found for order ID: " + existingOrder.getId());
                }
            }
        }

        if (orderDTO.getAddressId() != null) {
            Address address = addressRepository.findById(orderDTO.getAddressId())
                    .orElseThrow(() -> new ResourceNotFoundException("Address", "Id", orderDTO.getAddressId()));
            existingOrder.setShippingAddress(address);
        }

        for (OrderItem oldItem : existingOrder.getOrderItems()) {
            Product product = oldItem.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + oldItem.getQuantity());
                productRepository.save(product);
            }
        }

        existingOrder.getOrderItems().clear();
        BigDecimal newTotalAmount = BigDecimal.ZERO;

        if (orderDTO.getOrderItems() != null && !orderDTO.getOrderItems().isEmpty()) {
            for (OrderItemDTO itemDto : orderDTO.getOrderItems()) {
                Product product = productRepository.findById(itemDto.getProductDetails().getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Product", "Id", itemDto.getProductDetails().getId()));

                if (product.getStockQuantity() < itemDto.getQuantity()) {
                    throw new IllegalArgumentException("Insufficient stock for product: " + product.getName());
                }

                OrderItem newItem = new OrderItem();
                newItem.setProduct(product);
                newItem.setQuantity(itemDto.getQuantity());
                newItem.setPrice(product.getPrice());
                existingOrder.addOrderItem(newItem);

                product.setStockQuantity(product.getStockQuantity() - itemDto.getQuantity());
                productRepository.save(product);

                newTotalAmount = newTotalAmount.add(product.getPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity())));
            }
        }

        existingOrder.setTotalAmount(newTotalAmount);
        return convertToDTO(orderRepository.save(existingOrder));
    }

    @Override
    @Transactional
    public void deleteOrder(Long id) {
        Order existingOrder = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", id));
        for (OrderItem item : existingOrder.getOrderItems()) {
            Product product = item.getProduct();
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
                productRepository.save(product);
            }
        }
        orderRepository.delete(existingOrder);
    }

    private OrderDTO convertToDTO(Order order) {
    OrderDTO dto = new OrderDTO();
    dto.setId(order.getId());
    dto.setOrderDate(order.getOrderDate());
    dto.setTotalAmount(order.getTotalAmount());
    dto.setStatus(order.getStatus());

    if (order.getCustomer() != null) {
        dto.setCustomerId(order.getCustomer().getId());
        dto.setCustomerUsername(order.getCustomer().getUsername());
        dto.setCustomerCode(order.getCustomer().getCustomerCode());
        dto.setCustomerFirstName(order.getCustomer().getProfile().getFirstName());
        dto.setCustomerLastName(order.getCustomer().getProfile().getLastName());
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

    return dto;
}


    private OrderItemDTO convertOrderItemToDTO(OrderItem orderItem) {
        OrderItemDTO dto = new OrderItemDTO();
        dto.setId(orderItem.getId());
        dto.setQuantity(orderItem.getQuantity());
        dto.setPrice(orderItem.getPrice());

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
