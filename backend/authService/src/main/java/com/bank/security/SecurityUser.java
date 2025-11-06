package com.bank.security;

import com.bank.entity.model.Role;
import com.bank.entity.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class SecurityUser {
    public static List<GrantedAuthority> authorities(User u){
        Role r = u.getRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + r.name()));
    }
}
