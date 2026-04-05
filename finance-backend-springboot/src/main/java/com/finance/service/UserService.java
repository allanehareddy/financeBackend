package com.finance.service;

import com.finance.dto.request.AuthRequest;
import com.finance.dto.response.ApiResponse.PagedResponse;
import com.finance.dto.response.ApiResponse.UserResponse;
import com.finance.entity.Role;
import com.finance.entity.User;
import com.finance.exception.AppException;
import com.finance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PagedResponse<UserResponse> listUsers(Boolean active, String role, int page, int size) {
        Role roleEnum = (role != null) ? Role.valueOf(role.toUpperCase()) : null;
        PageRequest pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> result = userRepository.findAllFiltered(active, roleEnum, pageable);

        return PagedResponse.of(
                result.getContent().stream().map(UserResponse::from).toList(),
                result.getTotalElements(), page, size
        );
    }

    public UserResponse getUser(Long id) {
        return UserResponse.from(findOrThrow(id));
    }

    @Transactional
    public UserResponse createUser(AuthRequest.CreateUser req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new AppException.ConflictException("Email already in use");
        }

        Role role;
        try {
            role = Role.valueOf(req.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException.BadRequestException("Invalid role. Must be VIEWER, ANALYST, or ADMIN");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .active(true)
                .build();

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateUser(Long id, AuthRequest.UpdateUser req, Long requesterId) {
        User user = findOrThrow(id);

        // Prevent self-role-change
        if (id.equals(requesterId) && req.getRole() != null
                && !req.getRole().equalsIgnoreCase(user.getRole().name())) {
            throw new AppException.BadRequestException("You cannot change your own role");
        }

        if (req.getName() != null)   user.setName(req.getName());
        if (req.getActive() != null) user.setActive(req.getActive());
        if (req.getRole() != null) {
            try {
                user.setRole(Role.valueOf(req.getRole().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new AppException.BadRequestException("Invalid role. Must be VIEWER, ANALYST, or ADMIN");
            }
        }

        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id, Long requesterId) {
        if (id.equals(requesterId)) {
            throw new AppException.BadRequestException("You cannot delete your own account");
        }
        User user = findOrThrow(id);
        userRepository.delete(user);
    }

    private User findOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException.NotFoundException("User not found with id: " + id));
    }
}
