package src.main;

import src.main.controller.MainController;
import src.main.ui.AppUI;
import src.main.utils.Config;
import src.main.utils.ExceptionHandler;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
 * Main Application Entry Point.
 * Coordinates views, events, and steganography routes.
 */
public class App {

    private final AppUI view;
    private final MainController controller;

    public App() {
        // 1. Initialize View
        this.view = new AppUI();

        // 2. Initialize Controller (Passing View for Logging)
        this.controller = new MainController(view);

        // 3. Visual Boot Sequence
        view.log("BOOT SEQUENCE INITIATED...");
        view.log("LOADING CORE MODULES: AES-GCM, CHACHA20, CBC, ZERO-WIDTH, LSB-ENGINE");
        view.log("SYSTEM READY. LOAD A CARRIER FILE TO BEGIN.");

        // 4. Bind Events & Show
        bindEvents();
        view.setVisible(true);
    }

    public static void main(String[] args) {
        // Ensure Thread Safety
        SwingUtilities.invokeLater(App::new);
    }

    private void bindEvents() {
        // ==================================================================
        // CRYPTOGRAPHY HANDLERS
        // ==================================================================

        view.getEncryptBtn().addActionListener(e -> {
            if (isAuthMissing()) return;

            File src = getFileFromViewOrChooser("SELECT TARGET FOR ENCRYPTION", null);
            if (src == null) return;

            // Allow MIME-Masking (any extension)
            File dest = controller.showSaveDialog(view, "encrypted_artifact", Config.DESC_ENCRYPTED, Config.EXT_ENCRYPTED);
            if (dest == null) return;

            String algo = view.getSelectedCryptoAlgo();
            executeTask(() -> controller.encryptFile(src, dest, view.getPassword(), algo));
        });

        view.getDecryptBtn().addActionListener(e -> {
            if (isAuthMissing()) return;

            File src = getFileFromViewOrChooser("SELECT ENCRYPTED ARTIFACT", new String[]{Config.EXT_ENCRYPTED});
            if (src == null) return;

            // Decryption output can be anything
            File dest = controller.showSaveDialog(view, "decrypted_file", "All Files", "");
            if (dest == null) return;

            executeTask(() -> controller.decryptFile(src, dest, view.getPassword()));
        });

        // ==================================================================
        // STEGANOGRAPHY HANDLERS (With Decoy & Scatter Options)
        // ==================================================================

        // --- IMAGE ---
        view.getHideImgBtn().addActionListener(e -> handleStegoEmbed("image", Config.EXT_IMAGES));
        view.getExtractImgBtn().addActionListener(e -> handleStegoExtract("image", Config.EXT_IMAGES));

        // --- AUDIO ---
        view.getHideAudBtn().addActionListener(e -> handleStegoEmbed("audio", new String[]{Config.EXT_AUDIO_WAV}));
        view.getExtractAudBtn().addActionListener(e -> handleStegoExtract("audio", new String[]{Config.EXT_AUDIO_WAV}));

        // --- VIDEO ---
        view.getHideVidBtn().addActionListener(e -> handleStegoEmbed("video", Config.EXT_VIDEOS));
        view.getExtractVidBtn().addActionListener(e -> handleStegoExtract("video", Config.EXT_VIDEOS));

        // --- TEXT ---
        view.getHideTxtBtn().addActionListener(e -> handleStegoEmbed("text", new String[]{"txt"}));
        view.getExtractTxtBtn().addActionListener(e -> handleStegoExtract("text", new String[]{"txt"}));
    }

    // ==================================================================
    // UNIFIED LOGIC HANDLERS
    // ==================================================================

    private void handleStegoEmbed(String type, String[] extensions) {
        if (isAuthMissing()) return;

        // Payload Check
        if (view.getMessage().isEmpty()) {
            view.log("ERROR: PAYLOAD DATA MISSING. ABORTING INJECTION.");
            JOptionPane.showMessageDialog(view, "Payload Data Required.", "INPUT ERROR", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File src = getFileFromViewOrChooser("SELECT CARRIER " + type.toUpperCase(), extensions);
        if (src == null) return;

        String defaultExt = extensions[0];
        File dest = controller.showSaveDialog(view, "stego_" + type, type.toUpperCase(), defaultExt);
        if (dest == null) return;

        // Decoy Check, layout mode & chosen cryptographic algorithm
        boolean useDecoy = view.isDecoyEnabled();
        boolean useScatter = view.isScatterEnabled();
        String algo = view.getSelectedCryptoAlgo();
        String pass = view.getPassword();
        String msg = view.getMessage();

        executeTask(() -> {
            switch (type) {
                case "image" -> controller.embedInImage(src, dest, msg, pass, useDecoy, useScatter, algo);
                case "audio" -> controller.embedInAudio(src, dest, msg, pass, useDecoy, useScatter, algo);
                case "video" -> controller.embedInVideo(src, dest, msg, pass, algo);
                case "text" -> controller.embedInText(src, dest, msg, pass, useDecoy, algo);
            }
        });
    }

    private void handleStegoExtract(String type, String[] extensions) {
        if (isAuthMissing()) return;

        File src = getFileFromViewOrChooser("SELECT STEGO " + type.toUpperCase(), extensions);
        if (src == null) return;

        executeTask(() -> {
            String result = null;
            String pass = view.getPassword();

            switch (type) {
                case "image" -> result = controller.extractFromImage(src, pass);
                case "audio" -> result = controller.extractFromAudio(src, pass);
                case "video" -> result = controller.extractFromVideo(src, pass);
                case "text" -> result = controller.extractFromText(src, pass);
            }

            if (result != null) {
                String finalResult = result;
                SwingUtilities.invokeLater(() -> showResultDialog(finalResult));
            } else {
                view.log("EXTRACTION FINISHED: NO DATA OR WRONG KEY.");
            }
        });
    }

    // ==================================================================
    // UTILITIES
    // ==================================================================

    private File getFileFromViewOrChooser(String title, String[] extensions) {
        File dropped = view.getDroppedFile();
        if (dropped != null) {
            int c = JOptionPane.showConfirmDialog(view,
                    "USE DETECTED FILE: " + dropped.getName() + "?",
                    "FILE UPLOAD DETECTED", JOptionPane.YES_NO_OPTION);

            view.clearDroppedFile(); // Clear after decision
            if (c == JOptionPane.YES_OPTION) {
                view.log("USING DROPPED FILE: " + dropped.getName());
                return dropped;
            }
        }
        return controller.showOpenDialog(view, title, extensions);
    }

    private void executeTask(Runnable task) {
        view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() {
                try {
                    task.run();
                } catch (Exception e) {
                    view.log("CRITICAL FAILURE: " + e.getMessage());
                    ExceptionHandler.handle(e, "OPERATION FAILED");
                }
                return null;
            }

            @Override
            protected void done() {
                view.setCursor(Cursor.getDefaultCursor());
                view.log("PROCESS THREAD RELEASED.");
            }
        }.execute();
    }

    private boolean isAuthMissing() {
        if (view.getPassword().isEmpty()) {
            view.log("ACCESS DENIED: AUTH KEY IS MANDATORY.");
            JOptionPane.showMessageDialog(view, "AUTH KEY REQUIRED.", "ACCESS DENIED", JOptionPane.ERROR_MESSAGE);
            return true;
        }
        return false;
    }

    private void showResultDialog(String msg) {
        view.log("PAYLOAD DECRYPTED SUCCESSFULLY.");

        JTextArea area = new JTextArea(msg);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Config.FONT_MONO);
        area.setForeground(Config.NEON_CYAN);
        area.setBackground(Config.DARK_PURPLE);

        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(500, 300));
        scroll.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN));

        JOptionPane.showMessageDialog(view, scroll, "DECRYPTED INTELLIGENCE", JOptionPane.INFORMATION_MESSAGE);
    }
}