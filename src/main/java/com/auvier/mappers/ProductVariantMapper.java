package com.auvier.mappers;



import com.auvier.dtos.ProductVariantDto;
import com.auvier.entities.catalog.ProductVariantEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface ProductVariantMapper extends BaseMapper<ProductVariantDto, ProductVariantEntity> {
    @Override ProductVariantEntity toEntity(ProductVariantDto dto);
    @Override ProductVariantDto toDto(ProductVariantEntity entity);
}
