package com.acc.service;
import com.acc.dto.PaymentDTO;
import java.util.List;
public interface PaymentService {
    PaymentDTO savePayment(PaymentDTO paymentDTO);
    PaymentDTO getPaymentById(Long id);
    List<PaymentDTO> getAllPayments();
    PaymentDTO updatePayment(Long id, PaymentDTO paymentDTO);
    void deletePayment(Long id);
    PaymentDTO updatePaymentStatus(Long id, String status);
    

}
