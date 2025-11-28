package com.acc.serviceImpl;

import com.acc.dto.UserDTO;
import com.acc.entity.*;
import com.acc.repository.AdminRepository;
import com.acc.repository.CustomerRepository;
import com.acc.repository.RoleRepository;
import com.acc.repository.SuperAdminRepository;
import com.acc.repository.UserRepository;
import com.acc.service.UserService;

import jakarta.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    
    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private SuperAdminRepository superAdminRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

  
    private String getFullRoleName(String roleName) {
        String upperRoleName = roleName.toUpperCase();
       
        return upperRoleName.startsWith("ROLE_") 
                ? upperRoleName 
                : "ROLE_" + upperRoleName;
    }
    @Override
    public UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());

        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            dto.setRoles(user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet()));
        } else {
            dto.setRoles(Collections.emptySet());
        }
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
            log.warn("Username already exists: {}", userDto.getUsername());
            throw new IllegalArgumentException("Username already exists: " + userDto.getUsername());
        }

        if (userRepository.findByEmail(userDto.getEmail()).isPresent()) {
            log.warn("Email already registered: {}", userDto.getEmail());
            throw new IllegalArgumentException("Email already registered: " + userDto.getEmail());
        }

        User user = createUserEntityFromDTO(userDto);
        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
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

        if (userDTO.getUsername() != null && !userDTO.getUsername().equals(existingUser.getUsername())) {
            Optional<User> userWithSameUsername = userRepository.findByUsername(userDTO.getUsername());
            if (userWithSameUsername.isPresent() && !userWithSameUsername.get().getId().equals(id)) {
                log.warn("Attempted to update username to an existing one: {}", userDTO.getUsername());
                throw new IllegalArgumentException("Username already exists: " + userDTO.getUsername());
            }
            existingUser.setUsername(userDTO.getUsername());
        }

        if (userDTO.getEmail() != null && !userDTO.getEmail().equals(existingUser.getEmail())) {
            Optional<User> userWithSameEmail = userRepository.findByEmail(userDTO.getEmail());
            if (userWithSameEmail.isPresent() && !userWithSameEmail.get().getId().equals(id)) {
                log.warn("Attempted to update email to an existing one: {}", userDTO.getEmail());
                throw new IllegalArgumentException("Email already registered: " + userDTO.getEmail());
            }
            existingUser.setEmail(userDTO.getEmail());
        }

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty()) {
            existingUser.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        }

        if (userDTO.getRoles() != null && !userDTO.getRoles().isEmpty()) {
            
            Set<Role> newRoles = userDTO.getRoles().stream()
                    .map(roleName -> {
                        String fullRoleName = getFullRoleName(roleName);
                        return roleRepository.findByName(fullRoleName)
                                .orElseThrow(() -> {
                                    log.error("Role not found during update: {}", roleName);
                                    return new IllegalArgumentException("Role not found: " + roleName);
                                });
                    })
                    .collect(Collectors.toSet());
           
            existingUser.setRoles(newRoles);
        }

        User updatedUser = userRepository.save(existingUser);
        log.info("User updated successfully: {}", updatedUser.getUsername());
        return convertToDTO(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            log.warn("Attempted to delete a non-existent user with ID: {}", id);
            throw new UsernameNotFoundException("User not found with ID: " + id);
        }
        userRepository.deleteById(id);
        log.info("User with ID {} deleted successfully.", id);
    }

    @Override
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        log.debug("Attempting to load user with identifier (username/email): {}", identifier);

        Optional<User> userOptionalByEmail = userRepository.findByEmail(identifier);
        if (userOptionalByEmail.isPresent()) {
            User user = userOptionalByEmail.get();
            log.debug("Found User by email: {} (ID: {})", identifier, user.getId());
            return buildUserDetails(user.getEmail(), user.getPassword(), user.getRoles());
        }

        Optional<User> userOptionalByUsername = userRepository.findByUsernameWithRoles(identifier);
        if (userOptionalByUsername.isPresent()) {
            User user = userOptionalByUsername.get();
            log.debug("Found User by username: {} (ID: {})", identifier, user.getId());
            return buildUserDetails(user.getUsername(), user.getPassword(), user.getRoles());
        }

        Optional<Admin> adminOptionalByEmail = adminRepository.findByEmail(identifier);
        if (adminOptionalByEmail.isPresent()) {
            Admin admin = adminOptionalByEmail.get();
            log.debug("Found Admin by email: {}", identifier);
            return buildUserDetails(admin.getEmail(), admin.getPassword(), admin.getRoles());
        }

        Optional<Admin> adminOptionalByUsername = adminRepository.findByUsername(identifier);
        if (adminOptionalByUsername.isPresent()) {
            Admin admin = adminOptionalByUsername.get();
            log.debug("Found Admin by username: {}", identifier);
            return buildUserDetails(admin.getUsername(), admin.getPassword(), admin.getRoles());
        }

        log.warn("User or Admin not found for identifier: {}", identifier);
        throw new UsernameNotFoundException("User or Admin not found: " + identifier);
    }

    @Override
    @Transactional
    public User save(UserDTO userDto) {
        User user = createUserEntityFromDTO(userDto);
        User savedUser = userRepository.save(user);
        log.info("User saved: {}", savedUser.getUsername());
        return savedUser;
    }

    private User createUserEntityFromDTO(UserDTO userDto) {
        Set<Role> assignedRoles = new HashSet<>();

        if (userDto.getRoles() != null && !userDto.getRoles().isEmpty()) {
         
            assignedRoles = userDto.getRoles().stream()
                    .map(roleName -> {
                        String fullRoleName = getFullRoleName(roleName);
                        return roleRepository.findByName(fullRoleName)
                                .orElseThrow(() -> {
                                    log.error("Role not found during user creation: {}", roleName);
                                    return new IllegalArgumentException("Role not found: " + roleName);
                                });
                    })
                    .collect(Collectors.toSet());
           
        } else {
            Role defaultCustomerRole = roleRepository.findByName("ROLE_CUSTOMER")
                    .orElseThrow(() -> {
                        log.error("Default CUSTOMER role not found.");
                        return new IllegalArgumentException("Default CUSTOMER role not found.");
                    });
            assignedRoles.add(defaultCustomerRole);
        }

        User user;
        if (assignedRoles.stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"))) {
            user = new SuperAdmin();
        } else if (assignedRoles.stream().anyMatch(r -> r.getName().equals("ROLE_ADMIN"))) {
            user = new Admin();
        } else if (assignedRoles.stream().anyMatch(r -> r.getName().equals("ROLE_CUSTOMER"))) {
            user = new Customer();
        } else {
            user = new User();
        }

        user.setUsername(userDto.getUsername());
        user.setEmail(userDto.getEmail());
        user.setPassword(userDto.getPassword() != null && !userDto.getPassword().isBlank()
                ? passwordEncoder.encode(userDto.getPassword())
                : "");
        user.setRoles(assignedRoles);
        log.debug("Created user entity from DTO: {}", userDto.getUsername());
        return user;
    }

    @Override
    @Transactional
    public UserDTO updateUserRoles(String identifier, Set<String> newRoleNames) {
        log.debug("Starting updateUserRoles for identifier: {}", identifier);
        log.debug("New role names requested: {}", newRoleNames);

        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByUsername(identifier)
                        .orElseThrow(() -> {
                            log.error("User not found for role update with identifier: {}", identifier);
                            return new UsernameNotFoundException("User not found with identifier: " + identifier);
                        }));
        log.debug("Found user: {} (ID: {})", user.getUsername(), user.getId());
        log.debug("User's current roles: {}", user.getRoles().stream().map(Role::getName).collect(Collectors.joining(", ")));

        
        if (user.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_SUPER_ADMIN"))) {
            log.error("Attempted to modify roles of a Super Admin. Operation denied.");
            throw new IllegalArgumentException("Cannot modify roles of a Super Admin through this interface.");
        }

        // Security check: Deny assignment of Super Admin role through this method
//        if (newRoleNames.stream().anyMatch(r -> r.equalsIgnoreCase("ROLE_SUPER_ADMIN"))) {
//            log.error("Attempted to assign ROLE_SUPER_ADMIN. Operation denied.");
//            throw new IllegalArgumentException("Cannot assign ROLE_SUPER_ADMIN role.");
//        }

        
        Set<Role> rolesToAssign = newRoleNames.stream()
                .map(roleName -> {
                    String fullRoleName = getFullRoleName(roleName);
                    log.debug("Looking up role: {}", fullRoleName);
                    return roleRepository.findByName(fullRoleName)
                            .orElseThrow(() -> {
                                log.error("Role not found in database: {}", fullRoleName);
                                return new IllegalArgumentException("Role not found: " + fullRoleName);
                            });
                })
                .collect(Collectors.toSet());
       

        log.debug("Roles to assign (entities): {}", rolesToAssign.stream().map(Role::getName).collect(Collectors.joining(", ")));

        user.setRoles(rolesToAssign);
        log.debug("User entity roles updated in memory.");

        User updatedUser = userRepository.save(user);
        log.info("User saved to repository. ID: {}", updatedUser.getId());

        UserDTO resultDTO = convertToDTO(updatedUser);
        log.debug("Converted updated user to DTO. DTO roles: {}", resultDTO.getRoles());
        log.debug("Finished updateUserRoles for identifier: {}", identifier);

        return resultDTO;
    }
}