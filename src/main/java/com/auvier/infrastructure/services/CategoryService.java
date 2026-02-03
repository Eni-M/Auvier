package com.auvier.infrastructure.services;

import com.auvier.dtos.CategoryDto;
import com.auvier.infrastructure.genericservices.CrudService;

import java.util.List;

public interface CategoryService extends CrudService<CategoryDto, Long> {
    CategoryDto findCategoryByName(String name);
    List<CategoryDto> findByActiveTrue();
    List<CategoryDto> findParentCategories();
    List<CategoryDto> findChildCategories();
}
