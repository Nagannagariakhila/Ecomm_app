package com.acc.service;

import java.util.List;

import com.acc.dto.AuthResponseDTO;
import com.acc.dto.CustomerDTO;
import com.acc.dto.UserDTO;

public interface CustomerService {

    CustomerDTO getCustomerById(Long customerId);
    CustomerDTO saveCustomer(CustomerDTO customerDto);
    AuthResponseDTO loginCustomer(UserDTO userDto);
    CustomerDTO getCustomerByEmail(String email);
    void deleteCustomer(Long customerId);
    List<CustomerDTO> getAllCustomers();
    CustomerDTO updateCustomer(Long id, CustomerDTO customerDTO);
    
}