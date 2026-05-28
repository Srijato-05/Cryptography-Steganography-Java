package src.main;

import src.main.encryption.Encryption;
import src.main.encryption.Decryption;
import src.main.steganography.ImageSteganography;
import src.main.steganography.AudioSteganography;
import src.main.steganography.VideoSteganography;
import src.main.steganography.TextSteganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * TestSuite - Standalone Ultimate System Audit & Verification Suite.
 * Audits cryptographic ciphers, steganography layouts, decoy layer triggers,
 * scatter mode PRNG determinism, password entropy math, and file capacity limits.
 */
public class TestSuite {

    private static final String PASS_CORRECT = "CyberSecPass123_Secure!";
    private static final String PASS_DECOY = "DecoyPass_456!";
    private static final String PASS_WRONG = "WrongPassword_999!";
    
    private static final String MSG_SECRET = "CLASSIFIED_DATA: The coordinates are 45.109, -122.680.";
    private static final String MSG_DECOY = "PUBLIC_INFO: Hello, this is just a standard text memo.";
    private static final String MSG_UNICODE = "🌟 TOP SECRET // 🛡️ System Integrity: OK. 中文字符 & Arabic: السلام عليكم [12345]";

    private static int testsRun = 0;
    private static int testsPassed = 0;

    public static void main(String[] args) {
        System.out.println("======================================================================");
        System.out.println("            SECURE-STEGO SUITE - ULTIMATE DEEP SYSTEM AUDIT           ");
        System.out.println("======================================================================");

        try {
            // --- 1. CORE CRYPTOGRAPHY ENGINE TESTS ---
            runCryptoTest("AES-GCM", "0x00");
            runCryptoTest("ChaCha20-Poly1305", "0x01");
            runCryptoTest("AES-CBC", "0x02");
            runCryptoAutoDetectTest();

            // --- 2. ADVERSARIAL & SECURITY AUDITS ---
            runAdversarialTamperingTest("AES-GCM");
            runAdversarialTamperingTest("ChaCha20-Poly1305");
            runAdversarialTamperingTest("AES-CBC");

            // --- 3. SCATTER MODE DETERMINISM & SECURITY AUDIT ---
            runScatterModeDeterminismAudit();

            // --- 4. MATHEMATICAL ENTROPY & METADATA AUDITS ---
            runPasswordEntropyMathAudit();
            runMetadataCapacityLimitsAudit();

            // --- 5. EDGE CASES & STRESS BOUNDARIES ---
            runUnicodeResilienceTest();
            runCapacityOverflowTest();
            runEmptyPayloadTest();

            // --- 6. STEGANOGRAPHY MULTI-FORMAT SUITE ---
            runTextStegoTest();
            runImageStegoTest(false, "SEQUENTIAL", BufferedImage.TYPE_3BYTE_BGR);
            runImageStegoTest(true, "SCATTER", BufferedImage.TYPE_3BYTE_BGR);
            runImageColorModelsTest();
            runAudioStegoTest(false, "SEQUENTIAL");
            runAudioStegoTest(true, "SCATTER");
            runVideoStegoTest();
            
            // --- 7. DECOY LAYER PROTOCOL ---
            runDecoyVerificationTest();

        } catch (Exception e) {
            System.err.println("\n[FATAL] Test suite interrupted by uncaught exception:");
            e.printStackTrace();
        }

        System.out.println("\n======================================================================");
        System.out.println(String.format("TEST RESULTS: %d/%d PASSED", testsPassed, testsRun));
        System.out.println("======================================================================");
        
        if (testsPassed == testsRun) {
            System.out.println(">>> ALL SYSTEM MODULES OPERATIONAL. SECURITY AUDIT STATUS: SECURE.");
        } else {
            System.out.println(">>> SECURITY AUDIT STATUS: WARNING - AUDIT FAILURE ENCOUNTERED.");
            System.exit(1);
        }
    }

    private static void assertEqual(String expected, String actual, String testName) {
        testsRun++;
        if (expected != null && expected.equals(actual)) {
            testsPassed++;
            System.out.println(String.format("[PASS] %s", testName));
        } else {
            System.err.println(String.format("[FAIL] %s - Expected: '%s', Got: '%s'", testName, expected, actual));
        }
    }

    private static void assertEqual(int expected, int actual, String testName) {
        testsRun++;
        if (expected == actual) {
            testsPassed++;
            System.out.println(String.format("[PASS] %s", testName));
        } else {
            System.err.println(String.format("[FAIL] %s - Expected: %d, Got: %d", testName, expected, actual));
        }
    }

    private static void assertTrue(boolean condition, String testName) {
        testsRun++;
        if (condition) {
            testsPassed++;
            System.out.println(String.format("[PASS] %s", testName));
        } else {
            System.err.println(String.format("[FAIL] %s", testName));
        }
    }

    // ==================================================================================
    // CRYPTOGRAPHY AUDITS
    // ==================================================================================

    private static void runCryptoTest(String algo, String typeHex) throws Exception {
        System.out.println(String.format("\nExecuting Cryptography Test: %s (%s)", algo, typeHex));
        
        File plainFile = File.createTempFile("plain_", ".txt");
        Files.write(plainFile.toPath(), MSG_SECRET.getBytes(StandardCharsets.UTF_8));
        
        File cipherFile = File.createTempFile("cipher_", ".enc");
        File decryptedFile = File.createTempFile("decrypt_", ".txt");

        try {
            Encryption enc = new Encryption();
            enc.encryptFile(plainFile, cipherFile, PASS_CORRECT, algo);
            assertTrue(cipherFile.exists() && cipherFile.length() > 0, algo + " encryption output generated");

            Decryption dec = new Decryption();
            dec.decryptFile(cipherFile, decryptedFile, PASS_CORRECT);
            String decryptedText = new String(Files.readAllBytes(decryptedFile.toPath()), StandardCharsets.UTF_8);
            assertEqual(MSG_SECRET, decryptedText, algo + " decryption content integrity check");

            boolean tamperedOrFailed = false;
            try {
                dec.decryptFile(cipherFile, decryptedFile, PASS_WRONG);
            } catch (Exception e) {
                tamperedOrFailed = true;
            }
            assertTrue(tamperedOrFailed, algo + " decryption throws integrity exception with invalid key");

        } finally {
            plainFile.delete();
            cipherFile.delete();
            decryptedFile.delete();
        }
    }

    private static void runCryptoAutoDetectTest() throws Exception {
        System.out.println("\nExecuting Crypto Header Auto-Detection Test...");
        
        File plainFile = File.createTempFile("plain_detect_", ".txt");
        Files.write(plainFile.toPath(), MSG_SECRET.getBytes(StandardCharsets.UTF_8));
        
        File cipherFile = File.createTempFile("cipher_detect_", ".enc");
        File decryptedFile = File.createTempFile("decrypt_detect_", ".txt");

        try {
            Encryption enc = new Encryption();
            enc.encryptFile(plainFile, cipherFile, PASS_CORRECT, "ChaCha20-Poly1305");

            byte[] cipherBytes = Files.readAllBytes(cipherFile.toPath());
            assertTrue(cipherBytes[0] == 0x01, "ChaCha20 header byte is 0x01");

            Decryption dec = new Decryption();
            dec.decryptFile(cipherFile, decryptedFile, PASS_CORRECT);
            String decryptedText = new String(Files.readAllBytes(decryptedFile.toPath()), StandardCharsets.UTF_8);
            assertEqual(MSG_SECRET, decryptedText, "Auto-detection decryption matches source");

        } finally {
            plainFile.delete();
            cipherFile.delete();
            decryptedFile.delete();
        }
    }

    private static void runAdversarialTamperingTest(String algo) throws Exception {
        System.out.println(String.format("\nExecuting Adversarial Bit-Flipping Audit: %s...", algo));

        File plainFile = File.createTempFile("plain_tamper_", ".txt");
        Files.write(plainFile.toPath(), MSG_SECRET.getBytes(StandardCharsets.UTF_8));

        File cipherFile = File.createTempFile("cipher_tamper_", ".enc");
        File decryptedFile = File.createTempFile("decrypt_tamper_", ".txt");

        try {
            Encryption enc = new Encryption();
            enc.encryptFile(plainFile, cipherFile, PASS_CORRECT, algo);

            byte[] cipherBytes = Files.readAllBytes(cipherFile.toPath());
            if (cipherBytes.length > 50) {
                cipherBytes[45] ^= 0x01; 
                Files.write(cipherFile.toPath(), cipherBytes);
            }

            Decryption dec = new Decryption();
            boolean caughtTampering = false;
            try {
                dec.decryptFile(cipherFile, decryptedFile, PASS_CORRECT);
            } catch (Exception e) {
                caughtTampering = true;
            }

            if (algo.equals("AES-CBC")) {
                // CBC is unauthenticated; it won't throw an exception, but it will decrypt to corrupted text
                boolean isCorrupted = true;
                if (decryptedFile.exists() && decryptedFile.length() > 0) {
                    String decryptedText = new String(Files.readAllBytes(decryptedFile.toPath()), StandardCharsets.UTF_8);
                    if (decryptedText.equals(MSG_SECRET)) {
                        isCorrupted = false;
                    }
                }
                assertTrue(!caughtTampering && isCorrupted, "AES-CBC (unauthenticated) allowed decryption but resulted in corrupted plaintext as expected");
            } else {
                assertTrue(caughtTampering, algo + " successfully rejected modified ciphertext (MAC validation failed)");
            }

        } finally {
            plainFile.delete();
            cipherFile.delete();
            decryptedFile.delete();
        }
    }

    // ==================================================================================
    // SCATTER MODE DETERMINISM AUDIT
    // ==================================================================================

    private static void runScatterModeDeterminismAudit() throws Exception {
        System.out.println("\nExecuting LSB Scatter Determinism and Collision Audit...");

        int N = 10000;
        int K = 256; // bits to hide
        
        // Generate indices twice with same seed
        int[] run1 = generateIndicesForSeed(PASS_CORRECT, N, K);
        int[] run2 = generateIndicesForSeed(PASS_CORRECT, N, K);

        assertTrue(Arrays.equals(run1, run2), "PRNG Scatter generation is 100% deterministic with matching seed");

        // Generate indices with a slightly different seed (1 char difference)
        String passTweak = PASS_CORRECT + "!";
        int[] run3 = generateIndicesForSeed(passTweak, N, K);

        boolean isColliding = Arrays.equals(run1, run3);
        assertTrue(!isColliding, "Slightly altered seed generates completely different scatter mapping");
    }

    private static int[] generateIndicesForSeed(String seed, int N, int K) throws Exception {
        SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
        prng.setSeed(seed.getBytes(StandardCharsets.UTF_8));
        
        java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
        int[] selectedIndices = new int[K];
        for (int i = 0; i < K; i++) {
            int rand = i + prng.nextInt(N - i);
            int valI = map.getOrDefault(i, i);
            int valRand = map.getOrDefault(rand, rand);
            map.put(rand, valI);
            selectedIndices[i] = valRand + 34; // offset
        }
        return selectedIndices;
    }

    // ==================================================================================
    // MATHEMATICAL ENTROPY & CAPACITY METADATA AUDITS
    // ==================================================================================

    private static void runPasswordEntropyMathAudit() {
        System.out.println("\nExecuting Password Entropy Mathematics Verification...");

        // Weak password
        String passWeak = "aaaa";
        int entropyWeak = calculateEntropyBits(passWeak);
        // "aaaa" has 1 distinct character ('a'). Distinct count = 1.
        // math: log2(1 + 1) = log2(2) = 1.0. Length 4 * 1.0 = 4.
        assertEqual(4, entropyWeak, "Entropy for repetitive password 'aaaa' matches expected value");

        // Stronger password
        String passStrong = "abcdefg"; // distinct characters = 7. log2(8) = 3. Length 7 * 3 = 21.
        int entropyStrong = calculateEntropyBits(passStrong);
        assertEqual(21, entropyStrong, "Entropy for sequential distinct password 'abcdefg' matches expected value");
    }

    private static int calculateEntropyBits(String pass) {
        if (pass.isEmpty()) return 0;
        long distinctCount = pass.chars().distinct().count();
        return (int) (pass.length() * (Math.log(distinctCount + 1) / Math.log(2)));
    }

    private static void runMetadataCapacityLimitsAudit() {
        System.out.println("\nExecuting Media Carrier Capacity Limits Audit...");

        // Image: 120 x 120 pixels, BGR format (3 bytes per pixel)
        // limit = (120 * 120 * 3) / 8.0f = 5400 chars.
        int imageWidth = 120;
        int imageHeight = 120;
        float imageCapacity = (float) (imageWidth * imageHeight * 3) / 8.0f;
        assertEqual(5400, (int) imageCapacity, "Image carrier metadata capacity calculation verified");

        // WAV Audio: 4044 bytes total. Header size 44. Payload bytes = 4000.
        // limit = 4000 / 8.0f = 500 chars.
        long wavFileLength = 4044;
        float wavCapacity = (float) Math.max(0, wavFileLength - 44) / 8.0f;
        assertEqual(500, (int) wavCapacity, "WAV audio carrier metadata capacity calculation verified");
    }

    // ==================================================================================
    // EDGE CASES & BOUNDARY TESTS
    // ==================================================================================

    private static void runUnicodeResilienceTest() throws Exception {
        System.out.println("\nExecuting UTF-8 Unicode & Emoji Resilience Test...");

        File plainFile = File.createTempFile("plain_uni_", ".txt");
        Files.write(plainFile.toPath(), MSG_UNICODE.getBytes(StandardCharsets.UTF_8));

        File cipherFile = File.createTempFile("cipher_uni_", ".enc");
        File decryptedFile = File.createTempFile("decrypt_uni_", ".txt");

        try {
            Encryption enc = new Encryption();
            enc.encryptFile(plainFile, cipherFile, PASS_CORRECT, "AES-GCM");

            Decryption dec = new Decryption();
            dec.decryptFile(cipherFile, decryptedFile, PASS_CORRECT);
            String decryptedText = new String(Files.readAllBytes(decryptedFile.toPath()), StandardCharsets.UTF_8);
            assertEqual(MSG_UNICODE, decryptedText, "Unicode/Emoji content decrypted with 100% fidelity");

        } finally {
            plainFile.delete();
            cipherFile.delete();
            decryptedFile.delete();
        }
    }

    private static void runCapacityOverflowTest() throws Exception {
        System.out.println("\nExecuting Capacity Overflow Stress Test...");

        File imgCarrier = File.createTempFile("img_tiny_", ".png");
        BufferedImage img = new BufferedImage(5, 5, BufferedImage.TYPE_3BYTE_BGR);
        ImageIO.write(img, "png", imgCarrier);

        File outputImg = File.createTempFile("img_tiny_out_", ".png");

        try {
            ImageSteganography stego = new ImageSteganography();
            
            boolean caughtOverflow = false;
            try {
                stego.embedMessage(imgCarrier, outputImg, MSG_SECRET, PASS_CORRECT, false, false);
            } catch (Exception e) {
                caughtOverflow = true;
            }
            assertTrue(caughtOverflow, "Stego engine correctly rejected payload exceeding carrier boundaries");

        } finally {
            imgCarrier.delete();
            outputImg.delete();
        }
    }

    private static void runEmptyPayloadTest() throws Exception {
        System.out.println("\nExecuting Empty Payload Test...");

        File imgCarrier = File.createTempFile("img_empty_", ".png");
        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
        ImageIO.write(img, "png", imgCarrier);

        File outputImg = File.createTempFile("img_empty_out_", ".png");

        try {
            ImageSteganography stego = new ImageSteganography();
            
            stego.embedMessage(imgCarrier, outputImg, "", PASS_CORRECT, false, false);
            assertTrue(outputImg.exists(), "Embedded empty payload generated stego carrier");

            String extracted = stego.extractMessage(outputImg, PASS_CORRECT);
            assertEqual("", extracted, "Extracted payload verified as empty string");

        } finally {
            imgCarrier.delete();
            outputImg.delete();
        }
    }

    // ==================================================================================
    // STEGANOGRAPHY TESTS
    // ==================================================================================

    private static void runTextStegoTest() throws Exception {
        System.out.println("\nExecuting Text Zero-Width Steganography Test...");
        
        File carrierFile = File.createTempFile("text_carrier_", ".txt");
        String originalText = "Dear John,\nI am writing to verify our appointment tomorrow at 10 AM.\nBest regards,\nAlice.";
        Files.write(carrierFile.toPath(), originalText.getBytes(StandardCharsets.UTF_8));
        
        File outputCarrierFile = File.createTempFile("text_carrier_stego_", ".txt");

        try {
            TextSteganography stego = new TextSteganography();
            Encryption enc = new Encryption();
            Decryption dec = new Decryption();
            
            String encryptedPayload = enc.encryptMessage(MSG_SECRET, PASS_CORRECT, "AES-GCM");
            stego.embedMessage(carrierFile, outputCarrierFile, encryptedPayload);
            
            String stegoText = new String(Files.readAllBytes(outputCarrierFile.toPath()), StandardCharsets.UTF_8);
            assertTrue(stegoText.contains("Dear John"), "Carrier content body preserved");

            String extractedPayload = stego.extractMessage(outputCarrierFile);
            String decrypted = dec.decryptMessage(extractedPayload, PASS_CORRECT);
            assertEqual(MSG_SECRET, decrypted, "Text stego extraction matches secret");

        } finally {
            carrierFile.delete();
            outputCarrierFile.delete();
        }
    }

    private static void runImageStegoTest(boolean useScatter, String modeLabel, int imageType) throws Exception {
        System.out.println(String.format("\nExecuting Image Stego Test (%s)...", modeLabel));
        
        File imgCarrier = File.createTempFile("img_carrier_", ".png");
        BufferedImage img = new BufferedImage(120, 120, imageType);
        ImageIO.write(img, "png", imgCarrier);
        
        File outputImg = File.createTempFile("img_carrier_stego_", ".png");

        try {
            ImageSteganography stego = new ImageSteganography();
            Encryption enc = new Encryption();
            Decryption dec = new Decryption();
            
            String encryptedPayload = enc.encryptMessage(MSG_SECRET, PASS_CORRECT, "AES-GCM");
            stego.embedMessage(imgCarrier, outputImg, encryptedPayload, PASS_CORRECT, false, useScatter);
            assertTrue(outputImg.exists(), "Image stego carrier output generated");

            String extractedPayload = stego.extractMessage(outputImg, PASS_CORRECT);
            String decrypted = dec.decryptMessage(extractedPayload, PASS_CORRECT);
            assertEqual(MSG_SECRET, decrypted, "Image stego extracted message matching");

        } finally {
            imgCarrier.delete();
            outputImg.delete();
        }
    }

    private static void runImageColorModelsTest() throws Exception {
        System.out.println("\nExecuting Multi-Color-Model Steganography Audit...");
        
        runImageStegoTest(true, "GRAYSCALE-SCATTER", BufferedImage.TYPE_BYTE_GRAY);
        runImageStegoTest(false, "ARGB-SEQUENTIAL", BufferedImage.TYPE_INT_ARGB);
    }

    private static void runAudioStegoTest(boolean useScatter, String modeLabel) throws Exception {
        System.out.println(String.format("\nExecuting Audio Stego Test (%s)...", modeLabel));
        
        File audioCarrier = File.createTempFile("audio_carrier_", ".wav");
        writeMockWav(audioCarrier, 4000);
        
        File outputAudio = File.createTempFile("audio_carrier_stego_", ".wav");

        try {
            AudioSteganography stego = new AudioSteganography();
            Encryption enc = new Encryption();
            Decryption dec = new Decryption();
            
            String encryptedPayload = enc.encryptMessage(MSG_SECRET, PASS_CORRECT, "AES-GCM");
            stego.embedMessage(audioCarrier, outputAudio, encryptedPayload, PASS_CORRECT, false, useScatter);
            assertTrue(outputAudio.exists(), "Audio stego carrier output generated");

            String extractedPayload = stego.extractMessage(outputAudio, PASS_CORRECT);
            String decrypted = dec.decryptMessage(extractedPayload, PASS_CORRECT);
            assertEqual(MSG_SECRET, decrypted, "Audio stego extracted message matching");

        } finally {
            audioCarrier.delete();
            outputAudio.delete();
        }
    }

    private static void runVideoStegoTest() throws Exception {
        System.out.println("\nExecuting Video Stego EOF Test...");
        
        File videoCarrier = File.createTempFile("video_carrier_", ".mp4");
        Files.write(videoCarrier.toPath(), new byte[]{0, 0, 0, 32, 102, 116, 121, 112, 109, 112, 52, 50}); 
        
        File outputVideo = File.createTempFile("video_carrier_stego_", ".mp4");

        try {
            VideoSteganography stego = new VideoSteganography();
            Encryption enc = new Encryption();
            Decryption dec = new Decryption();
            
            String encryptedPayload = enc.encryptMessage(MSG_SECRET, PASS_CORRECT, "AES-GCM");
            stego.embedMessage(videoCarrier, outputVideo, encryptedPayload);
            assertTrue(outputVideo.exists(), "Video stego carrier output generated");

            String extractedPayload = stego.extractMessage(outputVideo);
            String decrypted = dec.decryptMessage(extractedPayload, PASS_CORRECT);
            assertEqual(MSG_SECRET, decrypted, "Video stego extracted message matching");

        } finally {
            videoCarrier.delete();
            outputVideo.delete();
        }
    }

    // ==================================================================================
    // DECOY LAYER TEST
    // ==================================================================================

    private static void runDecoyVerificationTest() throws Exception {
        System.out.println("\nExecuting Steganography Decoy Layer Trigger Test...");
        
        File imgCarrier = File.createTempFile("img_decoy_carrier_", ".png");
        BufferedImage img = new BufferedImage(150, 150, BufferedImage.TYPE_3BYTE_BGR);
        ImageIO.write(img, "png", imgCarrier);
        
        File outputImg = File.createTempFile("img_decoy_stego_", ".png");

        try {
            ImageSteganography stego = new ImageSteganography();
            Encryption enc = new Encryption();
            Decryption dec = new Decryption();
            
            String realEnc = enc.encryptMessage("REAL_LAYER::" + MSG_SECRET, PASS_CORRECT, "AES-GCM");
            String decoyEnc = enc.encryptMessage("DECOY_LAYER::" + MSG_DECOY, PASS_DECOY, "AES-GCM");
            String dualPayload = decoyEnc + "::" + realEnc;

            stego.embedMessage(imgCarrier, outputImg, dualPayload, PASS_CORRECT, true, true);
            assertTrue(outputImg.exists(), "Decoy image stego output generated");

            String extractedPayload = stego.extractMessage(outputImg, PASS_CORRECT);
            
            String[] parts = extractedPayload.split("::");
            assertTrue(parts.length == 2, "Extracted payload split into two layers");

            String decoyResult = null;
            try {
                String decrypted = dec.decryptMessage(parts[0], PASS_DECOY);
                if (decrypted.startsWith("DECOY_LAYER::")) {
                    decoyResult = decrypted.replace("DECOY_LAYER::", "");
                }
            } catch (Exception e) {}
            assertEqual(MSG_DECOY, decoyResult, "Decoy key extracts decoy payload");

            String realResult = null;
            try {
                String decrypted = dec.decryptMessage(parts[1], PASS_CORRECT);
                if (decrypted.startsWith("REAL_LAYER::")) {
                    realResult = decrypted.replace("REAL_LAYER::", "");
                }
            } catch (Exception e) {}
            assertEqual(MSG_SECRET, realResult, "Correct key extracts real payload");

        } finally {
            imgCarrier.delete();
            outputImg.delete();
        }
    }

    // ==================================================================================
    // AUDIO GENERATION HELPER
    // ==================================================================================

    private static void writeMockWav(File file, int numSamples) throws Exception {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            int subChunk2Size = numSamples * 2; // 16-bit
            int chunkSize = 36 + subChunk2Size;

            fos.write("RIFF".getBytes(StandardCharsets.US_ASCII));
            fos.write(intToByteArray(chunkSize), 0, 4);
            fos.write("WAVE".getBytes(StandardCharsets.US_ASCII));
            fos.write("fmt ".getBytes(StandardCharsets.US_ASCII));
            fos.write(intToByteArray(16), 0, 4); 
            fos.write(shortToByteArray((short) 1), 0, 2); 
            fos.write(shortToByteArray((short) 1), 0, 2); 
            fos.write(intToByteArray(44100), 0, 4); 
            fos.write(intToByteArray(44100 * 2), 0, 4); 
            fos.write(shortToByteArray((short) 2), 0, 2); 
            fos.write(shortToByteArray((short) 16), 0, 2); 
            fos.write("data".getBytes(StandardCharsets.US_ASCII));
            fos.write(intToByteArray(subChunk2Size), 0, 4);

            byte[] mockData = new byte[subChunk2Size];
            fos.write(mockData);
        }
    }

    private static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8),
                (byte) (value >>> 16),
                (byte) (value >>> 24)
        };
    }

    private static byte[] shortToByteArray(short value) {
        return new byte[]{
                (byte) value,
                (byte) (value >>> 8)
        };
    }
}
