package com.acc.serviceImpl;

import com.acc.dto.AdminDTO;
import com.acc.dto.UserDTO;
import com.acc.dto.AuthResponseDTO;
import com.acc.entity.Admin;
import com.acc.entity.Role;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.AdminRepository;
import com.acc.repository.RoleRepository;
import com.acc.service.AdminService;
import com.acc.config.JwtUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    @Transactional
    public AdminDTO registerAdmin(AdminDTO adminDto) {
        log.info("Attempting to register new admin with username: {}", adminDto.getUserName());
        if (adminRepository.findByUsername(adminDto.getUserName()).isPresent()) {
            log.warn("Registration failed: Username already exists: {}", adminDto.getUserName());
            throw new IllegalArgumentException("Username already exists: " + adminDto.getUserName());
        }

        if (adminRepository.findByEmail(adminDto.getEmail()).isPresent()) {
            log.warn("Registration failed: Email already registered: {}", adminDto.getEmail());
            throw new IllegalArgumentException("Email already registered: " + adminDto.getEmail());
        }

        Admin admin = new Admin();
        admin.setUsername(adminDto.getUserName());
        admin.setEmail(adminDto.getEmail());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));
        log.debug("Encoded password for admin: {}", adminDto.getUserName());

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> {
                    log.error("Role 'ROLE_ADMIN' not found in database.");
                    return new ResourceNotFoundException("Role", "name", "ROLE_ADMIN");
                });

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);
        log.debug("Assigned role 'ROLE_ADMIN' to new admin.");

        Admin savedAdmin = adminRepository.save(admin);
        log.info("New admin registered successfully with ID: {}", savedAdmin.getId());
        return mapAdminToDTO(savedAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDTO getAdminById(Long id) {
        log.info("Fetching admin by ID: {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Admin not found with ID: {}", id);
                    return new ResourceNotFoundException("Admin", "Id", id);
                });
        log.debug("Admin found with ID: {}", id);
        return mapAdminToDTO(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDTO> getAllAdmins() {
        log.info("Fetching all admins.");
        List<AdminDTO> admins = adminRepository.findAll().stream()
                .map(this::mapAdminToDTO)
                .collect(Collectors.toList());
        log.info("Found {} admins.", admins.size());
        return admins;
    }

    @Override
    @Transactional
    public AdminDTO updateAdmin(Long id, AdminDTO adminDto) {
        log.info("Attempting to update admin with ID: {}", id);
        Admin existingAdmin = adminRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Admin not found with ID: {}", id);
                    return new ResourceNotFoundException("Admin", "Id", id);
                });

        if (adminDto.getUserName() != null && !adminDto.getUserName().equals(existingAdmin.getUsername())) {
            log.debug("Checking for username uniqueness for new username: {}", adminDto.getUserName());
            if (adminRepository.findByUsername(adminDto.getUserName()).isPresent()) {
                log.warn("Update failed: Username already exists: {}", adminDto.getUserName());
                throw new IllegalArgumentException("Username already exists: " + adminDto.getUserName());
            }
            existingAdmin.setUsername(adminDto.getUserName());
        }

        if (adminDto.getEmail() != null && !adminDto.getEmail().equals(existingAdmin.getEmail())) {
            log.debug("Checking for email uniqueness for new email: {}", adminDto.getEmail());
            if (adminRepository.findByEmail(adminDto.getEmail()).isPresent()) {
                log.warn("Update failed: Email already registered: {}", adminDto.getEmail());
                throw new IllegalArgumentException("Email already registered: " + adminDto.getEmail());
            }
            existingAdmin.setEmail(adminDto.getEmail());
        }

        Admin updatedAdmin = adminRepository.save(existingAdmin);
        log.info("Admin with ID: {} updated successfully.", updatedAdmin.getId());
        return mapAdminToDTO(updatedAdmin);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id) {
        log.info("Attempting to delete admin with ID: {}", id);
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Deletion failed: Admin not found with ID: {}", id);
                    return new ResourceNotFoundException("Admin", "Id", id);
                });
        adminRepository.delete(admin);
        log.info("Admin with ID: {} deleted successfully.", id);
    }

    @Override
    @Transactional
    public AuthResponseDTO loginAdmin(UserDTO userDto) {
        log.info("Attempting admin login for username: {}", userDto.getUsername());
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDto.getUsername(), userDto.getPassword()));
        log.debug("Authentication successful for admin: {}", userDto.getUsername());

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtil.generateToken(userDetails);
        log.info("JWT token generated for admin: {}", userDto.getUsername());

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(jwtToken);
        response.setMessage("Admin login successful!");
        response.setRole("ADMIN");
        response.setUser(userDto);
        return response;
    }

    private AdminDTO mapAdminToDTO(Admin admin) {
        log.debug("Mapping Admin entity with ID {} to DTO.", admin.getId());
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setId(admin.getId());
        adminDTO.setUserName(admin.getUsername());
        adminDTO.setEmail(admin.getEmail());
        return adminDTO;
    }
}