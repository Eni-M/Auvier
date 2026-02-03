package com.auvier.infrastructure.services.impl;


import com.auvier.dtos.ProductVariantDto;
import com.auvier.entities.catalog.ProductEntity;
import com.auvier.entities.catalog.ProductVariantEntity;
import com.auvier.exception.DuplicateResourceException;
import com.auvier.infrastructure.services.ProductVariantService;
import com.auvier.mappers.ProductVariantMapper;
import com.auvier.repositories.ProductRepository;
import com.auvier.repositories.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductVariantServiceImpl implements ProductVariantService {

    private final ProductVariantRepository repository;
    private final ProductRepository productRepository;
    private final ProductVariantMapper mapper;

    @Override
    public List<ProductVariantDto> findAllByProductId(Long productId) {
        if (productId == null) throw new IllegalArgumentException("productId is required");
        return mapper.toDtoList(repository.findByProductIdOrderByIdAsc(productId));
    }

    @Override
    public ProductVariantDto findOne(Long id) {
        ProductVariantEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceAccessException("ProductVariant with id: " + id + " not found"));
        return mapper.toDto(entity);
    }

    @Override
    public ProductVariantDto add(ProductVariantDto dto) {

        if (dto.getId() != null && dto.getId() != 0 && repository.existsById(dto.getId())) {
            throw new DuplicateResourceException("ProductVariant with id: " + dto.getId() + " already exists");
        }

        if (dto.getProductId() == null) throw new IllegalArgumentException("productId is required");
        if (dto.getSku() == null || dto.getSku().isBlank()) throw new IllegalArgumentException("sku is required");

        if (repository.existsBySku(dto.getSku())) {
            throw new ResourceAccessException("ProductVariant with sku: " + dto.getSku() + " already exists");
        }

        ProductEntity product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + dto.getProductId()));

        ProductVariantEntity entity = mapper.toEntity(dto);
        entity.setProduct(product);

        // hard requirements (also match DB constraints)
        if (entity.getPrice() == null) throw new IllegalArgumentException("price is required");
        if (entity.getStock() == null) throw new IllegalArgumentException("stock is required");
        if (entity.getStock() < 0) throw new IllegalArgumentException("stock cannot be negative");
        if (entity.getColor() == null || entity.getColor().isBlank()) throw new IllegalArgumentException("color is required");
        if (entity.getSize() == null) throw new IllegalArgumentException("size is required");

        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public ProductVariantDto modify(Long id, ProductVariantDto dto) {

        ProductVariantEntity existing = repository.findById(id)
                .orElseThrow(() -> new ResourceAccessException("ProductVariant with id: " + id + " not found"));

        if (dto.getProductId() == null) throw new IllegalArgumentException("productId is required");
        if (dto.getSku() == null || dto.getSku().isBlank()) throw new IllegalArgumentException("sku is required");

        // SKU uniqueness, allow keeping same SKU
        if (!dto.getSku().equals(existing.getSku()) && repository.existsBySku(dto.getSku())) {
            throw new DuplicateResourceException("ProductVariant with sku: " + dto.getSku() + " already exists");
        }

        ProductEntity product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + dto.getProductId()));

        ProductVariantEntity entity = mapper.toEntity(dto);
        entity.setId(id);
        entity.setProduct(product);

        if (entity.getPrice() == null) throw new IllegalArgumentException("price is required");
        if (entity.getStock() == null) throw new IllegalArgumentException("stock is required");
        if (entity.getStock() < 0) throw new IllegalArgumentException("stock cannot be negative");
        if (entity.getColor() == null || entity.getColor().isBlank()) throw new IllegalArgumentException("color is required");
        if (entity.getSize() == null) throw new IllegalArgumentException("size is required");

        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public void remove(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceAccessException("ProductVariant with id: " + id + " not found");
        }
        repository.deleteById(id);
    }

    @Override
    public List<ProductVariantDto> findAll() {
        return List.of();
    }

    @Override
    public ProductVariantEntity findEntityById(Long id) {
        return repository.findById(id).orElse(null);
    }
}





