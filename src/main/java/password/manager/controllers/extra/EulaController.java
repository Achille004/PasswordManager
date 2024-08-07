package password.manager.controllers.extra;

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
    public static final URI FM_LINK = URI.create("https://github.com/Achille004"), SS_LINK = URI.create("https://github.com/samustocco");

    private final IOManager ioManager;

    public EulaController(IOManager ioManager) {
        this.ioManager = ioManager;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void githubFM(ActionEvent event) {
        browse(FM_LINK);
    }

    @FXML
    public void githubSS(ActionEvent event) {
        browse(SS_LINK);
    }

    private void browse(URI uri) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException e) {
                ioManager.getLogger().addError(e);
            }
        } else {
            ioManager.getLogger().addError(new UnsupportedOperationException("Unsupported action: Desktop.Action.BROWSE"));
        }
    }
}
