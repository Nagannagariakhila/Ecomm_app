package com.acc.service;
import com.acc.dto.AddressDTO;
import java.util.List;
public interface AddressService {

    AddressDTO getAddressById(Long id);
    List<AddressDTO> getAllAddresses();
    AddressDTO createAddress(AddressDTO addressDTO);
    AddressDTO updateAddress(Long id, AddressDTO addressDTO);
    void deleteAddress(Long id);
    List<AddressDTO> getAddressesByProfileId(Long profileId);
	List<AddressDTO> getAddressesByCustomerId(Long customerId);
}