package com.auvier.infrastructure.services.impl;


import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.ProductEntity;
import com.auvier.exceptions.ProductConflictException;
import com.auvier.exceptions.ProductNotFoundException;
import com.auvier.infrastructure.services.ProductService;
import com.auvier.mappers.ProductMapper;
import com.auvier.repositories.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository repository;
    private final ProductMapper mapper;

    @Override
    public ProductDto add(ProductDto dto) {
        if (repository.existsBySlug(dto.getSlug())) {
            throw new ProductConflictException("Slug already exists!");
        }

        ProductEntity entity = mapper.toEntity(dto);

        // Set the creator once. Because updatable = false, this stays forever.
        entity.setCreatedBy("admin_user");

        return mapper.toDto(repository.save(entity));
    }

    @Override
    public ProductDto modify(Long id, ProductDto dto) {
        ProductEntity existing = repository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Not found"));

        // Update fields from DTO to Entity
        mapper.updateEntityFromDto(dto, existing);

        // Set the editor. Because insertable = false, this only works on updates.
        existing.setUpdatedBy("admin_user");

        return mapper.toDto(repository.save(existing));
    }

    @Override
    public void remove(Long id) {
        if (!repository.existsById(id)) {
            throw new ProductNotFoundException("Product with id: " + id + " not found");
        }
        repository.deleteById(id);
    }

    // ... findOne and findProductbyName stay similar but use mapper
}

