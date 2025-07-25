package com.secureshare.securefiles.service;

import com.secureshare.securefiles.file.FileStorageService;
import com.secureshare.securefiles.service.exception.CryptoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

@Service
public class EncryptionService {
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int IV_LENGTH = 12;

    private final SecretKey secretKey;

    public EncryptionService(@Value("${app.encryption.secret-key}") String key) {
        this.secretKey = new SecretKeySpec(key.getBytes(), "AES");
    }

    public byte[] encrypt(byte[] data) throws CryptoException {
        try {
            byte[] iv = generateIv();
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            byte[] encrypted = cipher.doFinal(data);
            byte[] result = new byte[IV_LENGTH + encrypted.length];
            System.arraycopy(iv, 0, result, 0, IV_LENGTH);
            System.arraycopy(encrypted, 0, result, IV_LENGTH, encrypted.length);

            return result;
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Encryption failed", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData) throws CryptoException {
        try {
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, IV_LENGTH);

            byte[] data = new byte[encryptedData.length - IV_LENGTH];
            System.arraycopy(encryptedData, IV_LENGTH, data, 0, data.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH, iv));

            return cipher.doFinal(data);
        } catch (GeneralSecurityException e) {
            throw new CryptoException("Decryption failed", e);
        }
    }

    private byte[] generateIv() {
        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        return iv;
    }
}