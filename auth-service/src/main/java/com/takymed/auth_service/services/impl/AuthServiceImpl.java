package com.takymed.auth_service.services.impl;

import com.takymed.auth_service.dto.*;
import com.takymed.auth_service.entities.PasswordResetToken;
import com.takymed.auth_service.entities.User;
import com.takymed.auth_service.exceptions.AuthException;
import com.takymed.auth_service.exceptions.ResourceNotFoundException;
import com.takymed.auth_service.mappers.UserMapper;
import com.takymed.auth_service.repositories.PasswordResetTokenRepository;
import com.takymed.auth_service.repositories.UserRepository;
import com.takymed.auth_service.security.JwtUtils;
import com.takymed.auth_service.services.AuthService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final JavaMailSender mailSender;

    public AuthServiceImpl(AuthenticationManager authenticationManager, UserRepository userRepository,
            PasswordResetTokenRepository tokenRepository,
            PasswordEncoder passwordEncoder, JwtUtils jwtUtils, UserMapper userMapper,
            JavaMailSender mailSender) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.userMapper = userMapper;
        this.mailSender = mailSender;
    }

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

    @Override
    @Transactional
    public UserResponse updateProfile(ProfileRequest profileRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AuthException("Utilisateur non authentifié.");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé : " + email));

        user.setLastName(profileRequest.lastName());
        user.setFirstName(profileRequest.firstName());
        user.setPhone(profileRequest.phone());
        user.setAvatarUrl(profileRequest.avatarUrl());

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'email : " + email));

        // Nettoyer les anciens tokens
        tokenRepository.deleteByUser(user);

        // Créer un nouveau token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(LocalDateTime.now().plusHours(24)) // Expire dans 24h
                .build();

        tokenRepository.save(resetToken);

        // Envoi de l'email réel
        sendRecoveryEmail(email, token);
    }

    private void sendRecoveryEmail(String email, String token) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("Récupération de votre mot de passe - TakyMed");
            helper.setText(formatEmailContent(token), true);

            mailSender.send(message);
            log.info("Email de récupération envoyé avec succès à : {}", email);
        } catch (MessagingException e) {
            log.error("Erreur lors de l'envoi de l'email à {} : {}", email, e.getMessage());
            throw new AuthException("Impossible d'envoyer l'email de récupération. Veuillez réessayer plus tard.");
        }
    }

    private String formatEmailContent(String token) {
        return "<html>" +
                "<body style='font-family: Arial, sans-serif;'>" +
                "<div style='max-width: 600px; margin: 0 auto; border: 1px solid #ddd; padding: 20px;'>" +
                "<h2 style='color: #2c3e50;'>Réinitialisation de mot de passe</h2>" +
                "<p>Bonjour,</p>" +
                "<p>Vous avez demandé la réinitialisation de votre mot de passe pour votre compte TakyMed.</p>" +
                "<p>Veuillez utiliser le jeton suivant pour procéder au changement :</p>" +
                "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; font-size: 1.2em; font-weight: bold; text-align: center; color: #e74c3c;'>"
                +
                token +
                "</div>" +
                "<p>Ce jeton est valable pendant 24 heures.</p>" +
                "<p>Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.</p>" +
                "<br>" +
                "<p>L'équipe TakyMed</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new AuthException("Jeton invalide ou introuvable."));

        if (resetToken.isExpired()) {
            tokenRepository.delete(resetToken);
            throw new AuthException("Le jeton a expiré.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Supprimer le token après utilisation
        tokenRepository.delete(resetToken);
    }
}
