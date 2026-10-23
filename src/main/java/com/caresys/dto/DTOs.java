package com.caresys.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class DTOs {

    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    public static class JwtResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn = 3600L; // 1 hora em segundos
        private Long id;
        private String username;
        private String email;
        private String name;
        private String role;

        public JwtResponse(String accessToken, String refreshToken, Long id, String username, String email, String name, String role) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.id = id;
            this.username = username;
            this.email = email;
            this.name = name;
            this.role = role;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getTokenType() { return tokenType; }
        public Long getExpiresIn() { return expiresIn; }
        public Long getId() { return id; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getName() { return name; }
        public String getRole() { return role; }
    }

    public static class TokenRefreshResponse {
        private String accessToken;
        private String refreshToken;
        private String tokenType = "Bearer";
        private Long expiresIn = 3600L;

        public TokenRefreshResponse(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public String getTokenType() { return tokenType; }
        public Long getExpiresIn() { return expiresIn; }
    }

    public static class MessageResponse {
        private String message;
        public MessageResponse(String message) { this.message = message; }
        public String getMessage() { return message; }
    }
}
