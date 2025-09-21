package com.williams.bulktransactionservice.controller;

import com.williams.bulktransactionservice.constant.AppConstant;
import com.williams.bulktransactionservice.model.entity.User;
import com.williams.bulktransactionservice.model.request.UserRequest;
import com.williams.bulktransactionservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping(AppConstant.APP_CONTENT+"/auth")
public class AuthController {

    private static final Logger logger =
            LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public User registerUser(@RequestBody UserRequest userRequest) {
        logger.info("Registering new user request: {} " , userRequest);
        return userService.registerUser(userRequest);
    }


    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> body) {
        logger.info("User login attempt: {}", body.get("username"));
        return userService.login(body.get("username"), body.get("password"));
    }
}
