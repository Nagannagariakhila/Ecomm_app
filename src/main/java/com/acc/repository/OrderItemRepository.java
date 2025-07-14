package com.acc.repository;

import com.acc.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query; // Ensure this is imported
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    @Query("SELECT oi FROM OrderItem oi") 
    @EntityGraph(attributePaths = {"order", "product"}) 
    List<OrderItem> findAllWithOrderAndProduct();

   
    @Query("SELECT oi FROM OrderItem oi WHERE oi.id = :id") 
    @EntityGraph(attributePaths = {"order", "product"}) 
    Optional<OrderItem> findByIdWithOrderAndProduct(Long id);

}