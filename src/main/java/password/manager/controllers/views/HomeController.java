package password.manager.controllers.views;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class HomeController extends AbstractViewController {
    public HomeController(IOManager ioManager, ObservableResourceFactory langResources, HostServices hostServices) {
        super(ioManager, langResources, hostServices);
    }

    @FXML
    public Label homeDescTop, homeDescBtm;

    public void initialize(URL location, ResourceBundle resources) {
        langResources.bindTextProperty(homeDescTop, "home_desc.top");
        langResources.bindTextProperty(homeDescBtm, "home_desc.btm");
    }

    public void reset() {
    }
}
