package com.acc.serviceImpl;

import com.acc.dto.AddressDTO;
import com.acc.dto.ProfileDTO;
import com.acc.entity.Address;
import com.acc.entity.Customer;
import com.acc.entity.Profile;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.CustomerRepository;
import com.acc.repository.OrderRepository;
import com.acc.repository.ProfileRepository;
import com.acc.service.ProfileService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private OrderRepository orderRepository;
    private ProfileDTO convertToDTO(Profile profile) {
        log.debug("Converting Profile entity to DTO for profile ID: {}", profile.getId());
        ProfileDTO dto = new ProfileDTO();
        dto.setId(profile.getId());
        dto.setFirstName(profile.getFirstName());
        dto.setLastName(profile.getLastName());
        dto.setEmail(profile.getEmail());
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setCustomerId(profile.getCustomer() != null ? profile.getCustomer().getId() : null);

        List<AddressDTO> addresses = profile.getAddresses() != null
                ? profile.getAddresses().stream()
                        .filter(Address::isActive)
                        .map(this::convertAddressToDTO)
                        .collect(Collectors.toList())
                : new ArrayList<>();

        dto.setAddresses(addresses);
        log.debug("Conversion to DTO complete for profile ID: {}", profile.getId());
        return dto;
    }

    private AddressDTO convertAddressToDTO(Address address) {
        log.debug("Converting Address entity to DTO for address ID: {}", address.getId());
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCountry(address.getCountry());
        dto.setZipCode(address.getZipCode());
        dto.setType(address.getType());
        dto.setProfileId(address.getProfile() != null ? address.getProfile().getId() : null);
        dto.setActive(address.isActive());
        return dto;
    }

    private Address convertToEntity(AddressDTO dto) {
        log.debug("Converting Address DTO to entity.");
        Address address = new Address();
        address.setId(dto.getId());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setCountry(dto.getCountry());
        address.setZipCode(dto.getZipCode());
        address.setType(dto.getType());
        address.setActive(dto.isActive());
        return address;
    }

    private void updateAddressEntity(Address address, AddressDTO dto) {
        log.debug("Updating Address entity with data from DTO for address ID: {}", address.getId());
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setCountry(dto.getCountry());
        address.setZipCode(dto.getZipCode());
        address.setType(dto.getType());
    }

    private boolean isValidAddress(AddressDTO dto) {
        boolean isValid = dto.getStreet() != null && !dto.getStreet().isBlank()
                && dto.getCity() != null && !dto.getCity().isBlank()
                && dto.getState() != null && !dto.getState().isBlank()
                && dto.getCountry() != null && !dto.getCountry().isBlank()
                && dto.getZipCode() != null && !dto.getZipCode().isBlank()
                && dto.getType() != null && !dto.getType().isBlank();
        if (!isValid) {
            log.warn("Invalid address DTO detected. Skipping. Details: {}", dto);
        }
        return isValid;
    }

    @Override
    @Transactional
    public ProfileDTO saveProfile(ProfileDTO profileDTO) {
        log.info("Attempting to save a new profile for customer ID: {}", profileDTO.getCustomerId());

        if (profileDTO.getCustomerId() == null) {
            log.error("Customer ID is missing in the profile DTO.");
            throw new IllegalArgumentException("Customer ID is required to create a profile.");
        }

        Customer customer = customerRepository.findById(profileDTO.getCustomerId())
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", profileDTO.getCustomerId());
                    return new ResourceNotFoundException("Customer", "Id", profileDTO.getCustomerId());
                });

        if (customer.getProfile() != null) {
            log.error("Customer with ID {} already has a profile.", profileDTO.getCustomerId());
            throw new IllegalStateException("Customer with ID " + profileDTO.getCustomerId() + " already has a profile.");
        }

        Profile profile = new Profile();
        profile.setFirstName(profileDTO.getFirstName());
        profile.setLastName(profileDTO.getLastName());
        profile.setPhoneNumber(profileDTO.getPhoneNumber());
        profile.setEmail(profileDTO.getEmail());
        profile.setCustomer(customer);

        if (profileDTO.getAddresses() != null) {
            log.debug("Processing {} addresses for the new profile.", profileDTO.getAddresses().size());
            for (AddressDTO addressDto : profileDTO.getAddresses()) {
                if (isValidAddress(addressDto)) {
                    Address address = convertToEntity(addressDto);
                    address.setActive(true);
                    profile.addAddress(address);
                    log.debug("Added new valid address to profile: {}", addressDto.getStreet());
                }
            }
        }
        
        Profile savedProfile = profileRepository.save(profile);
        log.info("Profile created and saved with ID: {}", savedProfile.getId());

        customer.setProfile(savedProfile);
        customerRepository.save(customer);
        log.debug("Associated profile ID {} with customer ID {}.", savedProfile.getId(), customer.getId());

        return convertToDTO(savedProfile);
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(Long id, ProfileDTO profileDTO) {
        log.info("Attempting to update profile with ID: {}", id);
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Profile not found with ID: {}", id);
                    return new ResourceNotFoundException("Profile", "Id", id);
                });

        profile.setFirstName(profileDTO.getFirstName());
        profile.setLastName(profileDTO.getLastName());
        profile.setEmail(profileDTO.getEmail());
        profile.setPhoneNumber(profileDTO.getPhoneNumber());

        Map<Long, Address> existingAddresses = profile.getAddresses().stream()
                .collect(Collectors.toMap(Address::getId, a -> a));
        log.debug("Found {} existing addresses for profile ID: {}", existingAddresses.size(), id);

        if (profileDTO.getAddresses() != null) {
            for (AddressDTO dto : profileDTO.getAddresses()) {
                if (dto.getId() != null && existingAddresses.containsKey(dto.getId())) {
                    updateAddressEntity(existingAddresses.get(dto.getId()), dto);
                    log.debug("Updated existing address with ID: {}", dto.getId());
                } else if (isValidAddress(dto)) {
                    Address newAddress = convertToEntity(dto);
                    newAddress.setActive(true);
                    profile.addAddress(newAddress);
                    log.debug("Added a new address to the profile.");
                }
            }
        } else {
            log.debug("No addresses provided in DTO to update.");
        }

        Profile updatedProfile = profileRepository.save(profile);
        log.info("Profile with ID {} updated successfully.", updatedProfile.getId());
        return convertToDTO(updatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(Long profileId) {
        log.info("Fetching profile with ID: {}", profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> {
                    log.error("Profile not found with ID: {}", profileId);
                    return new ResourceNotFoundException("Profile", "Id", profileId);
                });
        log.info("Profile with ID {} fetched successfully.", profileId);
        return convertToDTO(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileDTO> getAllProfiles() {
        log.info("Fetching all profiles.");
        List<ProfileDTO> profiles = profileRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Fetched {} profiles.", profiles.size());
        return profiles;
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        log.info("Attempting to delete profile with ID: {}", id);
        Profile profile = profileRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Profile not found with ID: {}", id);
                    return new ResourceNotFoundException("Profile", "Id", id);
                });
        profileRepository.delete(profile);
        log.info("Profile with ID {} deleted successfully.", id);
    }
}