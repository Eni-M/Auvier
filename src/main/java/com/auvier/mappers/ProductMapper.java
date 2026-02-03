package com.auvier.mappers;

import com.auvier.dtos.CategoryDto;
import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.CategoryEntity;
import com.auvier.entities.catalog.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper extends BaseMapper<ProductDto, ProductEntity> {

    @Override
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryDtoToEntity")
    @Mapping(target = "subCategory", source = "subCategory", qualifiedByName = "categoryDtoToEntity")
    ProductEntity toEntity(ProductDto dto);

    @Override
    ProductDto toDto(ProductEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "category", source = "category", qualifiedByName = "categoryDtoToEntity")
    @Mapping(target = "subCategory", source = "subCategory", qualifiedByName = "categoryDtoToEntity")
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);

    @Named("categoryDtoToEntity")
    default CategoryEntity categoryDtoToEntity(CategoryDto dto) {
        if (dto == null || dto.getId() == null) return null;
        CategoryEntity entity = new CategoryEntity();
        entity.setId(dto.getId());
        return entity;
    }
}


