package com.auvier.mappers;

import com.auvier.dtos.CategoryDto;
import com.auvier.entities.catalog.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class CategoryMapper {

    public abstract CategoryEntity toEntity(CategoryDto dto);

    public abstract CategoryDto toDto(CategoryEntity entity);

    @Mapping(target = "id", ignore = true)
    public abstract void updateEntityFromDto(CategoryDto dto, @MappingTarget CategoryEntity entity);
}

