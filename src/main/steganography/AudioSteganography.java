package src.main.steganography;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Enhanced Audio Steganography Engine.
 * Supports LSB modification on PCM WAVE files.
 * Supports both Seeded Scatter LSB and Sequential LSB layout modes.
 */
public class AudioSteganography {

    private static final int WAV_HEADER_SIZE = 44;

    public void embedMessage(File sourceFile, File destFile, String message, String password, boolean useDecoy, boolean useScatter) throws Exception {
        // 1. Read All Bytes
        byte[] audioBytes = readFile(sourceFile);

        // 2. Prepare Payload
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] lengthBytes = intToBytes(messageBytes.length);

        // 3. Capacity Check
        int dataAreaSize = audioBytes.length - WAV_HEADER_SIZE;
        int requiredBits = 2 + 32 + messageBytes.length * 8; // 2 flags + 32 header + payload

        if (requiredBits > dataAreaSize) {
            throw new Exception("Audio file too short. Need " + requiredBits + " samples, have " + dataAreaSize);
        }

        // 4. Set Flags at byte 44 (index 0 of data area) and byte 45 (index 1 of data area)
        audioBytes[WAV_HEADER_SIZE] = (byte) ((audioBytes[WAV_HEADER_SIZE] & 0xFE) | (useDecoy ? 1 : 0));
        audioBytes[WAV_HEADER_SIZE + 1] = (byte) ((audioBytes[WAV_HEADER_SIZE + 1] & 0xFE) | (useScatter ? 1 : 0));

        // 5. Embed Length Header (sequential, bytes index 2 to 33 relative to data area, i.e., bytes 46 to 77)
        int audioIndex = WAV_HEADER_SIZE + 2;
        for (byte b : lengthBytes) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >>> i) & 1;
                audioBytes[audioIndex] = (byte) ((audioBytes[audioIndex] & 0xFE) | bit);
                audioIndex++;
            }
        }

        // 6. Embed Payload
        if (useScatter) {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            if (useDecoy) {
                prng.setSeed("DECOY_PROTOCOL_SEED".getBytes(StandardCharsets.UTF_8));
            } else {
                prng.setSeed(password.getBytes(StandardCharsets.UTF_8));
            }

            int N = dataAreaSize - 34; // available indices starting at index 34
            int K = messageBytes.length * 8;

            java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
            int[] selectedOffsets = new int[K];
            for (int i = 0; i < K; i++) {
                int rand = i + prng.nextInt(N - i);
                int valI = map.getOrDefault(i, i);
                int valRand = map.getOrDefault(rand, rand);
                map.put(rand, valI);
                selectedOffsets[i] = valRand + 34;
            }

            int bitIndex = 0;
            for (byte b : messageBytes) {
                for (int i = 7; i >= 0; i--) {
                    int bit = (b >>> i) & 1;
                    int actualIndex = WAV_HEADER_SIZE + selectedOffsets[bitIndex++];
                    audioBytes[actualIndex] = (byte) ((audioBytes[actualIndex] & 0xFE) | bit);
                }
            }
        } else {
            // Sequential Embed starting at index 34
            int payloadOffset = WAV_HEADER_SIZE + 34;
            for (byte b : messageBytes) {
                for (int i = 7; i >= 0; i--) {
                    int bit = (b >>> i) & 1;
                    audioBytes[payloadOffset] = (byte) ((audioBytes[payloadOffset] & 0xFE) | bit);
                    payloadOffset++;
                }
            }
        }

        // 7. Save File
        writeFile(destFile, audioBytes);
    }

    public String extractMessage(File sourceFile, String password) throws Exception {
        byte[] audioBytes = readFile(sourceFile);
        int dataAreaSize = audioBytes.length - WAV_HEADER_SIZE;

        // 1. Read Flags
        int decoyFlag = audioBytes[WAV_HEADER_SIZE] & 1;
        int scatterFlag = audioBytes[WAV_HEADER_SIZE + 1] & 1;
        boolean isDecoy = (decoyFlag == 1);
        boolean isScatter = (scatterFlag == 1);

        // 2. Extract Length Header (sequential, bytes 46 to 77)
        byte[] lengthBytes = new byte[4];
        int audioIndex = WAV_HEADER_SIZE + 2;

        for (int i = 0; i < 4; i++) {
            for (int bit = 7; bit >= 0; bit--) {
                int lsb = audioBytes[audioIndex] & 1;
                lengthBytes[i] = (byte) ((lengthBytes[i] | (lsb << bit)));
                audioIndex++;
            }
        }
        int messageLength = bytesToInt(lengthBytes);

        // Capacity sanity check
        int N = dataAreaSize - 34;
        if (messageLength < 0 || (messageLength * 8) > N) {
            throw new Exception("Invalid Message Length (Possible Wrong Password).");
        }

        // 3. Extract Message Bytes
        byte[] messageBytes = new byte[messageLength];
        if (isScatter) {
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            if (isDecoy) {
                prng.setSeed("DECOY_PROTOCOL_SEED".getBytes(StandardCharsets.UTF_8));
            } else {
                prng.setSeed(password.getBytes(StandardCharsets.UTF_8));
            }

            int K = messageLength * 8;
            java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
            int[] selectedOffsets = new int[K];
            for (int i = 0; i < K; i++) {
                int rand = i + prng.nextInt(N - i);
                int valI = map.getOrDefault(i, i);
                int valRand = map.getOrDefault(rand, rand);
                map.put(rand, valI);
                selectedOffsets[i] = valRand + 34;
            }

            int bitIndex = 0;
            for (int i = 0; i < messageLength; i++) {
                for (int bit = 7; bit >= 0; bit--) {
                    int actualIndex = WAV_HEADER_SIZE + selectedOffsets[bitIndex++];
                    int lsb = audioBytes[actualIndex] & 1;
                    messageBytes[i] = (byte) ((messageBytes[i] | (lsb << bit)));
                }
            }
        } else {
            // Sequential Extraction
            int payloadOffset = WAV_HEADER_SIZE + 34;
            for (int i = 0; i < messageLength; i++) {
                for (int bit = 7; bit >= 0; bit--) {
                    int lsb = audioBytes[payloadOffset] & 1;
                    messageBytes[i] = (byte) ((messageBytes[i] | (lsb << bit)));
                    payloadOffset++;
                }
            }
        }

        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    private byte[] readFile(File file) throws IOException {
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

    private void writeFile(File file, byte[] data) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(data);
        }
    }

    private byte[] intToBytes(int i) {
        return new byte[]{ (byte)(i >> 24), (byte)(i >> 16), (byte)(i >> 8), (byte)i };
    }

    private int bytesToInt(byte[] b) {
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }
}