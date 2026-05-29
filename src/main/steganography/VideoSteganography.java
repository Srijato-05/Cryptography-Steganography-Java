package src.main.steganography;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Video Steganography Engine.
 * Implements EOF (End-Of-File) Injection.
 * * LOGIC:
 * 1. Finds the end of the original video file.
 * 2. Appends a unique SIGNATURE string.
 * 3. Appends the Encrypted Payload.
 * * ADVANTAGE:
 * - Does not corrupt video playback (Players ignore data after EOF).
 * - Extremely fast compared to re-encoding video.
 */
public class VideoSteganography {

    // Unique signature to locate our data later
    private static final String EOF_SIGNATURE = "####PROJECT_HERMES_EOF####";

    // ==================================================================================
    // EMBEDDING LOGIC
    // ==================================================================================

    public void embedMessage(File sourceFile, File destFile, String message) throws Exception {
        // 1. Prepare Data
        byte[] videoBytes = readFile(sourceFile);
        byte[] signatureBytes = EOF_SIGNATURE.getBytes(StandardCharsets.UTF_8);
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

        // 2. Combine: [VIDEO] + [SIGNATURE] + [MESSAGE]
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            // Write original video
            fos.write(videoBytes);
            // Write Signature (Delimiter)
            fos.write(signatureBytes);
            // Write Payload
            fos.write(messageBytes);
        }
    }

    // ==================================================================================
    // EXTRACTION LOGIC
    // ==================================================================================

    public String extractMessage(File sourceFile) throws Exception {
        byte[] fileBytes = readFile(sourceFile);
        byte[] signatureBytes = EOF_SIGNATURE.getBytes(StandardCharsets.UTF_8);

        // 1. Find the Signature Index (Scan from end to optimize speed)
        int splitIndex = findSignatureIndex(fileBytes, signatureBytes);

        if (splitIndex == -1) {
            throw new Exception("No hidden data signature found in this video.");
        }

        // 2. Extract everything AFTER the signature
        int payloadStart = splitIndex + signatureBytes.length;
        int payloadLength = fileBytes.length - payloadStart;

        if (payloadLength <= 0) {
            return ""; // Signature found, but empty message
        }

        byte[] messageBytes = Arrays.copyOfRange(fileBytes, payloadStart, fileBytes.length);
        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    // ==================================================================================
    // UTILITIES
    // ==================================================================================

    /**
     * Scans the byte array to find the starting index of the Signature.
     * Uses a reverse scan (end-to-start) because EOF data is usually at the end.
     */
    private int findSignatureIndex(byte[] data, byte[] signature) {
        // Naive search (Knuth-Morris-Pratt is overkill for this size)
        for (int i = data.length - signature.length; i >= 0; i--) {
            boolean match = true;
            for (int j = 0; j < signature.length; j++) {
                if (data[i + j] != signature[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return i;
            }
        }
        return -1; // Not found
    }

    private byte[] readFile(File file) throws IOException {
        // Warning: Java arrays are max 2GB. For 4K videos, use RandomAccessFile instead.
        // For B.Tech Project level, standard IO is acceptable.
        if (file.length() > Integer.MAX_VALUE) {
            throw new IOException("File too large (Max 2GB supported in this version).");
        }
        byte[] data = new byte[(int) file.length()];
        try (FileInputStream fis = new FileInputStream(file)) {
            int bytesRead = 0;
            while (bytesRead < data.length) {
                int read = fis.read(data, bytesRead, data.length - bytesRead);
                if (read == -1) break;
                bytesRead += read;
            }
        }
        return data;
    }
}