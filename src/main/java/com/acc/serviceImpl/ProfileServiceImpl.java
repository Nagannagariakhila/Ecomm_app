package com.acc.serviceImpl;

import com.acc.dto.AddressDTO;
import com.acc.dto.ProfileDTO;
import com.acc.entity.Address;
import com.acc.entity.Customer;
import com.acc.entity.Profile;
import com.acc.exception.ResourceNotFoundException;
import com.acc.repository.CustomerRepository;
import com.acc.repository.ProfileRepository;
import com.acc.service.ProfileService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private ProfileDTO convertToDTO(Profile profile) {
        ProfileDTO dto = new ProfileDTO();
        dto.setId(profile.getId());
        dto.setFirstName(profile.getFirstName());
        dto.setLastName(profile.getLastName());
        dto.setEmail(profile.getEmail()); 
        dto.setPhoneNumber(profile.getPhoneNumber());
        dto.setCustomerId(profile.getCustomer() != null ? profile.getCustomer().getId() : null);

        if (profile.getAddresses() != null && !profile.getAddresses().isEmpty()) {
            dto.setAddresses(profile.getAddresses().stream()
                    .map(this::convertAddressToDTO)
                    .collect(Collectors.toList()));
        } else {
            dto.setAddresses(new ArrayList<>());
        }
        return dto;
    }

    private AddressDTO convertAddressToDTO(Address address) {
        AddressDTO dto = new AddressDTO();
        dto.setId(address.getId());
        dto.setStreet(address.getStreet());
        dto.setCity(address.getCity());
        dto.setState(address.getState());
        dto.setCountry(address.getCountry());
        dto.setZipCode(address.getZipCode());
        dto.setType(address.getType());
        dto.setProfileId(address.getProfile() != null ? address.getProfile().getId() : null);
        return dto;
    }

    private Address convertToEntity(AddressDTO addressDto) {
        Address address = new Address();
        address.setStreet(addressDto.getStreet());
        address.setCity(addressDto.getCity());
        address.setState(addressDto.getState());
        address.setCountry(addressDto.getCountry());
        address.setZipCode(addressDto.getZipCode());
        address.setType(addressDto.getType());
        return address;
    }

    @Override
    @Transactional
    public ProfileDTO saveProfile(ProfileDTO profileDTO) {
        if (profileDTO.getCustomerId() == null) {
            throw new IllegalArgumentException("Customer ID is required to create a profile.");
        }

        Customer customer = customerRepository.findById(profileDTO.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", "Id", profileDTO.getCustomerId()));

        Profile newProfile = new Profile();
        newProfile.setFirstName(profileDTO.getFirstName());
        newProfile.setLastName(profileDTO.getLastName());
        newProfile.setPhoneNumber(profileDTO.getPhoneNumber());
        newProfile.setEmail(profileDTO.getEmail()); 
        newProfile.setCustomer(customer);

        if (profileDTO.getAddresses() != null && !profileDTO.getAddresses().isEmpty()) {
            for (AddressDTO addressDto : profileDTO.getAddresses()) {
                Address address = convertToEntity(addressDto);
                newProfile.addAddress(address);
            }
        }

        Profile savedProfile = profileRepository.save(newProfile);
        return convertToDTO(savedProfile);
    }

    @Override
    @Transactional
    public ProfileDTO updateProfile(Long id, ProfileDTO profileDTO) {
        Profile existingProfile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "Id", id));

        existingProfile.setFirstName(profileDTO.getFirstName());
        existingProfile.setLastName(profileDTO.getLastName());
        existingProfile.setPhoneNumber(profileDTO.getPhoneNumber());
        existingProfile.setEmail(profileDTO.getEmail()); 

        if (profileDTO.getAddresses() != null) {
            Map<Long, Address> existingAddressesMap = existingProfile.getAddresses().stream()
                    .filter(a -> a.getId() != null)
                    .collect(Collectors.toMap(Address::getId, address -> address));

            Set<Long> incomingAddressIds = profileDTO.getAddresses().stream()
                    .map(AddressDTO::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            for (AddressDTO addressDto : profileDTO.getAddresses()) {
                if (addressDto.getId() != null && existingAddressesMap.containsKey(addressDto.getId())) {
                    Address addressToUpdate = existingAddressesMap.get(addressDto.getId());
                    addressToUpdate.setStreet(addressDto.getStreet());
                    addressToUpdate.setCity(addressDto.getCity());
                    addressToUpdate.setState(addressDto.getState());
                    addressToUpdate.setCountry(addressDto.getCountry());
                    addressToUpdate.setZipCode(addressDto.getZipCode());
                    addressToUpdate.setType(addressDto.getType());
                } else {
                    Address newAddress = convertToEntity(addressDto);
                    existingProfile.addAddress(newAddress);
                }
            }

            Iterator<Address> iterator = existingProfile.getAddresses().iterator();
            while (iterator.hasNext()) {
                Address existingAddress = iterator.next();
                if (existingAddress.getId() != null && !incomingAddressIds.contains(existingAddress.getId())) {
                    iterator.remove();
                    existingAddress.setProfile(null);
                }
            }

        } else {
            if (existingProfile.getAddresses() != null) {
                existingProfile.getAddresses().forEach(address -> address.setProfile(null));
                existingProfile.getAddresses().clear();
            }
        }

        Profile updatedProfile = profileRepository.save(existingProfile);
        return convertToDTO(updatedProfile);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileDTO getProfileById(Long profileId) {
        Profile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "Id", profileId));
        return convertToDTO(profile);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProfileDTO> getAllProfiles() {
        List<Profile> profiles = profileRepository.findAll();
        return profiles.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteProfile(Long id) {
        Profile existingProfile = profileRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Profile", "Id", id));
        profileRepository.delete(existingProfile);
    }
}
