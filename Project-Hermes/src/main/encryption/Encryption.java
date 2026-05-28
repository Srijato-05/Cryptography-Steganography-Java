package src.main.encryption;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Enhanced Encryption Engine.
 * Supports GCM, ChaCha20-Poly1305, and AES-CBC.
 */
public class Encryption {

    private static final int SALT_LENGTH_BYTE = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH_BIT = 256;

    public void encryptFile(File inputFile, File outputFile, String password, String algorithmName) throws Exception {
        byte algoId = getAlgorithmId(algorithmName);
        int ivLength = getIvLength(algoId);
        String cipherName = getCipherName(algoId);

        byte[] salt = getRandomBytes(SALT_LENGTH_BYTE);
        byte[] iv = getRandomBytes(ivLength);

        SecretKey secretKey = getSecretKey(password, salt, algoId);
        Cipher cipher = Cipher.getInstance(cipherName);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, getParameterSpec(algoId, iv));

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            // Write Header: AlgoId (1 byte) + Salt (16 bytes) + IV
            fos.write(algoId);
            fos.write(salt);
            fos.write(iv);

            try (CipherOutputStream cos = new CipherOutputStream(fos, cipher);
                 FileInputStream fis = new FileInputStream(inputFile)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = fis.read(buffer)) != -1) {
                    cos.write(buffer, 0, count);
                }
            }
        }
    }

    public String encryptMessage(String message, String password, String algorithmName) throws Exception {
        byte algoId = getAlgorithmId(algorithmName);
        int ivLength = getIvLength(algoId);
        String cipherName = getCipherName(algoId);

        byte[] salt = getRandomBytes(SALT_LENGTH_BYTE);
        byte[] iv = getRandomBytes(ivLength);

        SecretKey secretKey = getSecretKey(password, salt, algoId);
        Cipher cipher = Cipher.getInstance(cipherName);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, getParameterSpec(algoId, iv));

        byte[] encryptedBytes = cipher.doFinal(message.getBytes(StandardCharsets.UTF_8));

        // Combine: AlgoId + Salt + IV + EncryptedData
        byte[] combined = new byte[1 + salt.length + iv.length + encryptedBytes.length];
        combined[0] = algoId;
        System.arraycopy(salt, 0, combined, 1, salt.length);
        System.arraycopy(iv, 0, combined, 1 + salt.length, iv.length);
        System.arraycopy(encryptedBytes, 0, combined, 1 + salt.length + iv.length, encryptedBytes.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    // Keep legacy single-algorithm signatures to support normal usage without breakage
    public String encryptMessage(String message, String password) throws Exception {
        return encryptMessage(message, password, "AES-GCM");
    }

    public void encryptFile(File inputFile, File outputFile, String password) throws Exception {
        encryptFile(inputFile, outputFile, password, "AES-GCM");
    }

    // Helper mapping methods
    public static byte getAlgorithmId(String name) {
        if ("ChaCha20".equalsIgnoreCase(name) || "ChaCha20-Poly1305".equalsIgnoreCase(name)) return 0x01;
        if ("AES-CBC".equalsIgnoreCase(name)) return 0x02;
        return 0x00; // Default: AES-GCM
    }

    public static String getAlgorithmName(byte id) {
        switch (id) {
            case 0x01: return "ChaCha20-Poly1305";
            case 0x02: return "AES-CBC";
            default: return "AES-GCM";
        }
    }

    public static int getIvLength(byte id) {
        if (id == 0x02) return 16; // AES-CBC IV is 16 bytes
        return 12; // AES-GCM and ChaCha20-Poly1305 IV is 12 bytes
    }

    public static String getCipherName(byte id) {
        switch (id) {
            case 0x01: return "ChaCha20-Poly1305";
            case 0x02: return "AES/CBC/PKCS5Padding";
            default: return "AES/GCM/NoPadding";
        }
    }

    public static String getKeySpecAlgorithm(byte id) {
        if (id == 0x01) return "ChaCha20";
        return "AES";
    }

    public static AlgorithmParameterSpec getParameterSpec(byte id, byte[] iv) {
        if (id == 0x00) return new GCMParameterSpec(128, iv);
        return new IvParameterSpec(iv);
    }

    private SecretKey getSecretKey(String password, byte[] salt, byte id) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), getKeySpecAlgorithm(id));
    }

    private byte[] getRandomBytes(int length) {
        byte[] bytes = new byte[length];
        new SecureRandom().nextBytes(bytes);
        return bytes;
    }
}