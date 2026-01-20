package com.auvier.mappers;

import com.auvier.dtos.user.UserRegistrationDto;
import com.auvier.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;


@Mapper(componentModel = "spring")
public interface UserRegistrationMapper extends  BaseMapper<UserRegistrationDto, UserEntity>{

    @Override
    @Mapping(target = "id", ignore = true)
    UserEntity toEntity(UserRegistrationDto dto);

    @Override
    UserRegistrationDto toDto(UserEntity entity);

    @Mapping(target = "password", ignore = true) // Don't update password via this method
    @Mapping(target = "email", ignore = true)    // Keep the original email
    void updateEntityFromDto(UserRegistrationDto dto, @MappingTarget UserEntity entity);
}
