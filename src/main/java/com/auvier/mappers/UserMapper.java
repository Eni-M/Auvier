package com.auvier.mappers;

import com.auvier.dtos.user.UserResponseDto;
import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.dtos.user.UserUpdateDto;
import com.auvier.entities.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.Optional;

@Mapper(componentModel = "spring")
public interface UserMapper extends BaseMapper<UserSummaryDto, UserEntity> {
    UserResponseDto toResponseDto(UserEntity entity);

    UserSummaryDto toSummaryDto(UserEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "email", ignore = true) // optional, if email is immutable
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntityFromDto(UserUpdateDto dto, @MappingTarget UserEntity entity);

    // helper method for Optional
    default Optional<UserSummaryDto> toSummaryDto(Optional<UserEntity> entityOpt) {
        return entityOpt.map(this::toSummaryDto);
    }
}
