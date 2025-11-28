package com.acc.controller;
import com.acc.dto.UserDTO;
import com.acc.entity.User;
import com.acc.service.UserService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
@RestController
@RequestMapping("/api/users")
public class UserController {
    @Autowired
    private UserService userService;
    @PostMapping("/register")
    public ResponseEntity<User> registerUser(@Validated @RequestBody UserDTO userDTO) { 
        User registeredUser = userService.save(userDTO);
        return new ResponseEntity<>(registeredUser, HttpStatus.CREATED);
    }


    @GetMapping 
    @PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')") 
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = userService.getAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }
    

}