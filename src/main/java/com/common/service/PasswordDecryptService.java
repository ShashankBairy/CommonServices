package com.common.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.util.encoders.Base64;
import org.springframework.stereotype.Service;

@Service
public class PasswordDecryptService {

    // 32-byte AES key (base64). Keep this in a secret manager/env in real apps.
    private static final String PRE_KEY_SHARED = "R42FYg7zESO28+PZ7mIZte8H5ZiN6Fw5uQHWgcPqHko=";

    // MUST match the client's numChars
    private static final int NUM_CHARS_REMOVED = 1;
    
    public static String generateKey() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] key = new byte[32]; // 32 bytes for AES-256
        secureRandom.nextBytes(key);
        return Base64.toBase64String(key);
    }

    public String key() {
        String key = generateKey();
        System.out.println("Generated Key: " + key);
        return key;
    }

    /**
     * Decrypts payload: [1 byte cutIndex][ASCII modifiedBase64][ASCII removedChars]
     */
    public String decryptPassword(String encryptedPassword) throws Exception {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new Exception("Encrypted password cannot be null or empty");
        }

        // 1) Outer base64 decode (payload → bytes)
        byte[] payloadBytes;
        try {
            payloadBytes = Base64.decode(encryptedPassword);
        } catch (Exception e) {
            throw new Exception("Unable to decode base64 payload: " + e.getMessage());
        }
        if (payloadBytes.length < 1) {
            throw new Exception("Invalid payload length");
        }

        // 2) Read cutIndex and split modified vs removed
        int cutIndex = payloadBytes[0] & 0xFF;
        final byte[] modifiedBytes, removedBytes;

        if (cutIndex == 0) {
            modifiedBytes = Arrays.copyOfRange(payloadBytes, 1, payloadBytes.length);
            removedBytes = new byte[0];
        } else {
            if (payloadBytes.length < 1 + NUM_CHARS_REMOVED) {
                throw new Exception("Payload too short for removed chars: need at least " + (1 + NUM_CHARS_REMOVED));
            }
            int modifiedLen = payloadBytes.length - 1 - NUM_CHARS_REMOVED;
            modifiedBytes = Arrays.copyOfRange(payloadBytes, 1, 1 + modifiedLen);
            removedBytes = Arrays.copyOfRange(payloadBytes, 1 + modifiedLen, payloadBytes.length);
        }

        // 3) Reconstruct ASCII base64 BYTES (DO NOT base64-encode these again)
        byte[] originalBase64Bytes;
        if (cutIndex == 0) {
            originalBase64Bytes = modifiedBytes;
        } else {
            if (removedBytes.length != NUM_CHARS_REMOVED) {
                throw new Exception("Invalid removed chars length");
            }
            if (cutIndex > modifiedBytes.length) {
                throw new Exception("Cut index " + cutIndex + " exceeds modified bytes length " + modifiedBytes.length);
            }
            originalBase64Bytes = new byte[modifiedBytes.length + removedBytes.length];
            System.arraycopy(modifiedBytes, 0, originalBase64Bytes, 0, cutIndex);
            System.arraycopy(removedBytes, 0, originalBase64Bytes, cutIndex, removedBytes.length);
            System.arraycopy(modifiedBytes, cutIndex, originalBase64Bytes, cutIndex + removedBytes.length, modifiedBytes.length - cutIndex);
        }

        // 4) Convert ASCII bytes → String (this is the original base64 text)
        String reconstructedBase64 = new String(originalBase64Bytes, StandardCharsets.UTF_8);

        // Basic sanity: padding only at end, length % 4 == 0
        if (!reconstructedBase64.matches("^[A-Za-z0-9+/=]+$")) {
            throw new Exception("Invalid base64 characters in reconstructed string");
        }
        int firstEq = reconstructedBase64.indexOf('=');
        if (firstEq != -1 && firstEq < reconstructedBase64.length() - 2) {
            throw new Exception("Base64 padding '=' found before the end");
        }
        if (reconstructedBase64.length() % 4 != 0) {
            throw new Exception("Invalid base64 length: " + reconstructedBase64.length());
        }

        // 5) Decode base64 → AES ciphertext bytes
        byte[] encryptedBytes;
        try {
            encryptedBytes = Base64.decode(reconstructedBase64);
        } catch (Exception e) {
            throw new Exception("Unable to decode reconstructed base64: " + e.getMessage());
        }
        if (encryptedBytes.length % 16 != 0) {
            throw new Exception("Invalid encrypted bytes length: " + encryptedBytes.length + " (must be multiple of 16)");
        }

        // 6) Key decode + AES/ECB/PKCS5 decryption
        byte[] keyBytes = Base64.decode(PRE_KEY_SHARED);
        if (keyBytes.length != 32) {
            throw new Exception("Invalid key length: expected 32 bytes");
        }
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decrypted = cipher.doFinal(encryptedBytes);
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}


