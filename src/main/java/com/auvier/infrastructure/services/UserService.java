package com.auvier.infrastructure.services;

import com.auvier.dtos.user.UserRegistrationDto;
import com.auvier.dtos.user.UserResponseDto;
import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.entities.UserEntity;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.List;
import java.util.Optional;

public interface UserService extends UserDetailsService {
    Optional<UserEntity> findByUsername(String username);
    Optional<UserSummaryDto> findById(Long id);
    Optional<UserEntity> findByEmail(String email);
    UserResponseDto registerUser(UserRegistrationDto dto);
    UserResponseDto registerAdmin(UserRegistrationDto dto);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    void logout();
    List<UserSummaryDto> findAll();
    void deleteById(Long id);
    UserRegistrationDto updateUser(Long id, UserRegistrationDto dto);
    boolean isDeleteAllowed();
}
