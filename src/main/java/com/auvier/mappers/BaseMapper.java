package com.auvier.mappers;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public interface BaseMapper<D, E> {

    E toEntity(D dto);
    D toDto(E entity);

    default List<E> toEntityList(List<D> dtos) {
        if (dtos == null) return Collections.emptyList();
        return dtos.stream().map(this::toEntity).collect(Collectors.toList());
    }

    default List<D> toDtoList(List<E> entities) {
        if (entities == null) return Collections.emptyList();
        return entities.stream().map(this::toDto).collect(Collectors.toList());
    }
}



