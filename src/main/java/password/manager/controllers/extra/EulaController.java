package password.manager.controllers.extra;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import password.manager.utils.IOManager;

public class EulaController implements Initializable {
    public static final String FM_LINK = "https://github.com/Achille004", SS_LINK = "https://github.com/samustocco";

    private final IOManager ioManager;
    private final HostServices hostServices;

    public EulaController(IOManager ioManager, HostServices hostServices) {
        this.ioManager = ioManager;
        this.hostServices = hostServices;
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

    private void browse(String uri) {
        hostServices.showDocument(uri);
    }
}