package com.example.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.lock.domain.entity.Products;
import com.example.lock.domain.repository.ProductsRepository;
import com.example.lock.domain.service.ProductsTransactionLockService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
})
public class ProductsTransactionLockServiceTest {

    @Autowired
    private ProductsTransactionLockService productsTransactionLockService;
    @Autowired
    private ProductsRepository productsRepository;

    private Products products;

    @BeforeEach
    public void init() {
        products = Products.builder().stock(10).build();
        productsRepository.save(products);
    }

    @Test
    void 재고감소_트랜잭션_락_미적용_동시_테스트() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int stock = i;
            executorService.submit(() -> {
                try {
                    productsTransactionLockService.decrease(products.getId(), stock + 2);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Products persistProduct = productsRepository.findById(products.getId())
            .orElseThrow(RuntimeException::new);
        // 남은 재고가 5개 되어야 하나 5개가 아님
        assertThat(persistProduct.getStock()).isEqualTo(5);
    }

    @Test
    void 재고감소_트랜잭션_락_적용_동시_테스트() throws InterruptedException {
        int numberOfThreads = 2;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            final int stock = i;
            executorService.submit(() -> {
                try {
                    productsTransactionLockService.decreaseWithPessimisticLock(products.getId(), stock + 2);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Products persistProduct = productsRepository.findById(products.getId())
            .orElseThrow(RuntimeException::new);

        assertThat(persistProduct.getStock()).isEqualTo(5);
    }

}
