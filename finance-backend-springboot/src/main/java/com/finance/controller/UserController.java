package com.finance.controller;

import com.finance.dto.request.AuthRequest;
import com.finance.dto.response.ApiResponse.PagedResponse;
import com.finance.dto.response.ApiResponse.UserResponse;
import com.finance.repository.UserRepository;
import com.finance.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Tag(name = "4. Users", description = "User management. **ADMIN only.** Create users, assign roles, activate/deactivate accounts.")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

    @Operation(summary = "List users", description = "Returns paginated user list. Filter by active status or role. **ADMIN only.**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User list returned"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @GetMapping
    public ResponseEntity<PagedResponse<UserResponse>> list(
            @Parameter(description = "Filter by active status") @RequestParam(required = false) Boolean active,
            @Parameter(description = "Filter by role: VIEWER, ANALYST, ADMIN") @RequestParam(required = false) String role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        return ResponseEntity.ok(userService.listUsers(active, role, page, size));
    }

    @Operation(summary = "Get user by ID", description = "Returns a single user. **ADMIN only.**")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User found"),
        @ApiResponse(responseCode = "404", description = "User not found"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getOne(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUser(id));
    }

    @Operation(summary = "Create user", description = "Creates a new user with a role. **ADMIN only.**")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "User created"),
        @ApiResponse(responseCode = "409", description = "Email already in use"),
        @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody AuthRequest.CreateUser req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.createUser(req));
    }

    @Operation(summary = "Update user", description = "Update name, role, or active status. **ADMIN only.** Admins cannot deactivate themselves.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "User updated"),
        @ApiResponse(responseCode = "400", description = "Cannot deactivate own account"),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    @PatchMapping("/{id}")
    public ResponseEntity<UserResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody AuthRequest.UpdateUser req,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long requesterId = userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
        return ResponseEntity.ok(userService.updateUser(id, req, requesterId));
    }

    @Operation(summary = "Delete user", description = "Permanently deletes a user. **ADMIN only.** Admins cannot delete themselves.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "User deleted"),
        @ApiResponse(responseCode = "400", description = "Cannot delete own account"),
        @ApiResponse(responseCode = "403", description = "Admin access required")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long requesterId = userRepository.findByEmail(userDetails.getUsername()).orElseThrow().getId();
        userService.deleteUser(id, requesterId);
        return ResponseEntity.noContent().build();
    }
}
