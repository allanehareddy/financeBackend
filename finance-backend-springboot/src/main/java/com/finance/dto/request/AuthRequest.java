package com.finance.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthRequest {

    @Data
    public static class Login {
        @NotBlank @Email
        private String email;

        @NotBlank
        private String password;
    }

    @Data
    public static class CreateUser {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank @Email(message = "Valid email is required")
        private String email;

        @NotBlank @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotNull(message = "Role is required (VIEWER, ANALYST, ADMIN)")
        private String role;
    }

    @Data
    public static class UpdateUser {
        private String name;
        private String role;
        private Boolean active;
    }
}
