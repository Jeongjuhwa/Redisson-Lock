package com.example.lock.domain.repository;

import com.example.lock.domain.entity.Products;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;

public interface ProductsRepository extends JpaRepository<Products, Long> {

    @Query("""
            SELECT p FROM Products p 
            where p.id = :productId
        """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({
        @QueryHint(name = "javax.persistence.lock.timeout", value = "3000")
    })
    Optional<Products> findByIdForUpdate(Long productId);

}
