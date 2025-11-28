package com.acc.service;

import com.acc.dto.UserDTO;
import com.acc.entity.User; // Import your User entity

import org.springframework.security.core.userdetails.UserDetails; // Keep if loadUserByUsername is here

import java.util.List;
import java.util.Set;

public interface UserService {
    UserDTO registerUser(UserDTO userDto);
    UserDTO createUser(UserDTO userDTO); 
    UserDTO getUserById(Long id);
    UserDTO getUserByUsername(String username);
    List<UserDTO> getAllUsers();
    UserDTO updateUser(Long id, UserDTO userDTO);
    void deleteUser(Long id);
    User save(UserDTO userDto); 
    UserDTO convertToDTO(User user);
	UserDetails loadUserByUsername(String userName); 
	 UserDTO updateUserRoles(String identifier, Set<String> newRoleNames);
}