package com.acc.serviceImpl;

import com.acc.dto.UserDTO;
import com.acc.entity.Admin;
import com.acc.entity.Customer;
import com.acc.entity.Role;
import com.acc.entity.User;
import com.acc.repository.AdminRepository;
import com.acc.repository.RoleRepository;
import com.acc.repository.UserRepository;
import com.acc.service.UserService;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Primary
public class UserServiceImpl implements UserService, UserDetailsService {

    @Autowired private UserRepository userRepository;
    @Autowired private AdminRepository adminRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setRole(user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse("UNKNOWN_ROLE")
                .replace("ROLE_", ""));
        return dto;
    }

    private UserDetails buildUserDetails(String username, String password, Set<Role> roles) {
        List<SimpleGrantedAuthority> authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());

        return org.springframework.security.core.userdetails.User.builder()
                .username(username)
                .password(password)
                .authorities(authorities)
                .build();
    }

    @Override
    @Transactional
    public UserDTO registerUser(UserDTO userDto) {
        if (userRepository.findByUsername(userDto.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
        }

        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + userDto.getEmail());
        }

        User user = createUserEntityFromDTO(userDto);
        User savedUser = userRepository.save(user);
        return convertToDTO(savedUser);
    }

    @Override
    public UserDTO createUser(UserDTO userDTO) {
        return registerUser(userDTO);
    }

    @Override
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));
        return convertToDTO(user);
    }

    @Override
    public UserDTO getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return convertToDTO(user);
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UserDTO updateUser(Long id, UserDTO userDTO) {
        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with ID: " + id));

        if (userDTO.getUsername() != null) {
            existingUser.setUsername(userDTO.getUsername());
        }

        if (userDTO.getEmail() != null) {
            existingUser.setEmail(userDTO.getEmail());
        }

        if (userDTO.getPassword() != null) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        if (userDTO.getRole() != null) {
            Role newRole = roleRepository.findByName("ROLE_" + userDTO.getRole().toUpperCase())
                    .orElseThrow(() -> new IllegalArgumentException("Role not found"));
            existingUser.setRoles(Set.of(newRole));
        }

        return convertToDTO(userRepository.save(existingUser));
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UsernameNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> userOptional = userRepository.findByUsernameWithRoles(username);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return buildUserDetails(user.getUsername(), user.getPassword(), user.getRoles());
        }

        Optional<Admin> adminOptional = adminRepository.findByUsername(username);
        if (adminOptional.isPresent()) {
            Admin admin = adminOptional.get();
            return buildUserDetails(admin.getUsername(), admin.getPassword(), admin.getRoles());
        }

        throw new UsernameNotFoundException("User or Admin not found: " + username);
    }

    @Override
    @Transactional
    public User save(UserDTO userDto) {
        User user = createUserEntityFromDTO(userDto);
        return userRepository.save(user);
    }

    private User createUserEntityFromDTO(UserDTO userDto) {
        String roleName = "ROLE_" + (userDto.getRole() != null ? userDto.getRole().toUpperCase() : "USER");
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));

        User user;
        switch (roleName) {
            case "ROLE_ADMIN": user = new Admin(); break;
            case "ROLE_CUSTOMER": user = new Customer(); break;
            default: user = new User(); break;
        }

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        user.setRoles(Collections.singleton(role));
        return user;
    }
}
