package com.marsreg.common.service;

import com.marsreg.common.entity.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Optional;

public interface UserService extends UserDetailsService {
    User createUser(User user);
    Optional<User> getUserById(Long id);
    Optional<User> getUserByUsername(String username);
    Optional<User> getUserByEmail(String email);
    User updateUser(User user);
    void deleteUser(Long id);
    void changePassword(Long userId, String oldPassword, String newPassword);
    void updateLastLogin(String username);
} 