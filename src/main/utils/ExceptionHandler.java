package src.main.utils;

import javax.crypto.AEADBadTagException;
import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class ExceptionHandler {

    public static void handle(Exception e, String title) {
        e.printStackTrace(); // Log to console for debugging

        String userMessage = getUserFriendlyMessage(e);
        Toolkit.getDefaultToolkit().beep();

        JOptionPane.showMessageDialog(null, userMessage, title, JOptionPane.ERROR_MESSAGE);
    }

    private static String getUserFriendlyMessage(Exception e) {
        if (e instanceof AEADBadTagException) {
            return "Decryption Failed: Incorrect Password or Tampered Data.";
        }
        if (e instanceof IOException) {
            return "File Error: " + e.getMessage();
        }
        if (e instanceof IllegalArgumentException) {
            return "Invalid Input: " + e.getMessage();
        }
        return "An unexpected error occurred: " + e.getMessage();
    }
}