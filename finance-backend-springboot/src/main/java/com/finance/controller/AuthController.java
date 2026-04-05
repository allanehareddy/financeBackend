package com.finance.controller;

import com.finance.dto.request.AuthRequest;
import com.finance.dto.response.ApiResponse.LoginResponse;
import com.finance.dto.response.ApiResponse.UserResponse;
import com.finance.entity.User;
import com.finance.repository.UserRepository;
import com.finance.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "1. Authentication", description = "Login to receive a JWT token. Use it as Bearer <token> in all other requests.")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @Operation(
        summary = "Login",
        description = "Authenticate and get a JWT token (valid 24h).\n\n**Demo credentials:**\n- ADMIN: `admin@finance.com` / `Admin@123`\n- ANALYST: `analyst@finance.com` / `Analyst@123`\n- VIEWER: `viewer@finance.com` / `Viewer@123`"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful - copy token and click Authorize above"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials"),
        @ApiResponse(responseCode = "403", description = "Account inactive")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody AuthRequest.Login req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @Operation(
        summary = "Get current user",
        description = "Returns profile of the currently authenticated user.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User profile returned"),
        @ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findByEmail(userDetails.getUsername()).orElseThrow();
        return ResponseEntity.ok(UserResponse.from(user));
    }
}
