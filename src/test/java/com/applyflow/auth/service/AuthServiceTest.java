package com.applyflow.auth.service;

import com.applyflow.auth.dto.AuthResponse;
import com.applyflow.auth.dto.LoginRequest;
import com.applyflow.auth.dto.RegisterRequest;
import com.applyflow.common.exception.EmailAlreadyExistsException;
import com.applyflow.common.exception.InvalidCredentialsException;
import com.applyflow.user.Role;
import com.applyflow.user.User;
import com.applyflow.user.UserRepository;
import com.applyflow.user.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    private AuthService authService;

    private final RegisterRequest registerRequest =
            new RegisterRequest("  Test@Example.com ", "password123", " Test ", "User");

    @BeforeEach
    void setUp() {
        when(passwordEncoder.encode("dummy-password")).thenReturn("dummy-hash");
        authService = new AuthService(userRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_normalizesEmailAndStoresHashedPassword() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = authService.register(registerRequest);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getEmail()).isEqualTo("test@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
        assertThat(saved.getFirstName()).isEqualTo("Test");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(response.email()).isEqualTo("test@example.com");
    }

    @Test
    void register_duplicateEmail_throwsAndDoesNotSave() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void register_uniqueIndexViolationFromParallelRequest_isTranslated() {
        when(userRepository.existsByEmailIgnoreCase("test@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void login_validCredentials_returnsToken() {
        User user = new User("test@example.com", "hashed", "Test", "User", Role.USER);
        AuthResponse token = new AuthResponse("jwt", "Bearer", 3600);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtService.issueToken(user)).thenReturn(token);

        AuthResponse result = authService.login(new LoginRequest("Test@Example.com", "password123"));

        assertThat(result).isEqualTo(token);
    }

    @Test
    void login_wrongPassword_throwsAndIssuesNoToken() {
        User user = new User("test@example.com", "hashed", "Test", "User", Role.USER);
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("test@example.com", "wrong")))
                .isInstanceOf(InvalidCredentialsException.class);
        verify(jwtService, never()).issueToken(any());
    }

    @Test
    void login_unknownEmail_throwsAndStillChecksPassword() {
        when(userRepository.findByEmailIgnoreCase("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("nobody@example.com", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
        // BCrypt runs against a dummy hash, so timing does not reveal that the email is unknown
        verify(passwordEncoder).matches("password123", "dummy-hash");
        verify(jwtService, never()).issueToken(any());
    }
}