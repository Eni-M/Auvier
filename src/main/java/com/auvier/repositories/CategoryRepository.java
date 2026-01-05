package com.auvier.repositories;

import com.auvier.entities.catalog.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {

    boolean existsBySlug(String slug);

    Optional<CategoryEntity> findByName(String name);
    Optional<CategoryEntity> findById(Long id);
}
