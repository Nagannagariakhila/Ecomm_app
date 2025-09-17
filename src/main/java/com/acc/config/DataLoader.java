package com.acc.config;

import com.acc.entity.Role;
import com.acc.entity.SuperAdmin; // Import SuperAdmin entity
import com.acc.entity.User; // User is the base class for SuperAdmin
import com.acc.repository.RoleRepository;
import com.acc.repository.SuperAdminRepository; // Import SuperAdminRepository
import com.acc.repository.UserRepository; // Used for general user checks, including username
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.Optional; // Import Optional for clarity

@Component
public class DataLoader implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository; 
    private final SuperAdminRepository superAdminRepository;
    private final PasswordEncoder passwordEncoder;

    public DataLoader(RoleRepository roleRepository, UserRepository userRepository,
                      SuperAdminRepository superAdminRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.superAdminRepository = superAdminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
       
        final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
        final String ROLE_ADMIN = "ROLE_ADMIN";
        final String ROLE_USER = "ROLE_USER";
        final String ROLE_CUSTOMER = "ROLE_CUSTOMER";

       
        createRoleIfNotFound(ROLE_SUPER_ADMIN);
        createRoleIfNotFound(ROLE_ADMIN);
        createRoleIfNotFound(ROLE_USER);
        createRoleIfNotFound(ROLE_CUSTOMER);

        
        String superAdminEmail = "superadmin@example.com";
        String superAdminUsername = "superadmin";
        String superAdminPassword = "superadminpass"; 

        
        Optional<User> existingUser = userRepository.findByUsernameWithRoles(superAdminUsername);

        if (existingUser.isEmpty()) {
            System.out.println("Creating new Super Admin user...");

           
            Role superAdminRole = roleRepository.findByName(ROLE_SUPER_ADMIN)
                    .orElseThrow(() -> new RuntimeException("Error: SUPER_ADMIN Role not found after creation attempt. Please ensure roles are initialized correctly."));

            Set<Role> roles = new HashSet<>();
            roles.add(superAdminRole);

           
            SuperAdmin superAdmin = new SuperAdmin();
            superAdmin.setUsername(superAdminUsername);
            superAdmin.setEmail(superAdminEmail);
            superAdmin.setPassword(passwordEncoder.encode(superAdminPassword)); 
            superAdmin.setRoles(roles);

            
            superAdminRepository.save(superAdmin);
            System.out.println("Default Super Admin user created: Username: " + superAdminUsername + ", Email: " + superAdminEmail);
        } else {
            System.out.println("Super Admin user already exists with username: " + superAdminUsername);
           
        }
    }

    
    private void createRoleIfNotFound(String roleName) {
        if (roleRepository.findByName(roleName).isEmpty()) {
            roleRepository.save(new Role(roleName));
            System.out.println("Role created: " + roleName);
        } else {
            System.out.println("Role already exists: " + roleName);
        }
    }
}
