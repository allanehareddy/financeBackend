package com.finance.service;

import com.finance.dto.request.AuthRequest;
import com.finance.dto.response.ApiResponse.LoginResponse;
import com.finance.dto.response.ApiResponse.UserResponse;
import com.finance.entity.User;
import com.finance.exception.AppException;
import com.finance.repository.UserRepository;
import com.finance.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authManager;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public LoginResponse login(AuthRequest.Login req) {
        // Throws BadCredentialsException if invalid — caught by GlobalExceptionHandler
        authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword())
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException.NotFoundException("User not found"));

        if (!user.isActive()) {
            throw new AppException("Account is inactive", org.springframework.http.HttpStatus.FORBIDDEN);
        }

        UserDetails details = userDetailsService.loadUserByUsername(req.getEmail());
        String token = jwtUtil.generateToken(details);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUser(UserResponse.from(user));
        return response;
    }
}
