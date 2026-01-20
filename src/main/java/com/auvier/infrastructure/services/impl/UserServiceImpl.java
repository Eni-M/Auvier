package com.auvier.infrastructure.services.impl;

import com.auvier.dtos.user.UserRegistrationDto;
import com.auvier.dtos.user.UserResponseDto;
import com.auvier.dtos.user.UserSummaryDto;
import com.auvier.entities.UserEntity;
import com.auvier.enums.Role;
import com.auvier.exception.DuplicateResourceException;
import com.auvier.exception.ResourceNotFoundException;
import com.auvier.infrastructure.services.UserService;
import com.auvier.mappers.UserMapper;
import com.auvier.mappers.UserRegistrationMapper;
import com.auvier.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRegistrationMapper registrationMapper;
    private final UserMapper userMapper;
    private final UserRegistrationMapper userRegistrationMapper;



    @Override
    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<UserSummaryDto> findById(Long id) {
        return userMapper.toSummaryDto(userRepository.findById(id));
    }

    @Override
    public Optional<UserEntity> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public UserResponseDto registerUser(UserRegistrationDto dto) {

        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + dto.getUsername());
        }

        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new DuplicateResourceException("Email already registered: " + dto.getEmail());
        }

        UserEntity user = registrationMapper.toEntity(dto);

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.CUSTOMER);

        UserEntity saved = userRepository.save(user);

        return userMapper.toResponseDto(saved);
    }

    @Override
    public List<UserSummaryDto> findAll() {
        return userMapper.toDtoList(userRepository.findAll());
    }


    @Override
    public UserResponseDto registerAdmin(UserRegistrationDto dto) {

        UserEntity user = registrationMapper.toEntity(dto);

        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(Role.ADMIN);

        return userMapper.toResponseDto(userRepository.save(user));
    }


    public UserEntity save(UserEntity user) {
        return userRepository.save(user);
    }


    public void deleteById(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found with the id: " + id);
        }

        if (!isDeleteAllowed()) {
            throw new RuntimeException("Cannot delete the last remaining user.");
        }
        userRepository.deleteById(id);
    }

    @Override
    public boolean isDeleteAllowed() {
        return userRepository.count() > 1;
    }


    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    public UserRegistrationDto updateUser(Long userId, UserRegistrationDto dto) {
        // Fetch the existing UserEntity by ID
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Map DTO fields to the UserEntity
        userRegistrationMapper.updateEntityFromDto(dto, user);

        // Save the updated entity and return the updated DTO
        UserEntity savedUser = userRepository.save(user);
        return userRegistrationMapper.toDto(savedUser);
    }

    @Override
    public void logout() {
        // 1. Get the "Sticky Note" for this specific request
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null) {
            // 2. The note tells us the username (e.g., "john_doe")
            String username = auth.getName();

            // 3. Use that name to find the full person in your Database
            UserEntity user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // 4. NOW YOU HAVE THE ID!
            Long userId = user.getId();
            System.out.println("I am now logging out User ID: " + userId);

            // At this point, you can update the DB or log the ID.
        }

        // 5. Crumple up the sticky note and throw it away
        SecurityContextHolder.clearContext();
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .roles(user.getRole().name())
                .build();
    }


}
