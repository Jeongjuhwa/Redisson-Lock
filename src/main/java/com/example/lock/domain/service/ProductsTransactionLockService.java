package com.example.lock.domain.service;

import com.example.lock.domain.entity.Products;
import com.example.lock.domain.repository.ProductsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductsTransactionLockService {

    private final ProductsRepository productsRepository;

    // lock 흭득 없음
    public void decrease(Long productId, int stock) {
        Products products = productsRepository.findById(productId)
            .orElseThrow(RuntimeException::new);

        products.decreaseStock(stock);

    }

    public void decreaseWithPessimisticLock(Long productId, int stock) {
        Products products = productsRepository.findByIdForUpdate(productId)
            .orElseThrow(RuntimeException::new);

        products.decreaseStock(stock);
    }


}
