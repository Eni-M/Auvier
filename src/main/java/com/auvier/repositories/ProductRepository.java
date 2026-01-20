package com.auvier.repositories;

import com.auvier.entities.catalog.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {
    boolean existsBySlug(String slug);

    Optional<ProductEntity> findByName(String name);
}
