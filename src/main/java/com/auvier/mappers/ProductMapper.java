package com.auvier.mappers;

import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.CategoryEntity;
import com.auvier.entities.catalog.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", uses = {CategoryMapper.class})
public interface ProductMapper extends BaseMapper<ProductDto, ProductEntity> {

    @Override
    ProductEntity toEntity(ProductDto dto);

    @Override
    ProductDto toDto(ProductEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);
}


