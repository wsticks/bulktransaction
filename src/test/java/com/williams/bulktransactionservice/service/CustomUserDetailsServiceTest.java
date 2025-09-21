package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.entity.Role;
import com.williams.bulktransactionservice.model.entity.User;
import com.williams.bulktransactionservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        // Arrange
        User user = new User();
        user.setUsername("john");
        user.setPassword("password123");
        user.setRoles(Set.of(Role.ROLE_ADMIN, Role.ROLE_USER));

        when(userRepository.findByUsername("john")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = customUserDetailsService.loadUserByUsername("john");

        // Assert
        assertNotNull(userDetails);
        assertEquals("john", userDetails.getUsername());
        assertEquals("password123", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")));
        assertTrue(userDetails.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));

        verify(userRepository, times(1)).findByUsername("john");
    }

    @Test
    void loadUserByUsername_UserDoesNotExist_ThrowsException() {
        // Arrange
        when(userRepository.findByUsername("jane")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () ->
                customUserDetailsService.loadUserByUsername("jane")
        );
        assertEquals("User not found: jane", exception.getMessage());

        verify(userRepository, times(1)).findByUsername("jane");
    }
}
