package com.auvier.mappers;

import com.auvier.dtos.CategoryDto;
import com.auvier.entities.catalog.CategoryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface CategoryMapper extends BaseMapper<CategoryDto, CategoryEntity> {

    @Override
    @Mapping(target = "parent", source = "parentId", qualifiedByName = "idToCategory")
    CategoryEntity toEntity(CategoryDto dto);

    @Override
    @Mapping(target = "parentId", source = "parent.id")
    CategoryDto toDto(CategoryEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "children", ignore = true)
    @Mapping(target = "parent", source = "parentId", qualifiedByName = "idToCategory")
    void updateEntityFromDto(CategoryDto dto, @MappingTarget CategoryEntity entity);

    @Named("idToCategory")
    default CategoryEntity idToCategory(Long id) {
        if (id == null) return null;
        CategoryEntity c = new CategoryEntity();
        c.setId(id);
        return c;
    }
}

