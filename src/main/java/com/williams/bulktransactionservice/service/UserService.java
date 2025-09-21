package com.williams.bulktransactionservice.service;

import com.williams.bulktransactionservice.model.entity.User;
import com.williams.bulktransactionservice.model.request.UserRequest;
import com.williams.bulktransactionservice.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;
    private final AuthService authService;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, CustomUserDetailsService userDetailsService, AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.authService = authService;
    }


    public User registerUser(@RequestBody UserRequest userRequest) {
        Optional<User> userExists = userRepository.findByUsername(userRequest.getUsername());
        if (userExists.isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        // Create new user
        User newUser = new User();
        newUser.setUsername(userRequest.getUsername());
        newUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
        newUser.setRoles(userRequest.getRoles());

        return userRepository.save(newUser);
    }

    public Map<String, Object> login(String username, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, password)
        );
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        String token = authService.generateTokenWithExpiration(userDetails.getUsername()).get("token").toString();
        Date expiration = authService.extractExpiration(token);

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiresAt", expiration);
        response.put("username", username);

        return response;
    }
}
