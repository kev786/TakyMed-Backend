package com.takymed.auth_service.services.impl;

import com.takymed.auth_service.dto.AuthResponse;
import com.takymed.auth_service.dto.LoginRequest;
import com.takymed.auth_service.dto.RegisterRequest;
import com.takymed.auth_service.entities.User;
import com.takymed.auth_service.exceptions.AuthException;
import com.takymed.auth_service.exceptions.ResourceNotFoundException;
import com.takymed.auth_service.mappers.UserMapper;
import com.takymed.auth_service.repositories.UserRepository;
import com.takymed.auth_service.security.JwtUtils;
import com.takymed.auth_service.services.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;

    @Override
    public AuthResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateToken(authentication.getName());

        User user = userRepository.findByEmail(loginRequest.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Utilisateur non trouvé avec l'email : " + loginRequest.email()));

        return new AuthResponse(jwt, user.getEmail(), user.getRole());
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        if (Boolean.TRUE.equals(userRepository.existsByEmail(registerRequest.email()))) {
            throw new AuthException("Cet email est déjà utilisé !");
        }

        // Map and encode password
        User user = userMapper.toEntity(registerRequest);
        user.setPassword(passwordEncoder.encode(registerRequest.password()));

        userRepository.save(user);

        // Auto-login after registration
        return login(new LoginRequest(registerRequest.email(), registerRequest.password()));
    }
}
