package password.manager.controllers;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import password.manager.utils.IOManager;

public class EulaController implements Initializable {
    public static final URI FM_LINK = URI.create("https://github.com/Achille004"),
            SS_LINK = URI.create("https://github.com/samustocco");

    private final IOManager ioManager;

    public EulaController(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void githubFM(ActionEvent event) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(FM_LINK);
            } catch (IOException e) {
                ioManager.getLogger().addError(e);
            }
        }
    }

    @FXML
    public void githubSS(ActionEvent event) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(SS_LINK);
            } catch (IOException e) {
                ioManager.getLogger().addError(e);
            }
        }
    }
}
