package com.acc.controller;

import com.acc.dto.PaymentDTO;
import com.acc.service.PaymentQrCodeService;
import com.acc.service.PaymentService;
import com.acc.service.RazorpayService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.google.zxing.WriterException;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;
	
	@Autowired
    private RazorpayService razorpayService;
	
	@Autowired
    private PaymentQrCodeService paymentQrCodeService;

    @GetMapping(value = "/generate-qr-code", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQrCodeForPayment(
            @RequestParam String orderCode,
            @RequestParam double amount,
            @RequestParam String receiverName) throws WriterException, IOException {
        
        // This method correctly calls the service method that handles the UPI URL creation
        byte[] qrCode = paymentQrCodeService.generateUpiQrCode(
            receiverName, 
            "your_upi_id@bank", // Ensure this is your actual UPI ID
            amount, 
            orderCode, 
            300, 
            300
        );
        
        return ResponseEntity.ok(qrCode);
    }

    @PostMapping("/create-order")
    public ResponseEntity<String> createOrder(@RequestParam int amount) {
        try {
            String razorpayOrder = razorpayService.createOrder(amount);
            return ResponseEntity.ok(razorpayOrder);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<PaymentDTO> createPayment(@Validated @RequestBody PaymentDTO paymentDTO) {
        try {
            System.out.println("Received paymentDTO: " + paymentDTO); 
            PaymentDTO savedPayment = paymentService.savePayment(paymentDTO);
            return new ResponseEntity<>(savedPayment, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); 
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentDTO> getPaymentById(@PathVariable Long id) {
        PaymentDTO paymentDTO = paymentService.getPaymentById(id);
        return new ResponseEntity<>(paymentDTO, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<PaymentDTO>> getAllPayments() {
        List<PaymentDTO> payments = paymentService.getAllPayments();
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentDTO> updatePayment(@PathVariable Long id,
            @Validated @RequestBody PaymentDTO paymentDTO) {
        PaymentDTO updatedPayment = paymentService.updatePayment(id, paymentDTO);
        return new ResponseEntity<>(updatedPayment, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePayment(@PathVariable Long id) {
        paymentService.deletePayment(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
    
    @PatchMapping("/{id}/status")
    public ResponseEntity<PaymentDTO> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> request
    ) {
        String status = request.get("status");
        PaymentDTO updated = paymentService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(updated);
    }
}