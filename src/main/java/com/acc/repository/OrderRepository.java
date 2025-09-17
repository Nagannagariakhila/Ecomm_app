package com.acc.repository;
import com.acc.entity.Order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

	List<Order> findByCustomerId(Long customerId);
	
	boolean existsByShippingAddress_Id(Long addressId);

	  Optional<Order> findByOrderCode(String orderCode);
	  

	
}