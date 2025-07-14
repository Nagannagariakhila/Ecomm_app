package com.acc.serviceImpl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.acc.config.JwtUtil;
import com.acc.dto.*;
import com.acc.entity.*;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.*;
import com.acc.service.CustomerService;

@Service
@Transactional
public class CustomerServiceImpl implements CustomerService {

    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private CustomerCodeGenerator customerCodeGenerator;

    private static final String CUSTOMER_CODE_PREFIX = "CH";

    @Override
    public CustomerDTO saveCustomer(CustomerDTO customerDto) {
        UserDTO userDetails = customerDto.getUserDetails();
        if (userDetails == null)
            throw new IllegalArgumentException("User details required");

        userRepository.findByUsername(userDetails.getUsername())
                      .ifPresent(u -> { throw new IllegalArgumentException("Username already exists"); });

        userRepository.findByEmail(userDetails.getEmail())
                      .ifPresent(u -> { throw new IllegalArgumentException("Email already exists"); });

        Customer customer = new Customer();
        customer.setUsername(userDetails.getUsername());
        customer.setEmail(userDetails.getEmail());
        customer.setPassword(passwordEncoder.encode(userDetails.getPassword()));

        Role role = roleRepository.findByName("ROLE_CUSTOMER")
                                  .orElseThrow(() ->
                                       new ResourceNotFoundException("Role","name","ROLE_CUSTOMER"));
        customer.setRoles(Set.of(role));

        customer = customerRepository.save(customer); // saves & assigns ID

        ProfileDTO profileDTO = customerDto.getProfileDetails();
        if (profileDTO != null) {
            Profile profile = new Profile();
            profile.setFirstName(profileDTO.getFirstName());
            profile.setLastName(profileDTO.getLastName());
            profile.setPhoneNumber(profileDTO.getPhoneNumber());
            profile.setEmail(profileDTO.getEmail() != null
                             ? profileDTO.getEmail()
                             : customer.getEmail());
            profile.setCustomer(customer);

            if (profileDTO.getAddresses() != null) {
                for (AddressDTO aDto : profileDTO.getAddresses()) {
                    Address address = mapAddressToEntity(aDto);
                    profile.addAddress(address);
                }
            }

            profile = profileRepository.save(profile);
            customer.setProfile(profile);
        }

        String customerCode = generateCustomerCode();
        while (customerRepository.existsByCustomerCode(customerCode)) {
            customerCode = generateCustomerCode();
        }
        customer.setCustomerCode(customerCode);

        customer = customerRepository.save(customer);

        return mapCustomerToDTO(customer);
    }

    @Override
    public AuthResponseDTO loginCustomer(UserDTO userDto) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        userDto.getEmail(), userDto.getPassword()));
        String token = jwtUtil.generateToken((UserDetails) auth.getPrincipal());
        return new AuthResponseDTO(token, "Customer login successful!");
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerById(Long id) {
        return mapCustomerToDTO(customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer","Id",id)));
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO getCustomerByEmail(String email) {
        return mapCustomerToDTO(customerRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Customer","Email",email)));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerDTO> getAllCustomers() {
        return customerRepository.findAll()
                                 .stream()
                                 .map(this::mapCustomerToDTO)
                                 .collect(Collectors.toList());
    }

    @Override
    public CustomerDTO updateCustomer(Long id, CustomerDTO dto) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer","Id",id));

        UserDTO u = dto.getUserDetails();
        if (u != null) {
            if (!customer.getUsername().equals(u.getUsername()) &&
                userRepository.findByUsername(u.getUsername()).isPresent())
                throw new IllegalArgumentException("Username already exists: "+u.getUsername());

            if (!customer.getEmail().equals(u.getEmail()) &&
                userRepository.findByEmail(u.getEmail()).isPresent())
                throw new IllegalArgumentException("Email already registered: "+u.getEmail());

            customer.setUsername(u.getUsername());
            customer.setEmail(u.getEmail());
        }

        ProfileDTO pDto = dto.getProfileDetails();
        if (pDto != null) {
            Profile profile = customer.getProfile();
            if (profile == null) {
                profile = new Profile();
                profile.setCustomer(customer);
                customer.setProfile(profile);
            }

            profile.setFirstName(pDto.getFirstName());
            profile.setLastName(pDto.getLastName());
            profile.setPhoneNumber(pDto.getPhoneNumber());
            profile.setEmail(pDto.getEmail() != null ? pDto.getEmail() : customer.getEmail());

            if (pDto.getAddresses() != null) {
                profile.setAddresses(pDto.getAddresses().stream()
                        .map(this::mapAddressToEntity)
                        .collect(Collectors.toList()));
            }

            profileRepository.save(profile); // ✅ REQUIRED to persist changes!
        }

        if (customer.getCustomerCode() == null) {
            customer.setCustomerCode(customerCodeGenerator.generateCode(customer.getId()));
        }

        return mapCustomerToDTO(customerRepository.save(customer));
    }

    @Override
    public void deleteCustomer(Long id) {
        Customer c = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer","Id",id));
        customerRepository.delete(c);
    }

    private CustomerDTO mapCustomerToDTO(Customer c) {
    CustomerDTO dto = new CustomerDTO();

    // Set UserDetails
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

        // Convert address list
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
        Address a = new Address();
        a.setStreet(d.getStreet());
        a.setCity(d.getCity());
        a.setState(d.getState());
        a.setCountry(d.getCountry());
        a.setZipCode(d.getZipCode());
        a.setType(d.getType());
        return a;
    }

    private String generateCustomerCode() {
        Integer max = customerRepository.findMaxCustomerCodeNumberWithLock();
        int next  = (max != null) ? max + 1 : 1;
        return String.format("%s%03d", CUSTOMER_CODE_PREFIX, next);
    }
}
