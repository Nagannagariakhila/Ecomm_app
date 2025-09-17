package com.acc.serviceImpl;

import com.acc.dto.PaymentDTO;
import com.acc.entity.Order;
import com.acc.entity.Payment;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.OrderRepository;
import com.acc.repository.PaymentRepository;
import com.acc.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    @Lazy
    private PaymentServiceImpl self;

    private PaymentDTO convertToDTO(Payment payment) {
        log.debug("Converting Payment entity to DTO for ID: {}", payment.getId());
        PaymentDTO dto = new PaymentDTO();
        dto.setId(payment.getId());
        dto.setPaymentDate(payment.getPaymentDate());
        dto.setAmount(payment.getAmount());
        dto.setPaymentMethod(payment.getPaymentMethod());
        dto.setStatus(payment.getStatus());
        dto.setCustomerUsername(payment.getCustomerUsername());
        if (payment.getOrder() != null) {
            dto.setOrderId(payment.getOrder().getId());
        }
        return dto;
    }

    private Payment convertToEntity(PaymentDTO dto) {
        log.debug("Converting Payment DTO to entity for order ID: {}", dto.getOrderId());
        Payment entity = new Payment();
        entity.setPaymentDate(dto.getPaymentDate());
        entity.setAmount(dto.getAmount());
        entity.setPaymentMethod(dto.getPaymentMethod());
        entity.setStatus(dto.getStatus());
        entity.setCustomerUsername(dto.getCustomerUsername());
        return entity;
    }

    @Override
    @Transactional
    public PaymentDTO savePayment(PaymentDTO dto) {
        log.info("Attempting to save a new payment for order ID: {}", dto.getOrderId());
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.error("Invalid amount provided: {}", dto.getAmount());
            throw new IllegalArgumentException("Amount must be a positive value.");
        }

        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
            log.error("Payment method is missing.");
            throw new IllegalArgumentException("Payment method is required.");
        }

        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            dto.setStatus("PENDING");
            log.debug("Payment status was not provided, setting to default: PENDING");
        }

        if (dto.getPaymentDate() == null) {
            dto.setPaymentDate(LocalDateTime.now());
            log.debug("Payment date was not provided, setting to current time.");
        }

        Payment payment = convertToEntity(dto);

        if (dto.getOrderId() != null && dto.getOrderId() != 0) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> {
                        log.error("Order not found with ID: {}", dto.getOrderId());
                        return new ResourceNotFoundException("Order", "Id", dto.getOrderId());
                    });
            payment.setOrder(order);
            log.debug("Associated payment with order ID: {}", order.getId());
        }

        Payment saved = paymentRepository.save(payment);
        log.info("Payment saved successfully with ID: {}", saved.getId());
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long id) {
        log.info("Fetching payment with ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Payment not found with ID: {}", id);
                    return new ResourceNotFoundException("Payment", "Id", id);
                });
        return convertToDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPayments() {
        log.info("Fetching all payments.");
        List<PaymentDTO> payments = paymentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} payments.", payments.size());
        return payments;
    }

    @Override
    @Transactional
    public PaymentDTO updatePayment(Long id, PaymentDTO dto) {
        log.info("Attempting to update payment with ID: {}", id);
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Payment not found with ID: {}", id);
                    return new ResourceNotFoundException("Payment", "Id", id);
                });

        if (dto.getAmount() != null && dto.getAmount().compareTo(BigDecimal.ZERO) > 0) {
            existing.setAmount(dto.getAmount());
        }
        if (dto.getPaymentDate() != null) {
            existing.setPaymentDate(dto.getPaymentDate());
        }
        if (dto.getPaymentMethod() != null && !dto.getPaymentMethod().isBlank()) {
            existing.setPaymentMethod(dto.getPaymentMethod());
        }
        if (dto.getStatus() != null && !dto.getStatus().isBlank()) {
            existing.setStatus(dto.getStatus());
        }
        if (dto.getCustomerUsername() != null) {
            existing.setCustomerUsername(dto.getCustomerUsername());
        }

        if (dto.getOrderId() != null) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> {
                        log.error("Order not found with ID: {}", dto.getOrderId());
                        return new ResourceNotFoundException("Order", "Id", dto.getOrderId());
                    });
            existing.setOrder(order);
        }

        Payment updated = paymentRepository.save(existing);
        log.info("Payment with ID {} updated successfully.", updated.getId());
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        log.info("Attempting to delete payment with ID: {}", id);
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Payment not found with ID: {}", id);
                    return new ResourceNotFoundException("Payment", "Id", id);
                });
        paymentRepository.delete(payment);
        log.info("Payment with ID {} deleted successfully.", id);
    }

    @Override
    public PaymentDTO updatePaymentStatus(Long id, String status) {
        log.info("Calling internal method to update payment status for ID {} to '{}'.", id, status);
        return self._updatePaymentStatusInternal(id, status);
    }

    @Transactional
    public PaymentDTO _updatePaymentStatusInternal(Long id, String status) {
        log.info("Starting internal transaction to update payment status for ID {}...", id);
        
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Payment not found with ID: {}", id);
                    return new ResourceNotFoundException("Payment", "Id", id);
                });

        if (status == null || status.isBlank()) {
            log.error("Status must not be empty for payment ID {}.", id);
            throw new IllegalArgumentException("Status must not be empty");
        }

        log.info("Updating payment ID {} from status '{}' to '{}'.", id, payment.getStatus(), status);

        payment.setStatus(status);
        Payment updated = paymentRepository.saveAndFlush(payment);

        log.info("Payment ID {} successfully updated to status '{}'.", updated.getId(), updated.getStatus());

        return convertToDTO(updated);
    }
}