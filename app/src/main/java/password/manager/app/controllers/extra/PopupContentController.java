package password.manager.app.controllers.extra;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;

public class PopupContentController implements Initializable {
    @FXML
    private Label label;

    @FXML
    private AnchorPane bottomBar;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Logger.getInstance().addDebug("Initializing " + getClass().getSimpleName());
    }

    public void setState(String i18nKey, String bottomBarColor) {
        final ObservableResourceFactory resources = ObservableResourceFactory.getInstance();
        label.setText(resources.getValue("popup." + i18nKey));
        bottomBar.setStyle("-fx-background-color: " + bottomBarColor + ";");
    }
}
