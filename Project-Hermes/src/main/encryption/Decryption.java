package src.main.encryption;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Enhanced Decryption Engine.
 * Supports auto-detection of AES-GCM, ChaCha20-Poly1305, and AES-CBC.
 */
public class Decryption {

    private static final int SALT_LENGTH_BYTE = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH_BIT = 256;

    public void decryptFile(File inputFile, File outputFile, String password) throws Exception {
        try (FileInputStream fis = new FileInputStream(inputFile)) {

            // 1. Read Algorithm ID (First 1 Byte)
            int algoIdInt = fis.read();
            if (algoIdInt == -1) {
                throw new Exception("File corrupted: Missing Algorithm ID.");
            }
            byte algoId = (byte) algoIdInt;

            // 2. Read the Salt (Next 16 Bytes)
            byte[] salt = new byte[SALT_LENGTH_BYTE];
            if (fis.read(salt) != SALT_LENGTH_BYTE) {
                throw new Exception("File corrupted: Missing Salt header.");
            }

            // 3. Read the IV (Next 12 or 16 Bytes depending on AlgoId)
            int ivLength = Encryption.getIvLength(algoId);
            byte[] iv = new byte[ivLength];
            if (fis.read(iv) != ivLength) {
                throw new Exception("File corrupted: Missing IV header.");
            }

            // 4. Regenerate Key
            SecretKey secretKey = getSecretKey(password, salt, algoId);

            // 5. Initialize Cipher
            Cipher cipher = Cipher.getInstance(Encryption.getCipherName(algoId));
            cipher.init(Cipher.DECRYPT_MODE, secretKey, Encryption.getParameterSpec(algoId, iv));

            // 6. Decrypt Stream
            try (CipherInputStream cis = new CipherInputStream(fis, cipher);
                 FileOutputStream fos = new FileOutputStream(outputFile)) {

                byte[] buffer = new byte[8192];
                int count;
                while ((count = cis.read(buffer)) != -1) {
                    fos.write(buffer, 0, count);
                }
            }
        }
    }

    public String decryptMessage(String encryptedBase64, String password) throws Exception {
        // 1. Decode Base64 to get raw bytes
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);

        if (combined.length < 1 + SALT_LENGTH_BYTE) {
            throw new Exception("Data corrupted: payload too short.");
        }

        // 2. Extract Algo ID
        byte algoId = combined[0];

        // 3. Extract Salt
        byte[] salt = Arrays.copyOfRange(combined, 1, 1 + SALT_LENGTH_BYTE);

        // 4. Extract IV
        int ivLength = Encryption.getIvLength(algoId);
        if (combined.length < 1 + SALT_LENGTH_BYTE + ivLength) {
            throw new Exception("Data corrupted: payload too short for IV.");
        }
        byte[] iv = Arrays.copyOfRange(combined, 1 + SALT_LENGTH_BYTE, 1 + SALT_LENGTH_BYTE + ivLength);

        // 5. Extract Encrypted Content
        byte[] encryptedContent = Arrays.copyOfRange(combined, 1 + SALT_LENGTH_BYTE + ivLength, combined.length);

        // 6. Regenerate Key & Decrypt
        SecretKey secretKey = getSecretKey(password, salt, algoId);

        Cipher cipher = Cipher.getInstance(Encryption.getCipherName(algoId));
        cipher.init(Cipher.DECRYPT_MODE, secretKey, Encryption.getParameterSpec(algoId, iv));

        byte[] decryptedBytes = cipher.doFinal(encryptedContent);

        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }

    private SecretKey getSecretKey(String password, byte[] salt, byte id) throws Exception {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BIT);
        SecretKey tmp = factory.generateSecret(spec);
        return new SecretKeySpec(tmp.getEncoded(), Encryption.getKeySpecAlgorithm(id));
    }
}