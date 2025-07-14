package com.acc.repository;
import com.acc.entity.Cart;
import com.acc.entity.Customer; 
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; 
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomer(Customer customer);
    Optional<Cart> findByCustomer_Id(Long customerId);
    Optional<Cart> findByCustomerId(Long customerId);

}