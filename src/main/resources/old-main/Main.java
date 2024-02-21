/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2023  Francesco Marras

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see https://www.gnu.org/licenses/gpl-3.0.html.
 */

package main;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;

import javax.swing.*;
import javax.swing.border.*;

import main.security.*;
import main.utils.*;
import static main.utils.Utils.*;
import main.views.*;
import org.jetbrains.annotations.NotNull;

/**
 * Main class.
 *
 * @version 2.0
 * @author 2004marras@gmail.com
 */
public class Main extends JFrame {

    private final Logger logger;

    private LoginAccount loginAccount;
    private ArrayList<Account> accountList;
    private final String filePath;

    private String loginPassword;

    /**
     * Constructor method, initializes objects and gets the data filepath, then
     * runs the program.
     */
    public Main() {
        initComponents();

        // initialize objects
        accountList = new ArrayList<>();

        MenuBar.setVisible(false);

        // gets the filepath
        boolean isWindows = System.getProperty("os.name").contains("Windows");
        filePath = isWindows ? System.getProperty("user.home") + "\\AppData\\Local\\Password Manager\\" : "";

        logger = new Logger(filePath);

        run();
    }

    // #region GUI methods
    public void EncrypterButtonActionPerformed(ActionEvent evt) {
        switch (loginAccount.getLanguage()) {
            case "e" -> EncrypterPanel.load("Save", "Username");
            case "i" -> EncrypterPanel.load("Salva", "Nome utente");
            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        // redirects to encrypter panel
        replaceToDialogPanel(EncrypterPanel);
    }

    public void DecrypterButtonActionPerformed(ActionEvent evt) {
        switch (loginAccount.getLanguage()) {
            case "e" -> DecrypterPanel.load("Delete", "Save", "Username");
            case "i" -> DecrypterPanel.load("Elimina", "Salva", "Nome utente");
            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        // redirects to decrypter panel
        replaceToDialogPanel(DecrypterPanel);
    }

    public void SettingsButtonActionPerformed(ActionEvent evt) {
        String[] languageSelectorItems, savingOrderSelectorItems;
        String languageLabelText, savingOrderLabelText, loginPasswordLabelText;
        String confirmButtonText;

        switch (loginAccount.getLanguage()) {
            case "e" -> {
                languageSelectorItems = new String[] { "English", "Italian" };
                savingOrderSelectorItems = new String[] { "Software", "Username" };
                languageLabelText = "Language";
                savingOrderLabelText = "Saving order";
                loginPasswordLabelText = "Login password";
                confirmButtonText = "Confirm";
            }

            case "i" -> {
                languageSelectorItems = new String[] { "Inglese", "Italiano" };
                savingOrderSelectorItems = new String[] { "Software", "Nome utente" };
                languageLabelText = "Linguaggio";
                savingOrderLabelText = "Ordine di salvataggio";
                loginPasswordLabelText = "Password d'accesso";
                confirmButtonText = "Conferma";
            }

            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        SettingsPanel.load(languageSelectorItems, savingOrderSelectorItems, languageLabelText, savingOrderLabelText,
                loginPasswordLabelText, confirmButtonText, loginPassword);

        // redirects to settings panel
        replaceToDialogPanel(SettingsPanel);
    }

    public void LogHistoryButtonActionPerformed(ActionEvent evt) {
        switch (loginAccount.getLanguage()) {
            case "e" -> LogHistoryPanel.load("Log History", ">>> Actions / !!! Errors");
            case "i" -> LogHistoryPanel.load("Cronologia del Registro", ">>> Azioni / !!! Errori");
            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        // redirects to log history panel
        replaceToDialogPanel(LogHistoryPanel);
    }

    public void EulaAndCreditsButtonActionPerformed(ActionEvent evt) {
        // redirects to eula and credits panel
        replaceToDialogPanel(EulaAndCreditsPanel);
    }

    public void switchToProgramPanel(String @NotNull... password) {
        if (password.length > 0) {
            this.loginPassword = password[0];
        }

        String htmlSmallButtonTemplate = "<html> <body width='86px'> <center> ? </center> </body> </html>";
        switch (loginAccount.getLanguage()) {
            case "e" -> {
                ExportAsMenu.setText("Export as");
                EncrypterButton.setText("Encrypter");
                DecrypterButton.setText("Decrypter");
                SettingsButton.setText("Settings");
                LogHistoryButton.setText(htmlSmallButtonTemplate.replace("?", "Log History"));
                EulaAndCreditsButton.setText(htmlSmallButtonTemplate.replace("?", "EULA and Credits"));
            }

            case "i" -> {
                ExportAsMenu.setText("Esporta come");
                EncrypterButton.setText("Cripta");
                DecrypterButton.setText("Decifra");
                SettingsButton.setText("Impostazioni");
                LogHistoryButton.setText(htmlSmallButtonTemplate.replace("?", "Cronologia Registro"));
                EulaAndCreditsButton.setText(htmlSmallButtonTemplate.replace("?", "Termini e Crediti"));
            }

            default -> throw new IllegalArgumentException("Invalid language: " + loginAccount.getLanguage());
        }

        if (!accountList.isEmpty()) {
            MenuBar.setVisible(true);
        }

        // redirects to program panel
        replaceToMainPanel(ProgramPanel);
    }
    // #endregion

    /**
     * Redirects to the login or first run procedure, based on the password file
     * existence.
     */
    
    private void run() {
        
    }
}