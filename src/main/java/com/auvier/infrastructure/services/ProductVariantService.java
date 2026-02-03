package com.auvier.infrastructure.services;

import com.auvier.dtos.ProductVariantDto;
import com.auvier.entities.catalog.ProductVariantEntity;
import com.auvier.infrastructure.genericservices.CrudService;

import java.util.List;


public interface ProductVariantService extends CrudService<ProductVariantDto, Long> {
    ProductVariantDto findOne(Long id);
    List<ProductVariantDto> findAllByProductId(Long productId);
    ProductVariantEntity findEntityById(Long id);
}
