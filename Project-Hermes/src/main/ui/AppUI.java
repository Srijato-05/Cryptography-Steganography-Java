package src.main.ui;

import src.main.utils.Config;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

/**
 * Advanced Animated Cyberpunk AppUI.
 * Implements high-legibility layout scaling, a full-canvas telemetry terminal with size 16 bold font,
 * and seamless background matrix animation.
 */
public class AppUI extends JFrame {

    private JPasswordField passwordField;
    private JTextArea messageArea;
    private JTextArea systemLog;
    private JProgressBar capacityMeter;
    private JCheckBox decoyModeCheck;
    private JLabel capacityLabel;
    private JProgressBar entropyBar;
    private JLabel entropyValueLabel;
    private JComboBox<String> cryptoAlgoCombo;
    private JComboBox<String> stegoLayoutCombo;
    private JLabel fileMetadataLabel;

    private File droppedFile;
    private float maxChars = 0.0f;

    private JButton encryptBtn, decryptBtn;
    private JButton hideImgBtn, extractImgBtn, hideAudBtn, extractAudBtn, hideVidBtn, extractVidBtn, hideTxtBtn, extractTxtBtn;

    private CardLayout cardLayout;
    private JPanel cardContainer;
    private JButton tabCryptoBtn, tabStegoBtn, tabLogsBtn;

    private static final Font HEADER_FONT = new Font("OCR A Extended", Font.BOLD, 26);
    private static final Font LARGE_FONT = new Font("Consolas", Font.BOLD, 18);
    private static final Font MEDIUM_FONT = new Font("Consolas", Font.BOLD, 15);
    private static final Font TERMINAL_FONT = new Font("Lucida Console", Font.BOLD, 16); // Larger terminal font

    // Background animation particles (Binary Stream Rain)
    private final java.util.List<Particle> particles = new java.util.ArrayList<>();
    private javax.swing.Timer animatorTimer;

    private static class Particle {
        float x, y;
        float speed;
        String text;
        Color color;
    }

    private void updateMaxChars() {
        if (droppedFile == null) {
            maxChars = 0.0f;
            updateMetadataLabel("NO CARRIER LOADED");
            return;
        }
        String name = droppedFile.getName().toLowerCase();
        long length = droppedFile.length();
        if (isImageExtension(name)) {
            try {
                BufferedImage img = ImageIO.read(droppedFile);
                if (img != null) {
                    int w = img.getWidth();
                    int h = img.getHeight();
                    maxChars = (float) (w * h * 3) / 8.0f;
                    updateMetadataLabel(String.format("IMAGE CARRIER | %dx%d PX | %s | CAPACITY: %.0f CHARS", w, h, formatBytes(length), maxChars));
                } else {
                    maxChars = (float) length / 8.0f;
                    updateMetadataLabel("IMAGE CARRIER | " + formatBytes(length) + " | CAPACITY: " + (int)maxChars + " CHARS");
                }
            } catch (Exception e) {
                maxChars = (float) length / 8.0f;
                updateMetadataLabel("IMAGE CARRIER | " + formatBytes(length) + " | CAPACITY: " + (int)maxChars + " CHARS");
            }
        } else if (name.endsWith(".wav")) {
            maxChars = (float) Math.max(0, length - 44) / 8.0f;
            updateMetadataLabel("AUDIO WAVE CARRIER | " + formatBytes(length) + " | CAPACITY: " + (int)maxChars + " CHARS");
        } else if (isVideoExtension(name)) {
            maxChars = (float) Math.max(0, Integer.MAX_VALUE - length);
            updateMetadataLabel("VIDEO CONTAINER | " + formatBytes(length) + " | CAPACITY: HIGH (EOF INJECTION)");
        } else if (name.endsWith(".txt")) {
            maxChars = (float) length / 8.0f;
            updateMetadataLabel("TEXT FILE CARRIER | " + formatBytes(length) + " | CAPACITY: ZERO-WIDTH UNICODE");
        } else {
            maxChars = (float) length / 8.0f;
            updateMetadataLabel("UNKNOWN FORMAT | " + formatBytes(length) + " | CAPACITY: LSB STANDARD");
        }
    }

    private void updateMetadataLabel(String text) {
        fileMetadataLabel.setText("» " + text.toUpperCase());
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %cB", bytes / Math.pow(1024, exp), pre);
    }

    private boolean isImageExtension(String name) {
        for (String ext : Config.EXT_IMAGES) {
            if (name.endsWith("." + ext)) return true;
        }
        return false;
    }

    private boolean isVideoExtension(String name) {
        for (String ext : Config.EXT_VIDEOS) {
            if (name.endsWith("." + ext)) return true;
        }
        return false;
    }

    public AppUI() {
        setTitle(Config.APP_TITLE);
        setSize(1350, 950);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Core Layout Background grid with dynamic binary animation
        JPanel mainContainer = new JPanel(new BorderLayout(0, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(12, 6, 26), 0, getHeight(), Color.BLACK);
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());

                // Cyberpunk mesh grid
                g2.setColor(new Color(255, 0, 127, 8)); // Magenta trace
                for (int i = 0; i < getWidth(); i += 40) g2.drawLine(i, 0, i, getHeight());
                for (int j = 0; j < getHeight(); j += 40) g2.drawLine(0, j, getWidth(), j);

                // Draw ambient descending binary stream particles
                g2.setFont(TERMINAL_FONT);
                for (Particle p : particles) {
                    g2.setColor(p.color);
                    int px = (int) (p.x * getWidth());
                    int py = (int) (p.y * getHeight());
                    g2.drawString(p.text, px, py);
                }

                // Tech border
                g2.setColor(new Color(0, 240, 255, 15));
                g2.drawRect(5, 5, getWidth() - 10, getHeight() - 10);
            }
        };
        mainContainer.setBorder(new EmptyBorder(25, 25, 25, 25));
        enableDragAndDrop(mainContainer);

        // Populate background particles
        Random rand = new Random();
        for (int i = 0; i < 45; i++) {
            Particle p = new Particle();
            p.x = rand.nextFloat();
            p.y = rand.nextFloat();
            p.speed = 0.002f + rand.nextFloat() * 0.004f;
            p.text = rand.nextBoolean() ? "0" : "1";
            p.color = rand.nextBoolean() ? new Color(0, 240, 255, 35) : new Color(255, 0, 127, 30);
            particles.add(p);
        }

        // Start background repaint timer
        animatorTimer = new javax.swing.Timer(40, e -> {
            for (Particle p : particles) {
                p.y += p.speed;
                if (p.y > 1.0f) {
                    p.y = 0.0f;
                    p.x = rand.nextFloat();
                }
            }
            mainContainer.repaint();
        });
        animatorTimer.start();

        // 1. TOP HEADER + NAV BAR PANEL
        JPanel headerPanel = new JPanel(new BorderLayout(15, 10));
        headerPanel.setOpaque(false);

        JLabel titleLabel = new JLabel("SECURE-STEGO // COVERT SUITE");
        titleLabel.setFont(HEADER_FONT);
        titleLabel.setForeground(Config.NEON_CYAN);
        headerPanel.add(titleLabel, BorderLayout.WEST);

        // Custom Navigation Buttons acting as Tabs
        JPanel navPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        navPanel.setOpaque(false);

        tabCryptoBtn = createNavBtn("CRYPTOGRAPHY", true);
        tabStegoBtn = createNavBtn("STEGANOGRAPHY", false);
        tabLogsBtn = createNavBtn("TELEMETRY LOGS", false);

        navPanel.add(tabCryptoBtn);
        navPanel.add(tabStegoBtn);
        navPanel.add(tabLogsBtn);
        headerPanel.add(navPanel, BorderLayout.EAST);

        // CARRIER FILE METADATA GLOW BOX
        JPanel metaPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        metaPanel.setOpaque(false);
        metaPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0, 255, 255, 60)));
        fileMetadataLabel = new JLabel("» NO CARRIER LOADED. DROP FILE ANYWHERE.");
        fileMetadataLabel.setFont(MEDIUM_FONT);
        fileMetadataLabel.setForeground(Config.NEON_YELLOW);
        metaPanel.add(fileMetadataLabel);
        headerPanel.add(metaPanel, BorderLayout.SOUTH);

        mainContainer.add(headerPanel, BorderLayout.NORTH);

        // 2. CENTER CARD LAYOUT CONTAINER
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(createCryptoTabPanel(), "CRYPTO");
        cardContainer.add(createStegoTabPanel(), "STEGO");
        cardContainer.add(createLogsTabPanel(), "LOGS");

        mainContainer.add(cardContainer, BorderLayout.CENTER);

        // Tab selection action listeners
        tabCryptoBtn.addActionListener(e -> switchTab("CRYPTO", tabCryptoBtn));
        tabStegoBtn.addActionListener(e -> switchTab("STEGO", tabStegoBtn));
        tabLogsBtn.addActionListener(e -> switchTab("LOGS", tabLogsBtn));

        add(mainContainer);
    }

    private void switchTab(String cardName, JButton selectedBtn) {
        cardLayout.show(cardContainer, cardName);

        tabCryptoBtn.setBackground(new Color(15, 10, 30));
        tabCryptoBtn.setForeground(Config.NEON_CYAN);
        tabCryptoBtn.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 1));

        tabStegoBtn.setBackground(new Color(15, 10, 30));
        tabStegoBtn.setForeground(Config.NEON_CYAN);
        tabStegoBtn.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 1));

        tabLogsBtn.setBackground(new Color(15, 10, 30));
        tabLogsBtn.setForeground(Config.NEON_CYAN);
        tabLogsBtn.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 1));

        selectedBtn.setBackground(Config.NEON_CYAN);
        selectedBtn.setForeground(Color.BLACK);
        selectedBtn.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));

        tabCryptoBtn.repaint();
        tabStegoBtn.repaint();
        tabLogsBtn.repaint();
    }

    private JPanel createCryptoTabPanel() {
        GlassPanel p = new GlassPanel("STANDALONE FILE ENCRYPTION");
        JPanel content = p.getContentPane();

        JLabel cryptoChoiceLabel = new JLabel("CHOOSE CRYPTO ALGORITHM");
        cryptoChoiceLabel.setForeground(Config.TEXT_PRIMARY);
        cryptoChoiceLabel.setFont(LARGE_FONT);
        cryptoChoiceLabel.setAlignmentX(CENTER_ALIGNMENT);

        cryptoAlgoCombo = new JComboBox<>(new String[]{"AES-GCM (Recommended)", "ChaCha20-Poly1305", "AES-CBC"});
        styleComponent(cryptoAlgoCombo);
        cryptoAlgoCombo.setPreferredSize(new Dimension(500, 45));
        cryptoAlgoCombo.setMaximumSize(new Dimension(500, 45));

        JLabel authKeyLabel = new JLabel("AUTHENTICATION PASSPHRASE / KEY");
        authKeyLabel.setForeground(Config.TEXT_PRIMARY);
        authKeyLabel.setFont(LARGE_FONT);
        authKeyLabel.setAlignmentX(CENTER_ALIGNMENT);

        passwordField = new JPasswordField();
        styleComponent(passwordField);
        passwordField.setEchoChar('*'); // Safe echo character
        passwordField.setPreferredSize(new Dimension(500, 45));
        passwordField.setMaximumSize(new Dimension(500, 45));
        passwordField.setForeground(Config.NEON_CYAN);
        passwordField.setSelectedTextColor(Color.BLACK);
        passwordField.setSelectionColor(Config.NEON_CYAN);

        passwordField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateEntropy(); }
            public void removeUpdate(DocumentEvent e) { updateEntropy(); }
            public void changedUpdate(DocumentEvent e) { updateEntropy(); }
        });

        encryptBtn = createNeonBtn("ENCRYPT CARRIER FILE", true);
        decryptBtn = createNeonBtn("DECRYPT CARRIER FILE", true);

        JPanel telemetryPanel = createEntropyTelemetryPanel();
        telemetryPanel.setAlignmentX(CENTER_ALIGNMENT);

        content.add(Box.createVerticalGlue());
        content.add(cryptoChoiceLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(cryptoAlgoCombo);
        content.add(Box.createVerticalStrut(25));
        content.add(authKeyLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(passwordField);
        content.add(Box.createVerticalStrut(30));
        content.add(encryptBtn);
        content.add(Box.createVerticalStrut(12));
        content.add(decryptBtn);
        content.add(Box.createVerticalStrut(35));
        content.add(telemetryPanel);
        content.add(Box.createVerticalGlue());

        return p;
    }

    private JPanel createStegoTabPanel() {
        GlassPanel p = new GlassPanel("STEGANOGRAPHY OPERATIONS");
        JPanel content = p.getContentPane();

        JLabel payloadLabel = new JLabel("SECRET INJECTION PAYLOAD (TEXT / STRING)");
        payloadLabel.setForeground(Config.TEXT_PRIMARY);
        payloadLabel.setFont(LARGE_FONT);
        payloadLabel.setAlignmentX(CENTER_ALIGNMENT);

        messageArea = new JTextArea(5, 20);
        styleComponent(messageArea);
        JScrollPane scroll = new JScrollPane(messageArea);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 1));
        scroll.setMaximumSize(new Dimension(850, 120));

        messageArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateCapacity(); }
            public void removeUpdate(DocumentEvent e) { updateCapacity(); }
            public void changedUpdate(DocumentEvent e) { updateCapacity(); }
        });

        JPanel settingsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 25, 0));
        settingsPanel.setOpaque(false);

        JLabel layoutLabel = new JLabel("SCATTERING:");
        layoutLabel.setForeground(Config.TEXT_PRIMARY);
        layoutLabel.setFont(MEDIUM_FONT);
        stegoLayoutCombo = new JComboBox<>(new String[]{"PRNG Seeded Scatter", "Sequential LSB"});
        styleComponent(stegoLayoutCombo);
        stegoLayoutCombo.setPreferredSize(new Dimension(250, 35));
        stegoLayoutCombo.setMaximumSize(new Dimension(250, 35));

        decoyModeCheck = new JCheckBox("ENABLE DECOY LAYER");
        decoyModeCheck.setOpaque(false);
        decoyModeCheck.setForeground(Config.NEON_YELLOW);
        decoyModeCheck.setFont(MEDIUM_FONT);

        settingsPanel.add(layoutLabel);
        settingsPanel.add(stegoLayoutCombo);
        settingsPanel.add(Box.createHorizontalStrut(30));
        settingsPanel.add(decoyModeCheck);

        capacityLabel = new JLabel("CAPACITY USAGE: 0%");
        capacityLabel.setForeground(Config.NEON_CYAN);
        capacityLabel.setFont(MEDIUM_FONT);
        capacityLabel.setAlignmentX(CENTER_ALIGNMENT);

        capacityMeter = new JProgressBar(0, 100);
        capacityMeter.setMaximumSize(new Dimension(850, 8));
        capacityMeter.setForeground(Config.NEON_GREEN);
        capacityMeter.setBackground(new Color(20, 20, 30));

        hideImgBtn = createNeonBtn("INJECT IMAGE", false);
        extractImgBtn = createNeonBtn("EXTRACT IMAGE", false);
        hideAudBtn = createNeonBtn("INJECT AUDIO", false);
        extractAudBtn = createNeonBtn("EXTRACT AUDIO", false);
        hideVidBtn = createNeonBtn("INJECT VIDEO", false);
        extractVidBtn = createNeonBtn("EXTRACT VIDEO", false);
        hideTxtBtn = createNeonBtn("INJECT TEXT", false);
        extractTxtBtn = createNeonBtn("EXTRACT TEXT", false);

        JPanel btnGrid = new JPanel(new GridLayout(2, 4, 15, 12));
        btnGrid.setOpaque(false);
        btnGrid.setMaximumSize(new Dimension(950, 100));
        btnGrid.add(hideImgBtn); btnGrid.add(extractImgBtn);
        btnGrid.add(hideAudBtn); btnGrid.add(extractAudBtn);
        btnGrid.add(hideVidBtn); btnGrid.add(extractVidBtn);
        btnGrid.add(hideTxtBtn); btnGrid.add(extractTxtBtn);

        content.add(Box.createVerticalGlue());
        content.add(payloadLabel);
        content.add(Box.createVerticalStrut(12));
        content.add(scroll);
        content.add(Box.createVerticalStrut(12));
        content.add(settingsPanel);
        content.add(Box.createVerticalStrut(12));
        content.add(capacityLabel);
        content.add(capacityMeter);
        content.add(Box.createVerticalStrut(25));
        content.add(btnGrid);
        content.add(Box.createVerticalGlue());

        return p;
    }

    private JPanel createLogsTabPanel() {
        GlassPanel p = new GlassPanel("SYSTEM COMMAND LOGS & TELEMETRY");
        // Re-configure layout of the content pane to BorderLayout to allow full-size logging
        p.getContentPane().setLayout(new BorderLayout(0, 10));

        systemLog = new JTextArea();
        systemLog.setEditable(false);
        systemLog.setOpaque(false);
        systemLog.setForeground(Config.NEON_GREEN);
        systemLog.setFont(TERMINAL_FONT); // Larger size 16 bold terminal font
        systemLog.setMargin(new Insets(20, 20, 20, 20));

        DefaultCaret caret = (DefaultCaret) systemLog.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        JScrollPane scroll = new JScrollPane(systemLog);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 2));

        p.getContentPane().add(scroll, BorderLayout.CENTER);
        return p;
    }

    private JPanel createEntropyTelemetryPanel() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Config.NEON_CYAN, 1),
                " SYSTEM TELEMETRY ", 0, 0, MEDIUM_FONT, Config.NEON_CYAN
        ));
        p.setMaximumSize(new Dimension(450, 95));

        entropyValueLabel = new JLabel("KEY ENTROPY: 0 BITS (WEAK)");
        entropyValueLabel.setForeground(Config.NEON_RED);
        entropyValueLabel.setFont(MEDIUM_FONT);
        entropyValueLabel.setAlignmentX(CENTER_ALIGNMENT);

        entropyBar = new JProgressBar(0, 128);
        entropyBar.setMaximumSize(new Dimension(400, 10));
        entropyBar.setForeground(Config.NEON_RED);
        entropyBar.setBackground(new Color(20, 20, 30));
        entropyBar.setBorderPainted(false);

        p.add(Box.createVerticalStrut(8));
        p.add(entropyValueLabel);
        p.add(Box.createVerticalStrut(8));
        p.add(entropyBar);
        p.add(Box.createVerticalStrut(8));
        return p;
    }

    // --- LOGIC REFINEMENT ---

    public void log(String message) {
        String ts = new SimpleDateFormat("HH:mm:ss").format(new Date());
        if (systemLog != null) {
            systemLog.append("[" + ts + "] » " + message.toUpperCase() + "\n");
        } else {
            System.out.println("[" + ts + "] » " + message.toUpperCase());
        }
    }

    public void setStatus(String message) { log(message); }

    private void updateEntropy() {
        String pass = new String(passwordField.getPassword());
        if (pass.isEmpty()) {
            entropyBar.setValue(0);
            entropyValueLabel.setText("KEY ENTROPY: 0 BITS (WEAK)");
            entropyBar.setForeground(Config.NEON_RED);
            entropyValueLabel.setForeground(Config.NEON_RED);
            return;
        }
        int bits = (int) (pass.length() * (Math.log(pass.chars().distinct().count() + 1) / Math.log(2)));
        entropyBar.setValue(bits);
        if (bits < 40) {
            entropyBar.setForeground(Config.NEON_RED);
            entropyValueLabel.setText("KEY ENTROPY: " + bits + " BITS (WEAK)");
            entropyValueLabel.setForeground(Config.NEON_RED);
        } else if (bits < 80) {
            entropyBar.setForeground(Config.NEON_YELLOW);
            entropyValueLabel.setText("KEY ENTROPY: " + bits + " BITS (MODERATE)");
            entropyValueLabel.setForeground(Config.NEON_YELLOW);
        } else {
            entropyBar.setForeground(Config.NEON_GREEN);
            entropyValueLabel.setText("KEY ENTROPY: " + bits + " BITS (SECURE)");
            entropyValueLabel.setForeground(Config.NEON_GREEN);
        }
    }

    private void updateCapacity() {
        String text = messageArea.getText();
        if (droppedFile != null && !text.isEmpty()) {
            String name = droppedFile.getName().toLowerCase();
            if (name.endsWith(".txt")) {
                capacityMeter.setValue(50);
                capacityLabel.setText("CAPACITY USAGE: SECURE ZERO-WIDTH PACKING ACTIVE");
                capacityMeter.setForeground(Config.NEON_GREEN);
                capacityLabel.setForeground(Config.NEON_GREEN);
                return;
            }

            if (maxChars > 0.0f) {
                float currentChars = (float) text.length();
                float percent = (currentChars / maxChars) * 100.0f;
                int displayPercent = Math.min(100, (int) Math.ceil(percent));

                capacityMeter.setValue(displayPercent);
                capacityLabel.setText("CAPACITY USAGE: " + displayPercent + "%");

                if (displayPercent > 80) {
                    capacityMeter.setForeground(Config.NEON_RED);
                    capacityLabel.setForeground(Config.NEON_RED);
                } else if (displayPercent > 50) {
                    capacityMeter.setForeground(Config.NEON_YELLOW);
                    capacityLabel.setForeground(Config.NEON_YELLOW);
                } else {
                    capacityMeter.setForeground(Config.NEON_GREEN);
                    capacityLabel.setForeground(Config.NEON_CYAN);
                }
            }
        } else {
            capacityMeter.setValue(0);
            capacityLabel.setText("CAPACITY USAGE: 0%");
            capacityLabel.setForeground(Config.NEON_CYAN);
        }
    }

    private void styleComponent(JComponent c) {
        c.setBackground(new Color(18, 12, 36, 160));
        c.setForeground(Config.NEON_CYAN);
        c.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 1));
        c.setFont(LARGE_FONT);
        c.setAlignmentX(CENTER_ALIGNMENT);
        if (c instanceof JTextComponent) {
            ((JTextComponent) c).setCaretColor(Config.NEON_CYAN);
            ((JTextComponent) c).setMargin(new Insets(8, 8, 8, 8));
        }
        if (c instanceof JComboBox) {
            ((JComboBox<?>) c).setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    list.setBackground(new Color(18, 12, 36));
                    list.setOpaque(true);
                    Component renderer = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    renderer.setBackground(isSelected ? Config.NEON_CYAN : new Color(18, 12, 36));
                    renderer.setForeground(isSelected ? Color.BLACK : Config.NEON_CYAN);
                    setFont(MEDIUM_FONT);
                    return renderer;
                }
            });
        }
    }

    private JButton createNavBtn(String text, boolean activeDefault) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (getBackground().equals(Config.NEON_CYAN)) {
                    g2.setColor(Config.NEON_CYAN);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                } else {
                    g2.setColor(new Color(15, 10, 30));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                }
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBackground(activeDefault ? Config.NEON_CYAN : new Color(15, 10, 30));
        b.setForeground(activeDefault ? Color.BLACK : Config.NEON_CYAN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(activeDefault ? Color.BLACK : Config.NEON_CYAN, 1));
        b.setFont(MEDIUM_FONT);
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(160, 38));
        return b;
    }

    private JButton createNeonBtn(String text, boolean isLarge) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                super.paintComponent(g);
            }
        };
        b.setContentAreaFilled(false);
        b.setOpaque(false);
        b.setBackground(new Color(18, 12, 36));
        b.setForeground(Config.NEON_CYAN);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(Config.NEON_CYAN, 2));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));
        b.setFont(isLarge ? LARGE_FONT : MEDIUM_FONT);
        b.setAlignmentX(CENTER_ALIGNMENT);
        b.setPreferredSize(isLarge ? new Dimension(350, 50) : new Dimension(220, 42));

        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                b.setBackground(Config.NEON_CYAN);
                b.setForeground(Color.BLACK);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                b.setBackground(new Color(18, 12, 36));
                b.setForeground(Config.NEON_CYAN);
            }
        });
        return b;
    }

    private class GlassPanel extends JPanel {
        private JPanel contentPane;

        GlassPanel(String title) {
            setLayout(new BorderLayout(0, 15));
            setOpaque(false);
            setBorder(new EmptyBorder(25, 25, 25, 25));

            JLabel t = new JLabel(title, JLabel.CENTER);
            t.setFont(HEADER_FONT);
            t.setForeground(Config.NEON_CYAN);
            t.setAlignmentX(CENTER_ALIGNMENT);
            add(t, BorderLayout.NORTH);

            contentPane = new JPanel();
            contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
            contentPane.setOpaque(false);
            add(contentPane, BorderLayout.CENTER);
        }

        public JPanel getContentPane() {
            return contentPane;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Config.GLASS_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
            g2.setColor(Config.BORDER_GLOW);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 25, 25);
            super.paintComponent(g);
        }
    }

    private void enableDragAndDrop(JPanel p) {
        new DropTarget(p, new DropTargetAdapter() {
            @SuppressWarnings("unchecked")
            public void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> files = (List<File>) dtde.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        droppedFile = files.get(0);
                        log("CARRIER LOADED: " + droppedFile.getName());
                        updateMaxChars();
                        updateCapacity();
                    }
                } catch (Exception e) { log("ERROR: DROP FAILED"); }
            }
        });
    }

    // --- GETTERS & SETTERS ---

    public void setExternalFile(File file) {
        this.droppedFile = file;
        updateMaxChars();
        updateCapacity();
    }

    public String getPassword() { return new String(passwordField.getPassword()); }
    public String getMessage() { return messageArea.getText(); }
    public File getDroppedFile() { return droppedFile; }
    public void clearDroppedFile() {
        droppedFile = null;
        updateMaxChars();
        updateCapacity();
    }
    public boolean isDecoyEnabled() { return decoyModeCheck.isSelected(); }
    public boolean isScatterEnabled() {
        return stegoLayoutCombo.getSelectedIndex() == 0;
    }
    public String getSelectedCryptoAlgo() {
        String sel = (String) cryptoAlgoCombo.getSelectedItem();
        if (sel == null) return "AES-GCM";
        if (sel.contains("AES-GCM")) return "AES-GCM";
        return sel;
    }

    public JButton getEncryptBtn() { return encryptBtn; }
    public JButton getDecryptBtn() { return decryptBtn; }
    public JButton getHideImgBtn() { return hideImgBtn; }
    public JButton getExtractImgBtn() { return extractImgBtn; }
    public JButton getHideAudBtn() { return hideAudBtn; }
    public JButton getExtractAudBtn() { return extractAudBtn; }
    public JButton getHideVidBtn() { return hideVidBtn; }
    public JButton getExtractVidBtn() { return extractVidBtn; }
    public JButton getHideTxtBtn() { return hideTxtBtn; }
    public JButton getExtractTxtBtn() { return extractTxtBtn; }
}