package com.auvier.mappers;

import com.auvier.dtos.ProductDto;
import com.auvier.entities.catalog.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class ProductMapper implements BaseMapper<ProductDto, ProductEntity> {

//    @Autowired
//    protected CategoryRepository categoryRepository;

    // TODO: Uncomment when Category is implemented
    // @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    public abstract ProductEntity toEntity(ProductDto dto);

    // TODO: Uncomment when Category is implemented
    // @Mapping(target = "categoryId", source = "category.id")
    public abstract ProductDto toDto(ProductEntity entity);

    // CRITICAL: This updates an existing entity without creating a new one
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    // TODO: Uncomment when Category is implemented
    // @Mapping(target = "category", source = "categoryId", qualifiedByName = "idToCategory")
    public abstract void updateEntityFromDto(ProductDto dto, @MappingTarget ProductEntity entity);

//    @Named("idToCategory")
//    protected CategoryEntity idToCategory(Long id) {
//        return id == null ? null : categoryRepository.findById(id)
//                .orElseThrow(() -> new IllegalArgumentException("Category not found: " + id));
//    }
}
