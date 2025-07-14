package com.acc.service;

import java.util.List;

import org.springframework.security.core.userdetails.UserDetailsService;

import com.acc.dto.UserDTO;
import com.acc.entity.User;

public interface UserService extends UserDetailsService {
    UserDTO registerUser(UserDTO userDto);
    UserDTO createUser(UserDTO userDTO);
    UserDTO getUserById(Long id);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    UserDTO getUserByUsername(String username);
    User save(UserDTO userDto);
}
