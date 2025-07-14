package com.acc.service;
import com.acc.dto.OrderItemDTO;
import java.util.List;
public interface OrderItemService {
    OrderItemDTO saveOrderItem(OrderItemDTO orderItemDTO);
    List<OrderItemDTO> getAllOrderItems();
    OrderItemDTO getOrderItemById(Long id); 
	void deleteOrderItem(Long id);
	OrderItemDTO updateOrderItem(Long id, OrderItemDTO orderItemDTO);
}