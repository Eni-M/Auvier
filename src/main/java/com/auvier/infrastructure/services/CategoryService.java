package com.auvier.infrastructure.services;

import com.auvier.dtos.CategoryDto;
import com.auvier.infrastructure.genericservices.CrudService;


public interface CategoryService extends CrudService<CategoryDto, Long> {
    CategoryDto findCategoryByName(String name);
}
