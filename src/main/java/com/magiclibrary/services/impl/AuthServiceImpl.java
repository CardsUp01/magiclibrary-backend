package com.magiclibrary.services.impl;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.magiclibrary.dto.auth.LoginRequestDTO;
import com.magiclibrary.dto.auth.LoginResponseDTO;
import com.magiclibrary.dto.auth.RegisterRequestDTO;
import com.magiclibrary.entities.Role;
import com.magiclibrary.entities.User;
import com.magiclibrary.repositories.interfaces.RoleRepository;
import com.magiclibrary.repositories.interfaces.UserRepository;
import com.magiclibrary.security.jwt.JwtUtil;
import com.magiclibrary.services.auth.AuthService;

import com.magiclibrary.exceptions.custom.EmailAlreadyExistsException;
import com.magiclibrary.exceptions.custom.ForbiddenException;
import com.magiclibrary.exceptions.custom.InvalidCredentialsException;
import com.magiclibrary.exceptions.custom.RoleNotFoundException;

@Service
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {

        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        User user = userRepository.findByEmailUserWithRole(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Identifiants invalides"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordUser())) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        if (!Boolean.TRUE.equals(user.getActiveUser())) {
            throw new ForbiddenException("Compte inactif.");
        }

        Integer userId = user.getIdUser();
        String email = user.getEmailUser();
        String role = (user.getRole() != null) ? user.getRole().getLabelRole() : null;

        if (role == null) {
            throw new InvalidCredentialsException("Identifiants invalides");
        }

        boolean rememberMe = request.isRememberMe();

        String token = jwtUtil.generateToken(userId, email, role, rememberMe);
        LocalDateTime expiresAt = jwtUtil.getExpirationDate(token);

        return new LoginResponseDTO(token, expiresAt, userId, role);
    }

    @Override
    public void register(RegisterRequestDTO request) {

        if (request == null || request.getEmail() == null || request.getPassword() == null) {
            throw new InvalidCredentialsException("Données d'inscription invalides.");
        }

        if (userRepository.existsByEmailUser(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email déjà utilisé.");
        }

        Role memberRole = roleRepository.findByLabelRole("MEMBRE")
                .orElseThrow(() -> new RoleNotFoundException("Rôle MEMBRE introuvable."));

        String civilityDefault = "M";
        boolean subscriptionDefault = false;

        User user = new User(
                memberRole,
                civilityDefault,
                request.getFirstName(),
                request.getLastName(),
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                true,
                subscriptionDefault,
                LocalDateTime.now()
        );

        user.setDepositUser(false);
        user.setEmailVerifiedUser(false);

        userRepository.save(user);
    }
}