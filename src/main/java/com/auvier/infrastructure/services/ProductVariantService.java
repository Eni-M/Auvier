package com.auvier.infrastructure.services;

import com.auvier.dtos.ProductVariantDto;
import com.auvier.infrastructure.genericservices.CrudService;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ProductVariantService extends CrudService<ProductVariantDto, Long> {
    ProductVariantDto findOne(Long id);
    List<ProductVariantDto> findAllByProductId(Long productId);
}
