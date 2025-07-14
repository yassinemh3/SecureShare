package com.secureshare.securefiles;

import com.secureshare.securefiles.auth.AuthenticationRequest;
import com.secureshare.securefiles.auth.AuthenticationResponse;
import com.secureshare.securefiles.auth.AuthenticationService;
import com.secureshare.securefiles.auth.RegisterRequest;
import com.secureshare.securefiles.config.JwtService;
import com.secureshare.securefiles.token.Token;
import com.secureshare.securefiles.token.TokenRepository;
import com.secureshare.securefiles.token.TokenType;
import com.secureshare.securefiles.user.Role;
import com.secureshare.securefiles.user.User;
import com.secureshare.securefiles.user.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegister_ShouldReturnAuthResponse() {
        // Arrange
        RegisterRequest request = RegisterRequest.builder()
                .firstname("John")
                .lastname("Doe")
                .email("john@example.com")
                .password("password123")
                .build();

        User savedUser = User.builder()
                .firstname("John")
                .lastname("Doe")
                .email("john@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(User.class))).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh-token");

        // Act
        AuthenticationResponse response = authenticationService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(tokenRepository, times(1)).save(any(Token.class));
    }

    @Test
    void testAuthenticate_ShouldReturnAuthResponse() {
        // Arrange
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        User user = User.builder()
                .email("john@example.com")
                .password("encodedPassword")
                .role(Role.USER)
                .build();

        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("access-token");
        when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

        // Act
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("refresh-token", response.getRefreshToken());
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(tokenRepository, times(1)).save(any(Token.class));
    }
}
