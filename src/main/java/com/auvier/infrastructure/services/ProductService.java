package com.auvier.infrastructure.services;

import com.auvier.dtos.ProductDto;
import com.auvier.infrastructure.genericservices.CrudService;

public interface ProductService extends CrudService<ProductDto, Long> {
    ProductDto findProductbyName(String name);
}
