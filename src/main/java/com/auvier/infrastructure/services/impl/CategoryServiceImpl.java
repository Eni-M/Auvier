package com.auvier.infrastructure.services.impl;

import com.auvier.dtos.CategoryDto;
import com.auvier.entities.catalog.CategoryEntity;
import com.auvier.exceptions.global.ConflictException;
import com.auvier.exceptions.global.NotFoundException;
import com.auvier.infrastructure.services.CategoryService;
import com.auvier.mappers.CategoryMapper;
import com.auvier.repositories.CategoryRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Override
    public CategoryDto add(CategoryDto dto) {
        if (repository.existsBySlug(dto.getSlug())) {
            throw new ConflictException("Slug already exists!");
        }

        CategoryEntity entity = mapper.toEntity(dto);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public CategoryDto modify(Long id, CategoryDto dto) {
        CategoryEntity existing = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found"));

        mapper.updateEntityFromDto(dto, existing);
        return mapper.toDto(repository.save(existing));
    }

    @Override
    public void remove(Long id) {
        if (!repository.existsById(id)) {
            throw new NotFoundException("Category with id " + id + " not found");
        }
        repository.deleteById(id);
    }

    @Override
    public List<CategoryDto> findAll() {
        return mapper.toDtoList(repository.findAll());
    }

    @Override
    public CategoryDto findOne(Long id) {
        return mapper.toDto(repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Category not found")));
    }

    @Override
    public CategoryDto findCategoryByName(String name) {
        return mapper.toDto(repository.findByName(name)
                .orElseThrow(() -> new NotFoundException("Category not found")));
    }
}

