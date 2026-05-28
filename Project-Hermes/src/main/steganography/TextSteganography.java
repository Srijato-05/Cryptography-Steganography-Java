package src.main.steganography;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Text Steganography Engine.
 * Hides Base64 encrypted messages inside text files using Zero-Width characters.
 * - '\u200B' (Zero-Width Space) represents bit 0.
 * - '\u200C' (Zero-Width Non-Joiner) represents bit 1.
 */
public class TextSteganography {

    private static final char ZERO = '\u200B';
    private static final char ONE = '\u200C';

    public void embedMessage(File sourceFile, File destFile, String base64Message) throws Exception {
        String coverText = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);

        StringBuilder hidden = new StringBuilder();
        // Convert Base64 payload characters to bit representations
        for (char c : base64Message.toCharArray()) {
            for (int i = 7; i >= 0; i--) {
                int bit = (c >>> i) & 1;
                if (bit == 0) {
                    hidden.append(ZERO);
                } else {
                    hidden.append(ONE);
                }
            }
        }

        // Append the hidden string at the end of the cover text
        String stegoText = coverText + hidden.toString();

        Files.writeString(destFile.toPath(), stegoText, StandardCharsets.UTF_8);
    }

    public String extractMessage(File sourceFile) throws Exception {
        String stegoText = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);

        StringBuilder bits = new StringBuilder();
        for (char c : stegoText.toCharArray()) {
            if (c == ZERO) {
                bits.append('0');
            } else if (c == ONE) {
                bits.append('1');
            }
        }

        String bitStr = bits.toString();
        if (bitStr.isEmpty()) {
            throw new Exception("No hidden text message found.");
        }

        // Align to 8 bits (drop trailing bits if corrupted, but should match 8-bit blocks)
        int cleanLen = (bitStr.length() / 8) * 8;
        if (cleanLen == 0) {
            throw new Exception("No valid hidden message blocks found.");
        }

        StringBuilder decoded = new StringBuilder();
        for (int i = 0; i < cleanLen; i += 8) {
            String byteStr = bitStr.substring(i, i + 8);
            int charCode = Integer.parseInt(byteStr, 2);
            decoded.append((char) charCode);
        }

        return decoded.toString();
    }
}
