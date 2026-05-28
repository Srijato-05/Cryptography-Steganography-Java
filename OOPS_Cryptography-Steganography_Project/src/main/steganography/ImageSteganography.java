package src.main.steganography;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Image Steganography Engine.
 * Supports both Seeded Scatter LSB (fisher-yates) and Sequential LSB layout modes.
 */
public class ImageSteganography {

    // ==================================================================================
    // EMBEDDING LOGIC
    // ==================================================================================

    public void embedMessage(File sourceFile, File destFile, String message, String password, boolean useDecoy, boolean useScatter) throws Exception {
        // 1. Load Image and convert to standard byte format
        BufferedImage image = ImageIO.read(sourceFile);
        BufferedImage userImage = getImageToEmbed(image);

        WritableRaster raster = userImage.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        byte[] imgData = buffer.getData();

        // 2. Prepare Payload
        byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
        byte[] lengthBytes = intToBytes(messageBytes.length);

        // 3. Capacity Check (2 flag bits + 32 length header bits + payload bits)
        int totalRequiredBits = 2 + 32 + messageBytes.length * 8;
        if (totalRequiredBits > imgData.length) {
            throw new Exception("Payload exceeds image capacity. Need " + totalRequiredBits + " pixels.");
        }

        // 4. Set Decoy Flag at index 0 and Scatter Flag at index 1
        imgData[0] = (byte) ((imgData[0] & 0xFE) | (useDecoy ? 1 : 0));
        imgData[1] = (byte) ((imgData[1] & 0xFE) | (useScatter ? 1 : 0));

        // 5. Embed Length Header (pixels 2 to 33)
        int imgOffset = 2;
        for (byte b : lengthBytes) {
            for (int i = 7; i >= 0; i--) {
                int bit = (b >>> i) & 1;
                imgData[imgOffset] = (byte) ((imgData[imgOffset] & 0xFE) | bit);
                imgOffset++;
            }
        }

        // 6. Embed Payload
        if (useScatter) {
            // Seeded PRNG Scatter Mode
            SecureRandom prng = SecureRandom.getInstance("SHA1PRNG");
            if (useDecoy) {
                prng.setSeed("DECOY_PROTOCOL_SEED".getBytes(StandardCharsets.UTF_8));
            } else {
                prng.setSeed(password.getBytes(StandardCharsets.UTF_8));
            }

            int N = imgData.length - 34; // available indices starting at 34
            int K = messageBytes.length * 8;

            java.util.Map<Integer, Integer> map = new java.util.HashMap<>();
            int[] selectedIndices = new int[K];
            for (int i = 0; i < K; i++) {
                int rand = i + prng.nextInt(N - i);
                int valI = map.getOrDefault(i, i);
                int valRand = map.getOrDefault(rand, rand);
                map.put(rand, valI);
                selectedIndices[i] = valRand + 34; // offset of 34
            }

            int bitIndex = 0;
            for (byte b : messageBytes) {
                for (int i = 7; i >= 0; i--) {
                    int bit = (b >>> i) & 1;
                    int targetPixel = selectedIndices[bitIndex++];
                    imgData[targetPixel] = (byte) ((imgData[targetPixel] & 0xFE) | bit);
                }
            }
        } else {
            // Sequential Mode starting at index 34
            int payloadOffset = 34;
            for (byte b : messageBytes) {
                for (int i = 7; i >= 0; i--) {
                    int bit = (b >>> i) & 1;
                    imgData[payloadOffset] = (byte) ((imgData[payloadOffset] & 0xFE) | bit);
                    payloadOffset++;
                }
            }
        }

        // 7. Save as Lossless PNG
        ImageIO.write(userImage, "png", destFile);
    }

    // ==================================================================================
    // EXTRACTION LOGIC
    // ==================================================================================

    public String extractMessage(File sourceFile, String password) throws Exception {
        BufferedImage image = ImageIO.read(sourceFile);
        BufferedImage userImage = getImageToEmbed(image);

        WritableRaster raster = userImage.getRaster();
        DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
        byte[] imgData = buffer.getData();

        // 1. Read Decoy and Scatter Flags
        int decoyFlag = imgData[0] & 1;
        int scatterFlag = imgData[1] & 1;
        boolean isDecoy = (decoyFlag == 1);
        boolean isScatter = (scatterFlag == 1);

        // 2. Extract Length Header (pixels 2 to 33)
        byte[] lengthBytes = new byte[4];
        int imgOffset = 2;
        for (int i = 0; i < 4; i++) {
            for (int bit = 7; bit >= 0; bit--) {
                int lsb = imgData[imgOffset] & 1;
                lengthBytes[i] = (byte) ((lengthBytes[i] | (lsb << bit)));
                imgOffset++;
            }
        }
        int messageLength = bytesToInt(lengthBytes);

        // Capacity sanity check
        int N = imgData.length - 34;
        if (messageLength < 0 || (messageLength * 8) > N) {
            throw new Exception("Invalid Message Length. Wrong key or corrupted data.");
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
            int[] selectedIndices = new int[K];
            for (int i = 0; i < K; i++) {
                int rand = i + prng.nextInt(N - i);
                int valI = map.getOrDefault(i, i);
                int valRand = map.getOrDefault(rand, rand);
                map.put(rand, valI);
                selectedIndices[i] = valRand + 34;
            }

            int bitIndex = 0;
            for (int i = 0; i < messageLength; i++) {
                for (int bit = 7; bit >= 0; bit--) {
                    int targetPixel = selectedIndices[bitIndex++];
                    int lsb = imgData[targetPixel] & 1;
                    messageBytes[i] = (byte) ((messageBytes[i] | (lsb << bit)));
                }
            }
        } else {
            // Sequential Extraction
            int payloadOffset = 34;
            for (int i = 0; i < messageLength; i++) {
                for (int bit = 7; bit >= 0; bit--) {
                    int lsb = imgData[payloadOffset] & 1;
                    messageBytes[i] = (byte) ((messageBytes[i] | (lsb << bit)));
                    payloadOffset++;
                }
            }
        }

        return new String(messageBytes, StandardCharsets.UTF_8);
    }

    // ==================================================================================
    // HELPER METHODS
    // ==================================================================================

    private BufferedImage getImageToEmbed(BufferedImage original) {
        BufferedImage image = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
        image.getGraphics().drawImage(original, 0, 0, null);
        return image;
    }

    private byte[] intToBytes(int i) {
        return new byte[]{ (byte)(i >> 24), (byte)(i >> 16), (byte)(i >> 8), (byte)i };
    }

    private int bytesToInt(byte[] b) {
        return ((b[0] & 0xFF) << 24) | ((b[1] & 0xFF) << 16) | ((b[2] & 0xFF) << 8) | (b[3] & 0xFF);
    }
}