package src.main.controller;

import src.main.encryption.Decryption;
import src.main.encryption.Encryption;
import src.main.steganography.AudioSteganography;
import src.main.steganography.ImageSteganography;
import src.main.steganography.TextSteganography;
import src.main.steganography.VideoSteganography;
import src.main.ui.AppUI;
import src.main.utils.ExceptionHandler;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;

/**
 * Main Controller.
 * Coordinates Cryptography and Steganography Operations.
 */
public class MainController {

    private final AppUI view;
    private final Encryption encryption;
    private final Decryption decryption;
    private final ImageSteganography imageStego;
    private final AudioSteganography audioStego;
    private final VideoSteganography videoStego;
    private final TextSteganography textStego;

    private File lastSelectedDirectory;

    public MainController(AppUI view) {
        this.view = view;
        this.encryption = new Encryption();
        this.decryption = new Decryption();
        this.imageStego = new ImageSteganography();
        this.audioStego = new AudioSteganography();
        this.videoStego = new VideoSteganography();
        this.textStego = new TextSteganography();

        this.lastSelectedDirectory = new File(System.getProperty("user.home"));
    }

    // ==================================================================================
    // STANDALONE CRYPTOGRAPHY OPERATIONS
    // ==================================================================================

    public void encryptFile(File source, File destination, String password, String cryptoAlgo) {
        try {
            view.log("INITIATING " + cryptoAlgo + " ENCRYPTION PROTOCOL...");
            view.log("SOURCE: " + source.getName() + " | SIZE: " + source.length() + " BYTES");

            long startTime = System.currentTimeMillis();
            encryption.encryptFile(source, destination, password, cryptoAlgo);
            long duration = System.currentTimeMillis() - startTime;

            view.log("ENCRYPTION COMPLETE IN " + duration + "MS.");
            view.log("ARTIFACT GENERATED: " + destination.getName());

            JOptionPane.showMessageDialog(view, "Target Encrypted Successfully.", "Project Hermes", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            view.log("CRITICAL ERROR: ENCRYPTION FAILED.");
            ExceptionHandler.handle(e, "Encryption Error");
        }
    }

    public void decryptFile(File source, File destination, String password) {
        try {
            view.log("ATTEMPTING DECRYPTION ON: " + source.getName());
            view.log("VERIFYING CRYPTO HEADERS...");

            long startTime = System.currentTimeMillis();
            decryption.decryptFile(source, destination, password);
            long duration = System.currentTimeMillis() - startTime;

            view.log("ACCESS GRANTED. FILE RESTORED IN " + duration + "MS.");
            JOptionPane.showMessageDialog(view, "Target Decrypted Successfully.", "Project Hermes", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            view.log("ACCESS DENIED: DECRYPTION FAILED (INVALID KEY OR CORRUPT FILE).");
            ExceptionHandler.handle(e, "Decryption Error");
        }
    }

    // ==================================================================================
    // STEGANOGRAPHY INJECTION
    // ==================================================================================

    public void embedInImage(File src, File dest, String msg, String pass, boolean useDecoy, boolean useScatter, String cryptoAlgo) {
        try {
            view.log("ANALYZING IMAGE CARRIER: " + src.getName());

            String payloadToHide = preparePayload(msg, pass, useDecoy, cryptoAlgo);
            if (payloadToHide == null) return;

            view.log("INJECTING VIA IMAGE LSB (SCATTER=" + useScatter + ")...");
            imageStego.embedMessage(src, dest, payloadToHide, pass, useDecoy, useScatter);

            view.log("STEGANOGRAPHY COMPLETE. OUTPUT: " + dest.getName());
            JOptionPane.showMessageDialog(view, "Secure Injection (Image) Complete.", "Project Hermes", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            view.log("ERROR: IMAGE INJECTION FAILED.");
            ExceptionHandler.handle(e, "Embed Error");
        }
    }

    public void embedInAudio(File src, File dest, String msg, String pass, boolean useDecoy, boolean useScatter, String cryptoAlgo) {
        try {
            view.log("ANALYZING AUDIO WAVEFORM: " + src.getName());

            String payloadToHide = preparePayload(msg, pass, useDecoy, cryptoAlgo);
            if (payloadToHide == null) return;

            view.log("INJECTING VIA AUDIO LSB (SCATTER=" + useScatter + ")...");
            audioStego.embedMessage(src, dest, payloadToHide, pass, useDecoy, useScatter);

            view.log("STEGANOGRAPHY COMPLETE. OUTPUT: " + dest.getName());
            JOptionPane.showMessageDialog(view, "Secure Injection (Audio) Complete.", "Project Hermes", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            view.log("ERROR: AUDIO INJECTION FAILED.");
            ExceptionHandler.handle(e, "Embed Error");
        }
    }

    public void embedInVideo(File src, File dest, String msg, String pass, String cryptoAlgo) {
        try {
            view.log("ANALYZING VIDEO CONTAINER: " + src.getName());

            view.log("ENCRYPTING PAYLOAD (" + cryptoAlgo + ")...");
            String securePayload = encryption.encryptMessage(msg, pass, cryptoAlgo);

            view.log("APPENDING DATA TO VIDEO EOF...");
            videoStego.embedMessage(src, dest, securePayload);

            view.log("STEGANOGRAPHY COMPLETE. OUTPUT: " + dest.getName());
            JOptionPane.showMessageDialog(view, "Secure Injection (Video EOF) Complete.", "Project Hermes", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            view.log("ERROR: VIDEO INJECTION FAILED.");
            ExceptionHandler.handle(e, "Embed Error");
        }
    }

    public void embedInText(File src, File dest, String msg, String pass, boolean useDecoy, String cryptoAlgo) {
        try {
            view.log("ANALYZING TEXT DOCUMENT: " + src.getName());

            String payloadToHide = preparePayload(msg, pass, useDecoy, cryptoAlgo);
            if (payloadToHide == null) return;

            view.log("INJECTING PAYLOAD VIA ZERO-WIDTH MODULATION...");
            textStego.embedMessage(src, dest, payloadToHide);

            view.log("STEGANOGRAPHY COMPLETE. OUTPUT: " + dest.getName());
            JOptionPane.showMessageDialog(view, "Secure Injection (Text Zero-Width) Complete.", "Project Hermes", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception e) {
            view.log("ERROR: TEXT INJECTION FAILED.");
            ExceptionHandler.handle(e, "Embed Error");
        }
    }

    // ==================================================================================
    // STEGANOGRAPHY EXTRACTION
    // ==================================================================================

    public String extractFromImage(File src, String pass) {
        try {
            view.log("SCANNING SCATTERED/SEQUENTIAL PIXELS...");
            String securePayload = imageStego.extractMessage(src, pass);
            view.log("DECRYPTING EXTRACTED STREAM...");
            return decryptDualLayer(securePayload, pass);
        } catch (Exception e) {
            view.log("ERROR: EXTRACTION FAILED (WRONG KEY OR NO DATA).");
            return null;
        }
    }

    public String extractFromAudio(File src, String pass) {
        try {
            view.log("SCANNING SCATTERED/SEQUENTIAL PCM SAMPLES...");
            String securePayload = audioStego.extractMessage(src, pass);
            view.log("DECRYPTING EXTRACTED STREAM...");
            return decryptDualLayer(securePayload, pass);
        } catch (Exception e) {
            view.log("ERROR: AUDIO EXTRACTION FAILED.");
            return null;
        }
    }

    public String extractFromVideo(File src, String pass) {
        try {
            view.log("SCANNING VIDEO EOF SIGNATURE...");
            String securePayload = videoStego.extractMessage(src);
            view.log("DECRYPTING PAYLOAD...");
            return decryption.decryptMessage(securePayload, pass);
        } catch (Exception e) {
            view.log("ERROR: VIDEO EXTRACTION FAILED.");
            return null;
        }
    }

    public String extractFromText(File src, String pass) {
        try {
            view.log("SCANNING ZERO-WIDTH CHARACTERS...");
            String securePayload = textStego.extractMessage(src);
            view.log("DECRYPTING PAYLOAD...");
            return decryptDualLayer(securePayload, pass);
        } catch (Exception e) {
            view.log("ERROR: TEXT EXTRACTION FAILED.");
            return null;
        }
    }

    // ==================================================================================
    // DECOY & DECRYPTION UTILITIES
    // ==================================================================================

    private String preparePayload(String msg, String pass, boolean useDecoy, String cryptoAlgo) throws Exception {
        if (useDecoy) {
            view.log("WARNING: DECOY PROTOCOL ACTIVE.");

            String decoyPass = JOptionPane.showInputDialog(view,
                "ENTER DECOY PASSPHRASE (DIFFERENT FROM AUTH KEY):",
                "DECOY CONFIGURATION", JOptionPane.QUESTION_MESSAGE);
            if (decoyPass == null || decoyPass.isEmpty() || decoyPass.equals(pass)) {
                view.log("DECOY ABORTED: INVALID OR MATCHING DECOY KEY.");
                return null;
            }

            String decoyMsg = JOptionPane.showInputDialog(view,
                "ENTER DECOY PAYLOAD (HARMLESS MESSAGE):",
                "DECOY CONFIGURATION", JOptionPane.QUESTION_MESSAGE);
            if (decoyMsg == null || decoyMsg.isEmpty()) {
                view.log("DECOY ABORTED: DECOY MESSAGE IS EMPTY.");
                return null;
            }

            view.log("GENERATING DUAL-LAYER PAYLOAD STRUCTURE...");
            String realEnc = encryption.encryptMessage("REAL_LAYER::" + msg, pass, cryptoAlgo);
            String decoyEnc = encryption.encryptMessage("DECOY_LAYER::" + decoyMsg, decoyPass, cryptoAlgo);
            return decoyEnc + "::" + realEnc;
        } else {
            view.log("ENCRYPTING PAYLOAD (" + cryptoAlgo + ")...");
            return encryption.encryptMessage(msg, pass, cryptoAlgo);
        }
    }

    private String decryptDualLayer(String securePayload, String pass) throws Exception {
        if (securePayload.contains("::")) {
            String[] parts = securePayload.split("::");
            if (parts.length == 2) {
                // Try decoy first
                try {
                    String decrypted = decryption.decryptMessage(parts[0], pass);
                    if (decrypted.startsWith("DECOY_LAYER::")) {
                        view.log("NOTICE: DECOY PASSWORD USED. ACCESSING HARMLESS LAYER.");
                        return decrypted.replace("DECOY_LAYER::", "");
                    }
                } catch (Exception ignored) {
                }
                // Try real next
                try {
                    String decrypted = decryption.decryptMessage(parts[1], pass);
                    if (decrypted.startsWith("REAL_LAYER::")) {
                        view.log("NOTICE: REAL KEY VERIFIED. ACCESSING SENSITIVE CORE.");
                        return decrypted.replace("REAL_LAYER::", "");
                    }
                } catch (Exception e) {
                    view.log("ERROR: DECRYPTION FAILED FOR BOTH LAYERS.");
                    throw e;
                }
            }
        }
        return decryption.decryptMessage(securePayload, pass);
    }

    // ==================================================================================
    // FILE DIALOGS
    // ==================================================================================

    public File showOpenDialog(Component parent, String title, String[] extensions) {
        JFileChooser chooser = new JFileChooser(lastSelectedDirectory);
        chooser.setDialogTitle(title);

        if (extensions != null && extensions.length > 0) {
            chooser.setFileFilter(new FileNameExtensionFilter("Supported Media", extensions));
        }

        if (chooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
            lastSelectedDirectory = chooser.getSelectedFile().getParentFile();
            return chooser.getSelectedFile();
        }
        view.log("OPERATION ABORTED BY USER.");
        return null;
    }

    public File showSaveDialog(Component parent, String defaultName, String description, String extension) {
        JFileChooser chooser = new JFileChooser(lastSelectedDirectory);
        chooser.setDialogTitle("SELECT OUTPUT TARGET");

        String filename = extension.isEmpty() ? defaultName : defaultName + "." + extension;
        chooser.setSelectedFile(new File(lastSelectedDirectory, filename));

        if (!extension.isEmpty()) {
            chooser.setFileFilter(new FileNameExtensionFilter(description, extension));
        }

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            lastSelectedDirectory = selected.getParentFile();

            String name = selected.getName();
            if (!extension.isEmpty() && !name.contains(".")) {
                selected = new File(selected.getAbsolutePath() + "." + extension);
                view.log("AUTO-APPENDING EXTENSION: ." + extension);
            }

            view.log("TARGET SET: " + selected.getName());
            return selected;
        }
        view.log("SAVE OPERATION CANCELLED.");
        return null;
    }
}