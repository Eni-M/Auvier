package com.auvier.mappers;

import com.auvier.dtos.CategoryDto;
import com.auvier.entities.catalog.CategoryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<CategoryDto, CategoryEntity>{
    @Override
    CategoryEntity toEntity(CategoryDto dto);
}
