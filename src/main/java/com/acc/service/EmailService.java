package com.acc.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
public class EmailService {

    @Value("${mailtrap.api.url}")
    private String mailtrapApiUrl;

    @Value("${mailtrap.api.token}")
    private String mailtrapApiToken;

    @Value("${mailtrap.from.email}")
    private String fromEmail;

    private final RestTemplate restTemplate;

    public EmailService() {
        this.restTemplate = new RestTemplate();
    }

    public void sendEmail(String to, String subject, String body) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + mailtrapApiToken);

            Map<String, Object> from = new HashMap<>();
            from.put("email", fromEmail);

            Map<String, String> toRecipient = new HashMap<>();
            toRecipient.put("email", to);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("from", from);
            requestBody.put("to", Collections.singletonList(toRecipient));
            requestBody.put("subject", subject);
            requestBody.put("text", body);
            requestBody.put("category", "OTP");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(mailtrapApiUrl, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                System.out.println("Email sent successfully to " + to);
            } else {
                System.err.println("Failed to send email. Response: " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error sending email: " + e.getMessage());
            e.printStackTrace();
        }
    }
}