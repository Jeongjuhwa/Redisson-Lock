package com.example.lock.domain.service;

import com.example.lock.aop.annotation.DistributedLock;
import com.example.lock.domain.dto.ProductsDTO;
import com.example.lock.domain.entity.Products;
import com.example.lock.domain.repository.ProductsRepository;
import jakarta.transaction.Transactional;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductsService {

    private final ProductsRepository productsRepository;
    private final RedissonClient redissonClient;

    // 락 없이
    @Transactional
    public void decrease(Long productId, int stock) {
        Products products = productsRepository.findById(productId)
            .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
        products.decreaseStock(stock);
    }

    // 락 존재
    @Transactional
    public void decreaseWithLock(Long productId, int stock) {
        String lockKey = String.valueOf(productId).concat(":product");
        RLock rLock = redissonClient.getLock(lockKey);

        try {
            boolean rock = rLock.tryLock(3, 3, TimeUnit.SECONDS);

            if (rock) {
                Products products = productsRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다."));
                products.decreaseStock(stock);
            } else {
                throw new RuntimeException("락 획득에 실패했습니다.");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("락 대기 중 인터럽트 발생", e);
        } finally {
            if (rLock != null && rLock.isLocked()) {
                rLock.unlock();
            }
        }

    }

    // 트랜잭션 어노테이션 존재 x
    @DistributedLock(key = "#lockKey")
    public void decreaseWithLockAop(String lockKey, Long productId, int stock) {
        Products products = productsRepository.findById(productId)
            .orElseThrow(RuntimeException::new);
        products.decreaseStock(stock);
    }


}
