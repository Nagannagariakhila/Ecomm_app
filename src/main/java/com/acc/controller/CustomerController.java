package com.acc.controller;

import com.acc.dto.CustomerDTO;
import com.acc.dto.UserDTO;
import com.acc.dto.AuthResponseDTO;
import com.acc.service.CustomerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

	@Autowired
	private CustomerService customerService;

	@PostMapping("/register")
	public ResponseEntity<CustomerDTO> registerCustomer(@Validated @RequestBody CustomerDTO customerDto) {
		CustomerDTO savedCustomerDTO = customerService.saveCustomer(customerDto);
		return new ResponseEntity<>(savedCustomerDTO, HttpStatus.CREATED);
	}

	@PostMapping("/login")
	public ResponseEntity<AuthResponseDTO> loginCustomer(@Validated @RequestBody UserDTO userDto) {
		AuthResponseDTO authResponse = customerService.loginCustomer(userDto);
		return new ResponseEntity<>(authResponse, HttpStatus.OK);
	}
	
	@GetMapping("/all")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<List<CustomerDTO>> getAllCustomers() {
	    List<CustomerDTO> customers = customerService.getAllCustomers();
	    return new ResponseEntity<>(customers, HttpStatus.OK);
	}

	@GetMapping("/{customerId}")
	public ResponseEntity<CustomerDTO> getCustomerById(@PathVariable Long customerId) {
		CustomerDTO customerDTO = customerService.getCustomerById(customerId);
		return new ResponseEntity<>(customerDTO, HttpStatus.OK);
	}

	@GetMapping("/email/{email}")
	public ResponseEntity<CustomerDTO> getCustomerByEmail(@PathVariable String email) {
		CustomerDTO customerDTO = customerService.getCustomerByEmail(email);
		return new ResponseEntity<>(customerDTO, HttpStatus.OK);
	}

	@DeleteMapping("/{customerId}")
	public ResponseEntity<Void> deleteCustomer(@PathVariable Long customerId) {
		customerService.deleteCustomer(customerId);
		return new ResponseEntity<>(HttpStatus.NO_CONTENT);
	}
	
	@PutMapping("/{customerId}")
	public ResponseEntity<CustomerDTO> updateCustomer(
	        @PathVariable Long customerId,
	        @RequestBody CustomerDTO customerDTO) {

	    CustomerDTO updated = customerService.updateCustomer(customerId, customerDTO);
	    return new ResponseEntity<>(updated, HttpStatus.OK);
	}
	
	
	
	


}