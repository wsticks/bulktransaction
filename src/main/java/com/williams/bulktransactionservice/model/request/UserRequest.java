package com.williams.bulktransactionservice.model.request;

import com.williams.bulktransactionservice.model.entity.Role;
import lombok.Data;
import java.util.Set;

@Data
public class UserRequest {

    private String username;
    private String password;
    private Set<Role> roles;
}
