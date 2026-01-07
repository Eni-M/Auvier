package com.auvier.repositories;


import com.auvier.entities.catalog.ProductVariantEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface  ProductVariantRepository extends JpaRepository<ProductVariantEntity, Long> {
    List<ProductVariantEntity> findByProductIdOrderByIdAsc(Long productId);
    boolean existsBySku(String sku);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select v from ProductVariantEntity v where v.id = :id")
//    Optional<ProductVariantEntity> findByIdForUpdate(@Param("id") Long id);

}

