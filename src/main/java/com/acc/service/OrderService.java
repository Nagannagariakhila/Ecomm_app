package com.acc.service;
import java.util.List;
import com.acc.dto.OrderDTO;
public interface OrderService {
    OrderDTO saveOrder(OrderDTO orderDTO);
    OrderDTO getOrderById(Long id); 
    List<OrderDTO> getAllOrders();
    OrderDTO updateOrder(Long id, OrderDTO orderDTO);
    void deleteOrder(Long id);
	OrderDTO createOrderFromCart(Long cartId);
	OrderDTO savePartialOrder(Long customerId, OrderDTO orderDTO);
	List<OrderDTO> getOrdersByCustomerId(Long customerId);
	
	
}