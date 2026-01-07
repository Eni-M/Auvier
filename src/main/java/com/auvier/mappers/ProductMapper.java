package com.auvier.mappers;

import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<ProductDto, ProductEntity> {


    @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    ProductEntity toEntity(ProductDto dto);


    @Mapping(target = "categoryId", source = "category.id")
    ProductDto toDto(ProductEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);
}
