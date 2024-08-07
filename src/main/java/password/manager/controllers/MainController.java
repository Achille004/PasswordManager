package password.manager.controllers;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.Duration;
import password.manager.controllers.views.DecrypterController;
import password.manager.controllers.views.EncrypterController;
import password.manager.controllers.views.HomeController;
import password.manager.controllers.views.SettingsController;
import password.manager.controllers.views.AbstractViewController;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class MainController extends AbstractController {
    private static String[] titleStages;

    static {
        final String title = "Password Manager";
        final char[] lower = "abcdefghijklmnopqrstuvwxyz".toCharArray();
        final char[] upper = "ABCDEFGHIJKLMNOPQRTSUVWXYZ".toCharArray();

        final LinkedList<String> stages = new LinkedList<String>();
        String currString = "";

        for (char c : title.toCharArray()) {
            for (int i = 0; i < 26; i++) {
                if (Character.isLowerCase(c)) {
                    stages.add(currString + lower[i]);
                    if (lower[i] == c) {
                        break;
                    }
                } else if (Character.isUpperCase(c)) {
                    stages.add(currString + upper[i]);
                    if (upper[i] == c) {
                        break;
                    }
                } else {
                    stages.add(currString + c);
                    break;
                }
            }
            currString += c;
        }

        titleStages = stages.toArray(new String[0]);
    }

    public MainController(IOManager ioManager, ObservableResourceFactory langResources) {
        super(ioManager, langResources);
    }

    private Button lastSidebarButton = null;
    private Timeline titleAnimation;

    @FXML
    public BorderPane mainPane;

    @FXML
    public Label psmgTitle, mainTitle;

    @FXML
    public Button homeButton;

    private AnchorPane homePane;
    private GridPane encrypterPane, decrypterPane, settingsPane;
    private AbstractViewController homeController, encrypterController, decrypterController, settingsController;

    public void initialize(URL location, ResourceBundle resources) {
        homeController = new HomeController(ioManager, langResources);
        homePane = (AnchorPane) loadFxml("/fxml/views/home.fxml", homeController);

        encrypterController = new EncrypterController(ioManager, langResources);
        encrypterPane = (GridPane) loadFxml("/fxml/views/encrypter.fxml", encrypterController);

        decrypterController = new DecrypterController(ioManager, langResources);
        decrypterPane = (GridPane) loadFxml("/fxml/views/decrypter.fxml", decrypterController);

        settingsController = new SettingsController(ioManager, langResources);
        settingsPane = (GridPane) loadFxml("/fxml/views/settings.fxml", settingsController);

        titleAnimation = new Timeline();
        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(8 * i), event -> {
                psmgTitle.setText(str);
            }));
        }

        homeButton(null);
    }

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    @FXML
    public void homeButton(ActionEvent event) {
        sidebarButtonAction(null, homeController, homePane, "");
    }

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        sidebarButtonAction(event, encrypterController, encrypterPane, "encryption");
    }

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        sidebarButtonAction(event, decrypterController, decrypterPane, "decryption");
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        sidebarButtonAction(event, settingsController, settingsPane, "settings");
    }

    @FXML
    public void folderSidebarButton(ActionEvent event) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(ioManager.getFilePath().toFile());
            } catch (IOException e) {
                ioManager.getLogger().addError(e);
            }
        } else {
            ioManager.getLogger().addError(new UnsupportedOperationException("Unsupported action: Desktop.Action.OPEN"));
        }
    }

    private <T extends AbstractViewController, S extends Pane> void sidebarButtonAction(ActionEvent event, T destinationController, S destinationPane, String mainTitleKey) {
        // Show selected pane
        destinationController.reset();
        mainPane.centerProperty().set(destinationPane);

        // Set main title
        boolean isNotHome = !mainTitleKey.isBlank();
        homeButton.setVisible(isNotHome);
        mainTitle.setVisible(isNotHome);
        langResources.bindTextProperty(mainTitle, mainTitleKey);

        // Lowlight the previous button, if there is one
        if (lastSidebarButton != null) {
            lastSidebarButton.setStyle("-fx-background-color: #202428");
        }

        // Get and highlight the new button
        if (event != null) {
            lastSidebarButton = (Button) event.getSource();
            lastSidebarButton.setStyle("-fx-background-color: #42464a");
        }
    }
}
