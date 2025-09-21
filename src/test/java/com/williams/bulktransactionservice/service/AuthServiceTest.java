package com.williams.bulktransactionservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp()  {
        ReflectionTestUtils.setField(authService, "secret",
                "Z3Vlc3RzZWNyZXRrZXltdXN0YmUyNTZiaXRzZm9ySFM=");
        ReflectionTestUtils.setField(authService, "expirationMs", 3600000L);
    }

    @Test
    void testGenerateTokenWithExpiration() {
        Map<String, Object> tokenMap = authService.generateTokenWithExpiration("testuser");
        assertNotNull(tokenMap.get("token"));
        assertNotNull(tokenMap.get("expiresAt"));
    }

    @Test
    void testExtractUsernameAndExpiration() {
        Map<String, Object> tokenMap = authService.generateTokenWithExpiration("testuser");
        String token = (String) tokenMap.get("token");
        String username = authService.extractUsername(token);
        Date expiration = authService.extractExpiration(token);
        assertEquals("testuser", username);
        assertTrue(expiration.getTime() > new Date().getTime());
    }

    @Test
    void testIsTokenValid() {
        Map<String, Object> tokenMap = authService.generateTokenWithExpiration("validuser");
        String token = (String) tokenMap.get("token");
        org.springframework.security.core.userdetails.UserDetails userDetails =
                Mockito.mock(org.springframework.security.core.userdetails.UserDetails.class);
        Mockito.when(userDetails.getUsername()).thenReturn("validuser");

        boolean isValid = authService.isTokenValid(token, userDetails);
        assertTrue(isValid);
        Mockito.when(userDetails.getUsername()).thenReturn("wronguser");
        boolean isInvalid = authService.isTokenValid(token, userDetails);
        assertFalse(isInvalid);
    }
}
