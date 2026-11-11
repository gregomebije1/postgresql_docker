package com.example.wallet.security;

import org.junit.jupiter.api.Test;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CustomerMetadataEncryptionTest {
    @Test
    void aes256RoundTripWorks() throws Exception {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) key[i] = (byte) i;

        String plaintext = "+2348030000001";
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
        String encrypted = Base64.getEncoder().encodeToString(
                cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8)));

        assertNotEquals(plaintext, encrypted);
        assertFalse(encrypted.contains("8030000001"));

        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"));
        String decrypted = new String(
                cipher.doFinal(Base64.getDecoder().decode(encrypted)),
                StandardCharsets.UTF_8);

        assertEquals(plaintext, decrypted);
    }
}
