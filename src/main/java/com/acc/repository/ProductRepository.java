package com.acc.repository;

import com.acc.entity.Category;
import com.acc.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrue();
    boolean existsByNameAndCategory(String name, Category category);
    Optional<Product> findByNameAndCategory(String name, Category category);
    Optional<Product> findByName(String name);


	
}
