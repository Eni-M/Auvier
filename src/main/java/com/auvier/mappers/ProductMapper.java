package com.auvier.mappers;

import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductMapper extends BaseMapper<ProductDto, ProductEntity> {

    // TODO: Uncomment when Category is implemented
    // @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    ProductEntity toEntity(ProductDto dto);

    // TODO: Uncomment when Category is implemented
    // @Mapping(target = "categoryId", source = "category.id")
    ProductDto toDto(ProductEntity entity);

    // CRITICAL: This updates an existing entity without creating a new one
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    // TODO: Uncomment when Category is implemented
    // @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);
}
