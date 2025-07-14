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
        if (adminRepository.findByUsername(adminDto.getUserName()).isPresent()) {
            throw new IllegalArgumentException("Username already exists: " + adminDto.getUserName());
        }

        if (adminRepository.findByEmail(adminDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already registered: " + adminDto.getEmail());
        }

        Admin admin = new Admin();
        admin.setUsername(adminDto.getUserName());
        admin.setEmail(adminDto.getEmail());
        admin.setPassword(passwordEncoder.encode(adminDto.getPassword()));

        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", "ROLE_ADMIN"));

        Set<Role> roles = new HashSet<>();
        roles.add(adminRole);
        admin.setRoles(roles);

        Admin savedAdmin = adminRepository.save(admin);
        return mapAdminToDTO(savedAdmin);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDTO getAdminById(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "Id", id));
        return mapAdminToDTO(admin);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDTO> getAllAdmins() {
        return adminRepository.findAll().stream()
                .map(this::mapAdminToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AdminDTO updateAdmin(Long id, AdminDTO adminDto) {
        Admin existingAdmin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "Id", id));

        if (adminDto.getUserName() != null && !adminDto.getUserName().equals(existingAdmin.getUsername())) {
            if (adminRepository.findByUsername(adminDto.getUserName()).isPresent()) {
                throw new IllegalArgumentException("Username already exists: " + adminDto.getUserName());
            }
            existingAdmin.setUsername(adminDto.getUserName());
        }

        if (adminDto.getEmail() != null && !adminDto.getEmail().equals(existingAdmin.getEmail())) {
            if (adminRepository.findByEmail(adminDto.getEmail()).isPresent()) {
                throw new IllegalArgumentException("Email already registered: " + adminDto.getEmail());
            }
            existingAdmin.setEmail(adminDto.getEmail());
        }

        Admin updatedAdmin = adminRepository.save(existingAdmin);
        return mapAdminToDTO(updatedAdmin);
    }

    @Override
    @Transactional
    public void deleteAdmin(Long id) {
        Admin admin = adminRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Admin", "Id", id));
        adminRepository.delete(admin);
    }

    @Override
    @Transactional
    public AuthResponseDTO loginAdmin(UserDTO userDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDto.getUsername(), userDto.getPassword()));

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String jwtToken = jwtUtil.generateToken(userDetails);

        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(jwtToken);
        response.setMessage("Admin login successful!");
        response.setRole("ADMIN");
        response.setUser(userDto);
        return response;
    }

    private AdminDTO mapAdminToDTO(Admin admin) {
        AdminDTO adminDTO = new AdminDTO();
        adminDTO.setId(admin.getId());
        adminDTO.setUserName(admin.getUsername());
        adminDTO.setEmail(admin.getEmail());
        return adminDTO;
    }
}
