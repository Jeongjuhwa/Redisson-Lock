package com.example.lock.domain.repository;

import com.example.lock.domain.entity.Products;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductsRepository extends JpaRepository<Products, Long> {

}
