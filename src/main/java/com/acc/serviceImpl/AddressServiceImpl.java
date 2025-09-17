package com.acc.serviceImpl;

import com.acc.dto.AddressDTO;
import com.acc.entity.Address;
import com.acc.entity.Customer;
import com.acc.entity.Profile;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.AddressRepository;
import com.acc.repository.CustomerRepository;
import com.acc.repository.ProfileRepository;
import com.acc.service.AddressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    private static final Logger log = LoggerFactory.getLogger(AddressServiceImpl.class);

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long id) {
        log.info("Fetching address by ID: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Address not found with ID: {}", id);
                    return new ResourceNotFoundException("Address", "Id", id);
                });
        log.debug("Found address with ID: {}", id);
        return convertToDTO(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAllAddresses() {
        log.info("Fetching all addresses.");
        List<AddressDTO> addresses = addressRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} addresses.", addresses.size());
        return addresses;
    }

    @Override
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO) {
        log.info("Creating a new address for profile ID: {}", addressDTO.getProfileId());
        if (addressDTO.getProfileId() == null) {
            log.error("Profile ID is missing for address creation.");
            throw new IllegalArgumentException("Profile ID is required to create an address.");
        }

        Profile profile = profileRepository.findById(addressDTO.getProfileId())
                .orElseThrow(() -> {
                    log.error("Profile not found with ID: {}", addressDTO.getProfileId());
                    return new ResourceNotFoundException("Profile", "Id", addressDTO.getProfileId());
                });

        Address address = convertToEntity(addressDTO);
        address.setProfile(profile);
        profile.addAddress(address);

        Address savedAddress = addressRepository.save(address);
        log.info("Address created successfully with ID: {} for profile ID: {}", savedAddress.getId(), profile.getId());
        return convertToDTO(savedAddress);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long id, AddressDTO addressDTO) {
        log.info("Updating address with ID: {}", id);
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Address not found with ID: {}", id);
                    return new ResourceNotFoundException("Address", "Id", id);
                });

        existingAddress.setStreet(addressDTO.getStreet());
        existingAddress.setCity(addressDTO.getCity());
        existingAddress.setState(addressDTO.getState());
        existingAddress.setCountry(addressDTO.getCountry());
        existingAddress.setZipCode(addressDTO.getZipCode());
        existingAddress.setType(addressDTO.getType());
        log.debug("Updated address fields for ID: {}", id);

        if (addressDTO.getProfileId() != null &&
            existingAddress.getProfile() != null &&
            !existingAddress.getProfile().getId().equals(addressDTO.getProfileId())) {
            log.error("Attempted to change profile ID for address {}. This operation is not allowed.", id);
            throw new IllegalArgumentException("Cannot change the associated profile of an existing address directly.");
        }

        Address updatedAddress = addressRepository.save(existingAddress);
        log.info("Address with ID: {} updated successfully.", updatedAddress.getId());
        return convertToDTO(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        log.info("Deleting address with ID: {}", id);
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Address not found with ID: {}", id);
                    return new ResourceNotFoundException("Address", "Id", id);
                });

        if (address.getProfile() != null) {
            address.getProfile().removeAddress(address);
            log.debug("Removed address ID {} from profile ID {}", id, address.getProfile().getId());
        }

        addressRepository.delete(address);
        log.info("Address with ID: {} deleted successfully.", id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAddressesByProfileId(Long profileId) {
        log.info("Fetching all addresses for profile ID: {}", profileId);
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> {
                    log.error("Profile not found with ID: {}", profileId);
                    return new ResourceNotFoundException("Profile", "Id", profileId);
                });

        List<AddressDTO> addresses = profile.getAddresses().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} addresses for profile ID: {}", addresses.size(), profileId);
        return addresses;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAddressesByCustomerId(Long customerId) {
        log.info("Fetching addresses for customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> {
                    log.error("Customer not found with ID: {}", customerId);
                    return new ResourceNotFoundException("Customer", "Id", customerId);
                });

        Profile profile = customer.getProfile();
        if (profile == null) {
            log.warn("Profile not found for customer ID: {}. Returning empty list.", customerId);
            throw new ResourceNotFoundException("Profile", "CustomerId", customerId);
        }

        List<AddressDTO> addresses = profile.getAddresses().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        log.info("Found {} addresses for customer ID: {}", addresses.size(), customerId);
        return addresses;
    }

    private AddressDTO convertToDTO(Address address) {
        log.trace("Converting Address entity with ID {} to DTO.", address.getId());
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCountry(address.getCountry());
        dto.setZipCode(address.getZipCode());
        dto.setType(address.getType());
        if (address.getProfile() != null) {
            dto.setProfileId(address.getProfile().getId());
        }
        return dto;
    }

    private Address convertToEntity(AddressDTO dto) {
        log.trace("Converting Address DTO to entity.");
        Address address = new Address();
        address.setStreet(dto.getStreet());
        address.setCity(dto.getCity());
        address.setState(dto.getState());
        address.setCountry(dto.getCountry());
        address.setZipCode(dto.getZipCode());
        address.setType(dto.getType());
        return address;
    }
}