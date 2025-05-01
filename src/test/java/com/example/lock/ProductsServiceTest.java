package com.example.lock;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.config.EmbeddedRedisConfig;
import com.example.lock.domain.entity.Products;
import com.example.lock.domain.repository.ProductsRepository;
import com.example.lock.domain.service.ProductsService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@Import(com.example.config.EmbeddedRedisConfig.class)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "spring.data.redis.host=localhost",
    "spring.data.redis.port=6379",
    "spring.data.redis.password=",
    "spring.data.redis.timeout=60000"
})
public class ProductsServiceTest {

    @Autowired
    private ProductsService productsService;
    @Autowired
    private ProductsRepository productsRepository;

    private Products products;

    @BeforeEach
    public void init() {
        products = Products.builder().stock(10).build();
        productsRepository.save(products);
    }


    @Test
    void 재고차감_분산락_미적용_10명_테스트() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    productsService.decrease(products.getId(), 2);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Products persistProduct = productsRepository.findById(products.getId())
            .orElseThrow(RuntimeException::new);
        // 남은 재고가 0개 되어야 하나 0개가 아님
        assertThat(persistProduct.getStock()).isNotEqualTo(0);
    }

    @Test
    void 재고차감_분산락_적용_10명_테스트() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    productsService.decreaseWithLock(products.getId(), 2);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Products persistProduct = productsRepository.findById(products.getId())
            .orElseThrow(RuntimeException::new);
        assertThat(persistProduct.getStock()).isEqualTo(0);
    }

    @Test
    void 재고차감_분산락_AOP_적용_10명_테스트() throws InterruptedException {
        int numberOfThreads = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch countDownLatch = new CountDownLatch(numberOfThreads);

        for (int i = 0; i < numberOfThreads; i++) {
            executorService.submit(() -> {
                try {
                    productsService.decreaseWithLockAop(products.getId() + ":product",
                        products.getId(), 2);
                } finally {
                    countDownLatch.countDown();
                }
            });
        }

        countDownLatch.await();

        Products persistProduct = productsRepository.findById(products.getId())
            .orElseThrow(RuntimeException::new);
        assertThat(persistProduct.getStock()).isEqualTo(0);
    }

}
