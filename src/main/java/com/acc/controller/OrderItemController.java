package com.acc.controller;
import com.acc.dto.OrderItemDTO;
import com.acc.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/order-items")
public class OrderItemController {

	@Autowired
	private OrderItemService orderItemService;

	@PostMapping
	public ResponseEntity<OrderItemDTO> createOrderItem(@Validated @RequestBody OrderItemDTO orderItemDTO) {
		OrderItemDTO savedOrderItem = orderItemService.saveOrderItem(orderItemDTO);
		return new ResponseEntity<>(savedOrderItem, HttpStatus.CREATED);
	}

	@GetMapping
	public ResponseEntity<List<OrderItemDTO>> getAllOrderItems() {
		List<OrderItemDTO> orderItems = orderItemService.getAllOrderItems();
		return new ResponseEntity<>(orderItems, HttpStatus.OK);
	}

	@GetMapping("/{id}")
	public ResponseEntity<OrderItemDTO> getOrderItemById(@PathVariable Long id) {
		OrderItemDTO orderItem = orderItemService.getOrderItemById(id);
		return new ResponseEntity<>(orderItem, HttpStatus.OK);
	}

	@PutMapping("/{id}")
	public ResponseEntity<OrderItemDTO> updateOrderItem(@PathVariable Long id,
			@Validated @RequestBody OrderItemDTO orderItemDTO) {
		OrderItemDTO updatedOrderItem = orderItemService.updateOrderItem(id, orderItemDTO);
		return new ResponseEntity<>(updatedOrderItem, HttpStatus.OK);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> deleteOrderItem(@PathVariable Long id) {
		orderItemService.deleteOrderItem(id);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
}