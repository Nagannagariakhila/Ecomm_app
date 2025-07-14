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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public AddressDTO getAddressById(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "Id", id));
        return convertToDTO(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAllAddresses() {
        return addressRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public AddressDTO createAddress(AddressDTO addressDTO) {
        if (addressDTO.getProfileId() == null) {
            throw new IllegalArgumentException("Profile ID is required to create an address.");
        }

        Profile profile = profileRepository.findById(addressDTO.getProfileId())
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "Id", addressDTO.getProfileId()));

        Address address = convertToEntity(addressDTO);
        address.setProfile(profile);
        profile.addAddress(address);

        Address savedAddress = addressRepository.save(address);
        return convertToDTO(savedAddress);
    }

    @Override
    @Transactional
    public AddressDTO updateAddress(Long id, AddressDTO addressDTO) {
        Address existingAddress = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "Id", id));

        existingAddress.setStreet(addressDTO.getStreet());
        existingAddress.setCity(addressDTO.getCity());
        existingAddress.setState(addressDTO.getState());
        existingAddress.setCountry(addressDTO.getCountry());
        existingAddress.setZipCode(addressDTO.getZipCode());
        existingAddress.setType(addressDTO.getType());

        if (addressDTO.getProfileId() != null &&
            existingAddress.getProfile() != null &&
            !existingAddress.getProfile().getId().equals(addressDTO.getProfileId())) {
            throw new IllegalArgumentException("Cannot change the associated profile of an existing address directly.");
        }

        Address updatedAddress = addressRepository.save(existingAddress);
        return convertToDTO(updatedAddress);
    }

    @Override
    @Transactional
    public void deleteAddress(Long id) {
        Address address = addressRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Address", "Id", id));

        if (address.getProfile() != null) {
            address.getProfile().removeAddress(address);
        }

        addressRepository.delete(address);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAddressesByProfileId(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "Id", profileId));

        return profile.getAddresses().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressDTO> getAddressesByCustomerId(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "Id", customerId));

        Profile profile = customer.getProfile();
        if (profile == null) {
            throw new ResourceNotFoundException("Profile", "CustomerId", customerId);
        }

        return profile.getAddresses().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    

    private AddressDTO convertToDTO(Address address) {
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
