package com.example.lock.domain.entity;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "products")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Products {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    private int stock;

    public void decreaseStock(int stock) {
        if (this.stock < stock) {
            throw new RuntimeException("남은 재고가 존재하지 않습니다.");
        }

        this.stock -= stock;
    }
}
