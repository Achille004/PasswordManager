package password.manager.controllers;

import static password.manager.utils.Utils.*;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.util.Duration;
import password.manager.AppManager;
import password.manager.enums.SortingOrder;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class FirstRunController extends AbstractController {
    private final BooleanProperty switchToMain;
    
    public FirstRunController(IOManager ioManager, ObservableResourceFactory langResources, BooleanProperty switchToMain) {
        super(ioManager, langResources);
        this.switchToMain = switchToMain;
    }

    @FXML
    public Label firstRunTitle, firstRunDescTop, firstRunDescBtm, firstRunTermsCred, firstRunDisclaimer;

    @FXML
    public TextField firstRunPasswordVisible;

    @FXML
    public PasswordField firstRunPasswordHidden;

    @FXML
    public CheckBox firstRunCheckBox;

    @FXML
    public Button firstRunSubmitBtn;

    @FXML
    public ProgressBar firstRunPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        langResources.bindTextProperty(firstRunTitle, "hi");
        langResources.bindTextProperty(firstRunDescTop, "first_run.desc.top");
        langResources.bindTextProperty(firstRunDescBtm, "first_run.desc.btm");
        langResources.bindTextProperty(firstRunCheckBox, "first_run.check_box");
        langResources.bindTextProperty(firstRunTermsCred, "terms_credits");
        langResources.bindTextProperty(firstRunSubmitBtn, "lets_go");
        langResources.bindTextProperty(firstRunDisclaimer, "first_run.disclaimer");

        bindPasswordFields(firstRunPasswordHidden, firstRunPasswordVisible);

        ObservableList<Node> passStrChildren = firstRunPassStr.getChildrenUnmodifiable();
        firstRunPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            if (passStrChildren.size() != 0) {
                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(firstRunPassStr.progressProperty(), firstRunPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(firstRunPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });
    }

    @FXML
    public void doFirstRun() {
        if (checkTextFields(firstRunPasswordVisible, firstRunPasswordHidden) && firstRunCheckBox.isSelected()) {
            ioManager.setLoginAccount(SortingOrder.SOFTWARE, AppManager.DEFAULT_LOCALE, firstRunPasswordVisible.getText());
            switchToMain.set(true);
        }
    }
}
