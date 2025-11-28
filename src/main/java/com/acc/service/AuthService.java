package com.acc.service;

import com.acc.entity.JwtTokenUtil;
import com.acc.entity.Otp;
import com.acc.entity.Role;
import com.acc.entity.User;
import com.acc.repository.OtpRepository;
import com.acc.repository.UserRepository;
import com.acc.dto.AuthResponseDTO;
import com.acc.dto.UserDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AuthService {


    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OtpRepository otpRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserService userService;


    @Transactional
    public void generateAndSendOtp(String email) {
        log.info("Attempting to generate OTP for user with email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("User not found for email: {}", email);
                    return new IllegalArgumentException("User not found for email: " + email);
                });


        otpRepository.deleteByEmail(email);
        log.debug("Deleted any existing OTP record for email: {}", email);

        String otp = generateOtp();

        Otp otpEntity = new Otp();
        otpEntity.setEmail(email);
        otpEntity.setCode(otp);
        otpEntity.setExpiryTime(LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otpEntity);


        log.debug("OTP generated for {}. Code: {}. Expiry Time: {}", email, otp, otpEntity.getExpiryTime());

        String emailBody = "Your one-time password (OTP) for login is: " + otp + "\n\nThis code is valid for 5 minutes.";
        emailService.sendEmail(email, "Login OTP", emailBody);

        log.info("OTP successfully generated and email scheduled for sending to: {}", email);
    }


    @Transactional
    public AuthResponseDTO verifyOtp(String email, String otp) {

        log.info("Attempting to verify OTP for email: {}", email);
        log.debug("Server current time: {}", LocalDateTime.now());

        Otp otpEntity = otpRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("OTP record not found for email: {}", email);
                    return new IllegalArgumentException("OTP not found for this email.");
                });

        log.debug("Stored OTP Expiry Time: {}", otpEntity.getExpiryTime());

        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otpEntity);
            log.warn("OTP expired for email: {}. Deleting record.", email);
            throw new IllegalArgumentException("OTP has expired. Please request a new one.");
        }


        log.debug("Comparing OTPs. Stored: '{}', Received: '{}'", otpEntity.getCode(), otp);

        if (!otpEntity.getCode().equals(otp)) {

            log.warn("Invalid OTP provided for email: {}. Received OTP did not match stored OTP.", email);
            throw new IllegalArgumentException("Invalid OTP.");
        }

        log.info("OTP matched successfully for email: {}. Proceeding with authentication.", email);


        otpRepository.delete(otpEntity);
        log.debug("OTP record deleted after successful verification for email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {

                    log.error("Verified OTP but user not found by email: {}", email);
                    return new IllegalArgumentException("User not found.");
                });


        String token = jwtTokenUtil.generateToken(user);


        UserDTO userDTO = userService.convertToDTO(user);


        String roleName = user.getRoles().stream()
                .findFirst()
                .map(Role::getName)
                .orElse("ROLE_USER");

        log.info("JWT generated for user {} with role: {}", user.getUsername(), roleName.replace("ROLE_", ""));


        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(token);
        response.setMessage("OTP verified successfully and logged in.");
        response.setRole(roleName.replace("ROLE_", ""));
        response.setUser(userDTO);

        return response;
    }

    private String generateOtp() {
        Random random = new Random();

        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}