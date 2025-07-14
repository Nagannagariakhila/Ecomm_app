package com.acc.entity;

import org.springframework.stereotype.Component;

@Component
public class CustomerCodeGenerator {

    private static final String PREFIX = "CH";

    public String generateCode(Long id) {
        long value = (id == null) ? 1L : id;
        return PREFIX + String.format("%03d", value);  
    }
}
