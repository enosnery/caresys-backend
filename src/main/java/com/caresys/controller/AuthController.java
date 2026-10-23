package com.caresys.controller;

import com.caresys.dto.DTOs.*;
import com.caresys.model.RefreshToken;
import com.caresys.model.User;
import com.caresys.repository.UserRepository;
import com.caresys.security.JwtUtils;
import com.caresys.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/auth")
@Tag(name = "1. Autenticação (JWT & Refresh Token)", description = "Endpoints de login, hash de senha e renovação de token")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(summary = "Autenticar usuário e obter Access Token (1h) + Refresh Token",
            description = "Recebe usuário e senha, valida diretamente contra o banco de dados e emite o token JWT com 1 hora de expiração.")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        User user = userRepository.findByUsername(loginRequest.getUsername())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body(new MessageResponse("Usuário ou senha inválidos."));
        }

        if (Boolean.FALSE.equals(user.getActive())) {
            return ResponseEntity.status(403).body(new MessageResponse("Usuário inativo no sistema."));
        }

        String jwt = jwtUtils.generateToken(user.getUsername(), user.getRole());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                refreshToken.getToken(),
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        ));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Renovar Access Token com Refresh Token",
            description = "Gera um novo JWT de 1h a partir de um Refresh Token válido.")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    UserDetails userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPasswordHash())
                            .authorities(user.getRole())
                            .build();

                    String token = jwtUtils.generateToken(userDetails, user.getRole());
                    return ResponseEntity.ok(new TokenRefreshResponse(token, requestRefreshToken));
                })
                .orElseThrow(() -> new RuntimeException("Refresh token não encontrado no banco de dados."));
    }

    @GetMapping("/validate")
    @Operation(summary = "Verificar validade do Access Token atual")
    public ResponseEntity<?> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(new MessageResponse("Token ausente."));
        }
        String token = authHeader.substring(7);
        try {
            if (jwtUtils.isTokenExpired(token)) {
                return ResponseEntity.status(401).body(new MessageResponse("Token expirado."));
            }
            String username = jwtUtils.extractUsername(token);
            User user = userRepository.findByUsername(username).orElse(null);
            if (user == null || Boolean.FALSE.equals(user.getActive())) {
                return ResponseEntity.status(401).body(new MessageResponse("Usuário inválido ou inativo."));
            }
            return ResponseEntity.ok(new MessageResponse("Token válido."));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(new MessageResponse("Token inválido ou expirado."));
        }
    }

    @PostMapping("/logout")
    @Operation(summary = "Revogar Refresh Token do usuário atual")
    public ResponseEntity<?> logoutUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails userDetails) {
            User user = userRepository.findByUsername(userDetails.getUsername()).orElse(null);
            if (user != null) {
                refreshTokenService.deleteByUserId(user.getId());
            }
        }
        return ResponseEntity.ok(new MessageResponse("Logout realizado e Refresh Token revogado com sucesso!"));
    }
}
