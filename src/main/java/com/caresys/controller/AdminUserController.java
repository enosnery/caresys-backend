package com.caresys.controller;

import com.caresys.model.Staff;
import com.caresys.model.User;
import com.caresys.repository.StaffRepository;
import com.caresys.repository.UserRepository;
import com.caresys.security.RefreshTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/admin/users")
@Tag(name = "8. Administração - Usuários", description = "CRUD de usuários exclusivo para Administradores")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminUserController {

    private static final Set<String> VALID_ROLES = Set.of("ROLE_ADMIN", "ROLE_PHYSIO", "ROLE_RECEPTIONIST");

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping
    @Operation(summary = "Listar todos os usuários")
    public List<User> getAll() {
        return userRepository.findAllByOrderByIdAsc();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter usuário por ID")
    public ResponseEntity<User> getById(@PathVariable Long id) {
        return userRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Criar novo usuário (sem exigir senha no cadastro administrativo; senha padrão inicial gerada automaticamente)")
    public ResponseEntity<?> create(@RequestBody User userRequest) {
        if (userRequest.getName() == null || userRequest.getName().trim().length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("message", "O nome completo deve conter ao menos 3 caracteres."));
        }
        if (userRequest.getUsername() == null || userRequest.getUsername().trim().length() < 3) {
            return ResponseEntity.badRequest().body(Map.of("message", "O nome de usuário (login) deve conter ao menos 3 caracteres."));
        }
        if (userRequest.getEmail() == null || !userRequest.getEmail().contains("@")) {
            return ResponseEntity.badRequest().body(Map.of("message", "Informe um e-mail válido."));
        }

        String normalizedUsername = userRequest.getUsername().trim().toLowerCase();
        String normalizedEmail = userRequest.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(normalizedUsername)) {
            return ResponseEntity.badRequest().body(Map.of("message", "Nome de usuário já está em uso."));
        }
        if (userRepository.existsByEmail(normalizedEmail)) {
            return ResponseEntity.badRequest().body(Map.of("message", "E-mail já está em uso."));
        }

        userRequest.setName(userRequest.getName().trim());
        userRequest.setUsername(normalizedUsername);
        userRequest.setEmail(normalizedEmail);

        String role = (userRequest.getRole() != null && VALID_ROLES.contains(userRequest.getRole()))
                ? userRequest.getRole()
                : "ROLE_PHYSIO";
        userRequest.setRole(role);

        // Senha não é solicitada no modal de cadastro administrativo; define senha inicial padrão ("admin123" em SHA-256 + BCrypt)
        String rawOrHash = userRequest.getPasswordHash();
        if (rawOrHash == null || rawOrHash.isBlank()) {
            rawOrHash = sha256Hex("admin123");
        }
        userRequest.setPasswordHash(passwordEncoder.encode(rawOrHash));
        userRequest.setActive(true);

        User saved = userRepository.save(userRequest);
        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Alterar dados de um usuário (nome, login, e-mail, perfil, status ativo)")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody User updated) {
        return userRepository.findById(id).map(user -> {
            if (updated.getName() != null) {
                if (updated.getName().trim().length() < 3) {
                    return ResponseEntity.badRequest().body(Map.of("message", "O nome completo deve conter ao menos 3 caracteres."));
                }
                user.setName(updated.getName().trim());
            }

            if (updated.getUsername() != null && !updated.getUsername().isBlank()) {
                String newUsername = updated.getUsername().trim().toLowerCase();
                if (!user.getUsername().equalsIgnoreCase(newUsername) && userRepository.existsByUsername(newUsername)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Este nome de usuário já está em uso por outra conta."));
                }
                user.setUsername(newUsername);
            }

            if (updated.getEmail() != null && !updated.getEmail().isBlank()) {
                String newEmail = updated.getEmail().trim().toLowerCase();
                if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
                    return ResponseEntity.badRequest().body(Map.of("message", "Este e-mail já está em uso por outra conta."));
                }
                user.setEmail(newEmail);
            }

            if (updated.getRole() != null && VALID_ROLES.contains(updated.getRole())) {
                user.setRole(updated.getRole());
            }

            if (updated.getActive() != null) {
                if ("admin".equalsIgnoreCase(user.getUsername()) && Boolean.FALSE.equals(updated.getActive())) {
                    return ResponseEntity.badRequest().body(Map.of("message", "O administrador principal não pode ser desativado."));
                }
                user.setActive(updated.getActive());
            }

            if (updated.getPasswordHash() != null && !updated.getPasswordHash().isBlank()) {
                user.setPasswordHash(passwordEncoder.encode(updated.getPasswordHash()));
            }

            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/toggle-active")
    @Operation(summary = "Ativar ou desativar usuário")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            if ("admin".equalsIgnoreCase(user.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of("message", "O administrador principal não pode ser desativado."));
            }
            user.setActive(!Boolean.TRUE.equals(user.getActive()));
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir usuário do sistema")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            if ("admin".equalsIgnoreCase(user.getUsername())) {
                return ResponseEntity.badRequest().body(Map.of("message", "O usuário administrador principal não pode ser excluído."));
            }

            // Desvincula colaborador (Staff) associado para evitar violação de chave estrangeira
            Staff linkedStaff = staffRepository.findByUserUsername(user.getUsername()).orElse(null);
            if (linkedStaff != null) {
                linkedStaff.setUser(null);
                staffRepository.save(linkedStaff);
            }

            refreshTokenService.deleteByUserId(user.getId());
            userRepository.delete(user);
            return ResponseEntity.ok(Map.of("message", "Usuário excluído com sucesso."));
        }).orElse(ResponseEntity.notFound().build());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível na JVM", e);
        }
    }
}
