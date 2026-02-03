package com.auvier.infrastructure.services.impl;

import com.auvier.dtos.CategoryDto;
import com.auvier.exception.ResourceNotFoundException;
import com.auvier.exception.DuplicateResourceException;
import com.auvier.infrastructure.services.CategoryService;
import com.auvier.mappers.CategoryMapper;
import com.auvier.repositories.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    @Override
    public CategoryDto findCategoryByName(String name) {
        var entity = repository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with name: " + name));
        return mapper.toDto(entity);
    }

    @Override
    public CategoryDto add(CategoryDto model) {
        if (model.getId() != null && model.getId() != 0) {
            if (repository.existsById(model.getId())) {
                throw new DuplicateResourceException("Category with id: " + model.getId() + " already exists");
            }
        }

        var entity = mapper.toEntity(model);
        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public List<CategoryDto> findAll() {
        return mapper.toDtoList(repository.findAll());
    }

    @Override
    public List<CategoryDto> findByActiveTrue() {
        return mapper.toDtoList(repository.findByActiveTrue());
    }

    @Override
    public List<CategoryDto> findParentCategories() {
        return mapper.toDtoList(repository.findByParentIsNullAndActiveTrue());
    }

    @Override
    public List<CategoryDto> findChildCategories() {
        return mapper.toDtoList(repository.findByParentIsNotNullAndActiveTrue());
    }


    @Override
    public CategoryDto findOne(Long id) {
        var entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return mapper.toDto(entity);
    }

    @Override
    public CategoryDto modify(Long id, CategoryDto model) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }

        var entity = mapper.toEntity(model);
        entity.setId(id);

        repository.save(entity);
        return mapper.toDto(entity);
    }

    @Override
    public void remove(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        repository.deleteById(id);
    }
}

