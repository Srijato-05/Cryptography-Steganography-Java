package src.main.utils;

import java.awt.*;

public class Config {
    // ==================================================================================
    // BRANDING
    // ==================================================================================
    public static final String APP_TITLE = "SECURE-STEGO // ADVANCED SUITE";
    public static final String APP_VERSION = "v3.0.0 (Ultimate)";

    // ==================================================================================
    // CYBERSECURITY COLOR PALETTE
    // ==================================================================================
    public static final Color NEON_CYAN = new Color(0, 255, 255);
    public static final Color NEON_GREEN = new Color(0, 255, 100); // For Success Logs
    public static final Color NEON_YELLOW = new Color(255, 200, 0); // For Warnings
    public static final Color NEON_RED = new Color(255, 50, 50);   // For Critical Errors

    public static final Color DARK_PURPLE = new Color(15, 15, 25);
    public static final Color TERMINAL_BG = new Color(10, 10, 10); // Log Background
    public static final Color GLASS_BG = new Color(20, 22, 33, 110);
    public static final Color BORDER_GLOW = new Color(0, 255, 255, 100);
    public static final Color TEXT_PRIMARY = new Color(240, 240, 240);

    // Fallback/Standard Colors
    public static final Color COLOR_BACKGROUND = new Color(30, 30, 40);
    public static final Color COLOR_PANEL = new Color(45, 45, 60, 220);
    public static final Color COLOR_ACCENT = NEON_CYAN;
    public static final Color COLOR_TEXT = TEXT_PRIMARY;
    public static final Color COLOR_BUTTON_BG = new Color(60, 60, 80);

    // ==================================================================================
    // FONTS
    // ==================================================================================
    public static final Font FONT_HEADER = new Font("OCR A Extended", Font.BOLD, 22);
    public static final Font FONT_MAIN = new Font("Consolas", Font.BOLD, 14);
    public static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_TERMINAL = new Font("Lucida Console", Font.PLAIN, 11); // New Terminal Font

    // ==================================================================================
    // FILE EXTENSIONS
    // ==================================================================================

    // Cryptography
    public static final String EXT_ENCRYPTED = "enc";
    public static final String DESC_ENCRYPTED = "Encrypted Data (*.enc)";

    // Audio
    public static final String EXT_AUDIO_WAV = "wav";

    // Collections
    public static final String[] EXT_IMAGES = {"png", "jpg", "jpeg", "bmp"};
    public static final String[] EXT_VIDEOS = {"mp4", "mkv", "avi"};

    private Config() {
        // Prevent instantiation
    }
}