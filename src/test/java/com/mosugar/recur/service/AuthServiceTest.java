package com.mosugar.recur.service;

import com.mosugar.recur.dto.request.LoginRequest;
import com.mosugar.recur.dto.request.RegisterRequest;
import com.mosugar.recur.dto.response.AuthResponse;
import com.mosugar.recur.dto.response.UserResponse;
import com.mosugar.recur.entity.User;
import com.mosugar.recur.enums.Currency;
import com.mosugar.recur.exception.EmailAlreadyExistsException;
import com.mosugar.recur.exception.ResourceNotFoundException;
import com.mosugar.recur.mapper.UserMapper;
import com.mosugar.recur.repository.UserRepository;
import com.mosugar.recur.security.JwtService;
import com.mosugar.recur.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserMapper userMapper;
    @InjectMocks AuthServiceImpl authService;

    private static final UUID USER_ID = UUID.randomUUID();

    private User storedUser() {
        return User.builder()
                .id(USER_ID)
                .email("alice@test.com")
                .passwordHash("hashed")
                .displayName("Alice")
                .mainCurrency(Currency.EUR)
                .build();
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_normalizesInputHashesPasswordAndReturnsToken() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User toSave = inv.getArgument(0);
            toSave.setId(USER_ID);
            return toSave;
        });
        when(jwtService.generateToken(USER_ID, "alice@test.com")).thenReturn("jwt");

        AuthResponse response = authService.register(
                new RegisterRequest("  Alice@Test.COM ", "secret123", "  Alice  "));

        assertThat(response.token()).isEqualTo("jwt");
        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertThat(saved.getValue().getEmail()).isEqualTo("alice@test.com");
        assertThat(saved.getValue().getDisplayName()).isEqualTo("Alice");
        assertThat(saved.getValue().getPasswordHash())
                .isEqualTo("hashed")
                .isNotEqualTo("secret123");
    }

    @Test
    void register_rejectsAnEmailAlreadyTakenWhateverItsCase() {
        when(userRepository.existsByEmail("alice@test.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("ALICE@test.com", "secret123", "Alice")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_authenticatesWithNormalizedEmailThenReturnsToken() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(storedUser()));
        when(jwtService.generateToken(USER_ID, "alice@test.com")).thenReturn("jwt");

        AuthResponse response = authService.login(new LoginRequest(" Alice@Test.com", "secret123"));

        assertThat(response.token()).isEqualTo("jwt");
        ArgumentCaptor<Authentication> auth = ArgumentCaptor.forClass(Authentication.class);
        InOrder order = inOrder(authenticationManager, jwtService);
        order.verify(authenticationManager).authenticate(auth.capture());
        order.verify(jwtService).generateToken(USER_ID, "alice@test.com");
        assertThat(auth.getValue()).isInstanceOf(UsernamePasswordAuthenticationToken.class);
        assertThat(auth.getValue().getPrincipal()).isEqualTo("alice@test.com");
        assertThat(auth.getValue().getCredentials()).isEqualTo("secret123");
    }

    @Test
    void login_withWrongPasswordGivesNoToken() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@test.com", "wrong")))
                .isInstanceOf(BadCredentialsException.class);

        verify(userRepository, never()).findByEmail(any());
        verify(jwtService, never()).generateToken(any(), any());
    }

    @Test
    void login_throwsNotFoundWhenTheUserDisappearedAfterAuthentication() {
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("alice@test.com", "secret123")))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(jwtService, never()).generateToken(any(), any());
    }

    // ── me ───────────────────────────────────────────────────────────────────

    @Test
    void me_returnsTheProfileOfTheAuthenticatedUser() {
        User user = storedUser();
        UserResponse expected = new UserResponse(USER_ID, "alice@test.com", "Alice", Currency.EUR, null);
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("alice@test.com");
        when(userRepository.findByEmail("alice@test.com")).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(expected);

        assertThat(authService.me(principal)).isEqualTo(expected);
    }

    @Test
    void me_throwsNotFoundForAnUnknownUser() {
        UserDetails principal = mock(UserDetails.class);
        when(principal.getUsername()).thenReturn("ghost@test.com");
        when(userRepository.findByEmail("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.me(principal))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
