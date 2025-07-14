package com.acc.serviceImpl;

import com.acc.dto.PaymentDTO;
import com.acc.entity.Order;
import com.acc.entity.Payment;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.OrderRepository;
import com.acc.repository.PaymentRepository;
import com.acc.service.PaymentService;
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

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    
    @Autowired
    @Lazy
    private PaymentServiceImpl self;

    private PaymentDTO convertToDTO(Payment payment) {
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
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a positive value.");
        }

        if (dto.getPaymentMethod() == null || dto.getPaymentMethod().trim().isEmpty()) {
            throw new IllegalArgumentException("Payment method is required.");
        }

        if (dto.getStatus() == null || dto.getStatus().trim().isEmpty()) {
            dto.setStatus("PENDING");
        }

        if (dto.getPaymentDate() == null) {
            dto.setPaymentDate(LocalDateTime.now());
        }

        Payment payment = convertToEntity(dto);

        if (dto.getOrderId() != null && dto.getOrderId() != 0) {
            Order order = orderRepository.findById(dto.getOrderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", dto.getOrderId()));
            payment.setOrder(order);
        }

        Payment saved = paymentRepository.save(payment);
        return convertToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", id));
        return convertToDTO(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentDTO> getAllPayments() {
        return paymentRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public PaymentDTO updatePayment(Long id, PaymentDTO dto) {
        Payment existing = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", id));

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
                    .orElseThrow(() -> new ResourceNotFoundException("Order", "Id", dto.getOrderId()));
            existing.setOrder(order);
        }

        Payment updated = paymentRepository.save(existing);
        return convertToDTO(updated);
    }

    @Override
    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", id));
        paymentRepository.delete(payment);
    }

    
    @Override
    public PaymentDTO updatePaymentStatus(Long id, String status) {
        return self._updatePaymentStatusInternal(id, status); 
    }

    
    @Transactional
    public PaymentDTO _updatePaymentStatusInternal(Long id, String status) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment", "Id", id));

        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status must not be empty");
        }

        System.out.println("🔄 Updating payment ID " + id + " from " + payment.getStatus() + " to " + status);

        payment.setStatus(status);
        Payment updated = paymentRepository.saveAndFlush(payment);

        System.out.println("✅ Updated payment ID " + updated.getId() + " to status " + updated.getStatus());

        return convertToDTO(updated);
    }
}
