package com.lotfresh.lotfresh.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class StorageProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "storage_id")
    private Long storageId;

    private Long stock;

    private Date expirationDateStart; // 유통기한 제조일?

    private Date expirationDateEnd; // 유통기한 끝일?
}
