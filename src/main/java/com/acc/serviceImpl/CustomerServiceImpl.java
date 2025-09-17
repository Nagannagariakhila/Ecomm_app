package com.acc.serviceImpl;

import com.acc.config.JwtUtil;
import com.acc.dto.*;
import com.acc.entity.*;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.*;
import com.acc.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    private static final Logger log = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomerCodeGenerator customerCodeGenerator;

    /**
     * Helper method to save the customer in a separate transaction and get the ID.
     * This ensures the ID is available for the main transaction.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Customer generateAndSaveCustomerCode(Customer customer) {
        log.debug("Entering generateAndSaveCustomerCode method in a new transaction.");
        Customer savedCustomer = customerRepository.save(customer);
        if (savedCustomer.getId() == null) {
            log.error("Failed to generate customer ID during initial save.");
            throw new IllegalStateException("Failed to generate customer ID during initial save.");
        }
        String customerCode = customerCodeGenerator.generateCode(savedCustomer.getId());
        savedCustomer.setCustomerCode(customerCode);
        Customer customerWithCode = customerRepository.save(savedCustomer);
        log.info("Generated and saved customer code '{}' for new customer with ID: {}", customerCode, customerWithCode.getId());
        return customerWithCode;
    }
    
    @Override
    public CustomerDTO saveCustomer(CustomerDTO customerDto) {
        log.info("Attempting to save a new customer with username: {}", customerDto.getUserDetails().getUsername());
        UserDTO userDetails = customerDto.getUserDetails();
        if (userDetails == null) {
            log.error("User details are missing for customer creation.");
            throw new IllegalArgumentException("User details required");
        }

        userRepository.findByUsername(userDetails.getUsername())
                      .ifPresent(u -> {
                          log.warn("Username '{}' already exists.", userDetails.getUsername());
                          throw new IllegalArgumentException("Username already exists");
                      });

        userRepository.findByEmail(userDetails.getEmail())
                      .ifPresent(u -> {
                          log.warn("Email '{}' already exists.", userDetails.getEmail());
                          throw new IllegalArgumentException("Email already exists");
                      });

        Customer customer = new Customer();
        customer.setUsername(userDetails.getUsername());
        customer.setEmail(userDetails.getEmail());
        customer.setPassword(passwordEncoder.encode(userDetails.getPassword()));
        log.debug("Encoded password for user: {}", userDetails.getUsername());

        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                                  .orElseThrow(() -> {
                                      log.error("Role 'ROLE_CUSTOMER' not found in database.");
                                      return new ResourceNotFoundException("Role", "name", "ROLE_CUSTOMER");
                                  });
        customer.setRoles(Set.of(role));
        log.debug("Assigned role 'ROLE_CUSTOMER' to customer.");

        customer = generateAndSaveCustomerCode(customer);

        ProfileDTO profileDTO = customerDto.getProfileDetails();
        if (profileDTO != null) {
            log.debug("Mapping and setting profile details for customer ID: {}", customer.getId());
            Profile profile = new Profile();
            profile.setFirstName(profileDTO.getFirstName());
            profile.setLastName(profileDTO.getLastName());
            profile.setPhoneNumber(profileDTO.getPhoneNumber());
            profile.setEmail(profileDTO.getEmail() != null ? profileDTO.getEmail() : customer.getEmail());

            profile.setCustomer(customer);
            customer.setProfile(profile);

            if (profileDTO.getAddresses() != null) {
                log.debug("Mapping and setting {} addresses for customer ID: {}", profileDTO.getAddresses().size(), customer.getId());
                for (AddressDTO aDto : profileDTO.getAddresses()) {
                    Address address = mapAddressToEntity(aDto);
                    profile.addAddress(address);
                }
            }
        }
        
        customer = customerRepository.save(customer);
        log.info("Customer with ID: {} and code: {} saved successfully.", customer.getId(), customer.getCustomerCode());

        return mapCustomerToDTO(customer);
    }

    @Override
    public AuthResponseDTO loginCustomer(UserDTO userDto) {
        log.info("Attempting customer login for email: {}", userDto.getEmail());
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userDto.getEmail(), userDto.getPassword()));
        String token = jwtUtil.generateToken((UserDetails) auth.getPrincipal());
        log.info("Customer '{}' logged in successfully. JWT token generated.", userDto.getEmail());
        return new AuthResponseDTO(token, "Customer login successful!");
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        log.info("Fetching customer by ID: {}", id);
        return mapCustomerToDTO(customerRepository.findById(id)
                                                  .orElseThrow(() -> {
                                                      log.error("Customer not found with ID: {}", id);
                                                      return new ResourceNotFoundException("Customer", "Id", id);
                                                  }));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByEmail(String email) {
        log.info("Fetching customer by email: {}", email);
        return mapCustomerToDTO(customerRepository.findByEmail(email)
                                                  .orElseThrow(() -> {
                                                      log.error("Customer not found with email: {}", email);
                                                      return new ResourceNotFoundException("Customer", "Email", email);
                                                  }));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        log.info("Fetching all customers.");
        List<CustomerDTO> customers = customerRepository.findAll()
                                 .stream()
                                 .map(this::mapCustomerToDTO)
                                 .collect(Collectors.toList());
        log.info("Found {} customers.", customers.size());
        return customers;
    }

    @Override
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto) {
        log.info("Attempting to update customer with ID: {}", id);
        Customer customer = customerRepository.findById(id)
                                              .orElseThrow(() -> {
                                                  log.error("Customer not found with ID: {}", id);
                                                  return new ResourceNotFoundException("Customer", "Id", id);
                                              });

        UserDTO u = dto.getUserDetails();
        if (u != null) {
            log.debug("Updating user details for customer ID: {}", id);
            if (!customer.getUsername().equals(u.getUsername()) && userRepository.findByUsername(u.getUsername()).isPresent()) {
                log.warn("Attempted to update username to '{}', but it already exists.", u.getUsername());
                throw new IllegalArgumentException("Username already exists: " + u.getUsername());
            }

            if (!customer.getEmail().equals(u.getEmail()) && userRepository.findByEmail(u.getEmail()).isPresent()) {
                log.warn("Attempted to update email to '{}', but it is already registered.", u.getEmail());
                throw new IllegalArgumentException("Email already registered: " + u.getEmail());
            }

            customer.setUsername(u.getUsername());
            customer.setEmail(u.getEmail());
        }

        ProfileDTO pDto = dto.getProfileDetails();
        if (pDto != null) {
            log.debug("Updating profile details for customer ID: {}", id);
            Profile profile = customer.getProfile();
            if (profile == null) {
                log.debug("Profile not found for customer ID: {}. Creating a new one.", id);
                profile = new Profile();
                profile.setCustomer(customer);
                customer.setProfile(profile);
            }

            profile.setFirstName(pDto.getFirstName());
            profile.setLastName(pDto.getLastName());
            profile.setPhoneNumber(pDto.getPhoneNumber());
            profile.setEmail(pDto.getEmail() != null ? pDto.getEmail() : customer.getEmail());

            if (pDto.getAddresses() != null) {
                log.debug("Updating addresses for profile ID: {}", profile.getId());
                profile.setAddresses(pDto.getAddresses().stream()
                                         .map(this::mapAddressToEntity)
                                         .collect(Collectors.toList()));
            }

            profileRepository.save(profile);
        }

        Customer updatedCustomer = customerRepository.save(customer);
        log.info("Customer with ID {} updated successfully.", updatedCustomer.getId());
        return mapCustomerToDTO(updatedCustomer);
    }

    @Override
    public void deleteCustomer(Long id) {
        log.info("Attempting to delete customer with ID: {}", id);
        Customer c = customerRepository.findById(id)
                                       .orElseThrow(() -> {
                                           log.error("Customer not found with ID: {}", id);
                                           return new ResourceNotFoundException("Customer", "Id", id);
                                       });
        customerRepository.delete(c);
        log.info("Customer with ID {} deleted successfully.", id);
    }

    private CustomerDTO mapCustomerToDTO(Customer c) {
        log.debug("Mapping Customer entity with ID {} to DTO.", c.getId());
        CustomerDTO dto = new CustomerDTO();

        UserDTO u = new UserDTO();
        u.setId(c.getId());
        u.setUsername(c.getUsername());
        u.setEmail(c.getEmail());
        c.getRoles().stream().findFirst()
         .ifPresent(r -> u.setRole(r.getName().replace("ROLE_", "")));
        dto.setUserDetails(u);
        dto.setCustomerCode(c.getCustomerCode());

        Profile profile = c.getProfile();
        if (profile != null) {
            ProfileDTO profileDTO = new ProfileDTO();
            profileDTO.setId(profile.getId());
            profileDTO.setFirstName(profile.getFirstName());
            profileDTO.setLastName(profile.getLastName());
            profileDTO.setPhoneNumber(profile.getPhoneNumber());
            profileDTO.setEmail(profile.getEmail());
            profileDTO.setCustomerId(c.getId());

            if (profile.getAddresses() != null && !profile.getAddresses().isEmpty()) {
                List<AddressDTO> addressDTOs = profile.getAddresses().stream().map(address -> {
                    AddressDTO a = new AddressDTO();
                    a.setId(address.getId());
                    a.setStreet(address.getStreet());
                    a.setCity(address.getCity());
                    a.setState(address.getState());
                    a.setCountry(address.getCountry());
                    a.setZipCode(address.getZipCode());
                    a.setType(address.getType());
                    a.setProfileId(profile.getId());
                    return a;
                }).collect(Collectors.toList());
                profileDTO.setAddresses(addressDTOs);
            }

            dto.setProfileDetails(profileDTO);
        }
        return dto;
    }

    private Address mapAddressToEntity(AddressDTO d) {
        log.debug("Mapping Address DTO to entity.");
        Address a = new Address();
        a.setStreet(d.getStreet());
        a.setCity(d.getCity());
        a.setState(d.getState());
        a.setCountry(d.getCountry());
        a.setZipCode(d.getZipCode());
        a.setType(d.getType());
        return a;
    }

    @Override
    public List<CustomerDTO> searchCustomers(String query) {
        // TODO: Implement search functionality
        return null;
    }
}