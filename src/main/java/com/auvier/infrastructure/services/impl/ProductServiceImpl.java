package com.auvier.infrastructure.services.impl;


import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.ProductEntity;
import com.auvier.exception.DuplicateResourceException;
import com.auvier.exception.ResourceNotFoundException;
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
            throw new DuplicateResourceException("Product with slug '" + dto.getSlug() + "' already exists");
        }

        ProductEntity entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public ProductDto modify(Long id, ProductDto dto) {
        ProductEntity existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        // Update fields from DTO to Entity
        mapper.updateEntityFromDto(dto, existing);

        return mapper.toDto(repository.save(existing));
    }

    @Override
    public void remove(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        repository.deleteById(id);
    }

    @Override
    public ProductDto findOne(Long id) {
        ProductEntity entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return mapper.toDto(entity);
    }

    @Override
    public List<ProductDto> findAll() {
        return repository.findAll()
                .stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public ProductDto findProductbyName(String name) {
        ProductEntity entity = repository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Product with name: " + name + " not found"));
        return mapper.toDto(entity);
    }
}

