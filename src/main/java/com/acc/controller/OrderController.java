
package com.acc.controller;
import com.acc.dto.OrderDTO;
import com.acc.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

	@Autowired
	private OrderService orderService;

	@PostMapping("/from-cart/{cartId}")
	public ResponseEntity<OrderDTO> createOrderFromCart(@PathVariable Long cartId) {
	    OrderDTO savedOrder = orderService.createOrderFromCart(cartId);
	    return new ResponseEntity<>(savedOrder, HttpStatus.CREATED);
	}

	@PostMapping("/customer/{customerId}/partial")
	public ResponseEntity<OrderDTO> createPartialOrder(
	        @PathVariable Long customerId,
	        @RequestBody OrderDTO orderDTO) {
	    OrderDTO createdOrder = orderService.savePartialOrder(customerId, orderDTO);
	    return ResponseEntity.ok(createdOrder);
	}



	@GetMapping
	public ResponseEntity<List<OrderDTO>> getAllOrders() {
		List<OrderDTO> orders = orderService.getAllOrders();
		return new ResponseEntity<>(orders, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderDTO> getOrderById(@PathVariable Long id) {
		OrderDTO order = orderService.getOrderById(id);
		return new ResponseEntity<>(order, HttpStatus.OK);
	}
	@GetMapping("/customer/{customerId}")
	public ResponseEntity<List<OrderDTO>> getOrdersByCustomerId(@PathVariable Long customerId) {
	    List<OrderDTO> orders = orderService.getOrdersByCustomerId(customerId);
	    return new ResponseEntity<>(orders, HttpStatus.OK);
	}


	@PutMapping("/{id}")
	public ResponseEntity<OrderDTO> updateOrder(@PathVariable Long id, @Validated @RequestBody OrderDTO orderDTO) {
		OrderDTO updatedOrder = orderService.updateOrder(id, orderDTO);
		return new ResponseEntity<>(updatedOrder, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
		orderService.deleteOrder(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}