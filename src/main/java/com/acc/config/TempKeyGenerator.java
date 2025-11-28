package com.acc.config;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Base64;

public class TempKeyGenerator {

    private static final Logger logger = LoggerFactory.getLogger(TempKeyGenerator.class);

    public static void main(String[] args) {
        SecretKey key = Keys.secretKeyFor(SignatureAlgorithm.HS512);
        String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
        
        logger.info("--- COPY THIS KEY ---");
        logger.info(base64Key);
        logger.info("---------------------");
    }
}