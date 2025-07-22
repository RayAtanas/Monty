package com.monty.backend.Controller;

import com.monty.backend.DTO.UserDTO;
import com.monty.backend.Service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final AuthService authService;

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    @Autowired
    public UserController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<UserDTO> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String email = authentication.getName();

            UserDTO user = authService.getCurrentUser(email);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Failed to get current user", e);
            return ResponseEntity.badRequest().build();
        }
    }
}