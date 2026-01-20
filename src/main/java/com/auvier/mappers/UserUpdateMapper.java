package com.auvier.mappers;

import com.auvier.dtos.user.UserUpdateDto;
import com.auvier.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface UserUpdateMapper extends BaseMapper<UserUpdateDto, UserEntity>{

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "password", ignore = true)
    void updateEntityFromDto(UserUpdateDto dto, @MappingTarget UserEntity entity);

}
