package com.acc.controller;

import com.acc.dto.PaymentDTO;
import com.acc.service.PaymentService;
import com.acc.service.RazorpayService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

	@Autowired
	private PaymentService paymentService;
	
	@Autowired
    private RazorpayService razorpayService;

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
            System.out.println("Received paymentDTO: " + paymentDTO); // ✅ Log incoming data
            PaymentDTO savedPayment = paymentService.savePayment(paymentDTO);
            return new ResponseEntity<>(savedPayment, HttpStatus.CREATED);
        } catch (Exception e) {
            e.printStackTrace(); // ✅ Log the full error
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