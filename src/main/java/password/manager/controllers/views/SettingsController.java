package password.manager.controllers.views;

import static password.manager.utils.Utils.*;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import password.manager.enums.SortingOrder;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;
import password.manager.utils.Utils;

public class SettingsController extends AbstractViewController {
    public SettingsController(IOManager ioManager, ObservableResourceFactory langResources) {
        super(ioManager, langResources);
    }

    @FXML
    public GridPane settingsPane;

    @FXML
    public ComboBox<Locale> settingsLangCB;
    @FXML
    public ComboBox<SortingOrder> settingsOrderCB;

    @FXML
    public TextField settingsLoginPasswordVisible;
    @FXML
    public PasswordField settingsLoginPasswordHidden;

    @FXML
    public Label settingsLangLbl, selectedLangLbl, settingsSortingOrderLbl, selectedOrderLbl,
            settingsLoginPasswordLbl, settingsLoginPasswordDesc, settingsDriveConnLbl, wip;

    @FXML
    public ProgressBar settingsLoginPassStr;

    public void initialize(URL location, ResourceBundle resources) {
        ObjectProperty<Locale> localeProperty = ioManager.getUserPreferences().getLocaleProperty();
        ObjectProperty<SortingOrder> sortingOrderProperty = ioManager.getUserPreferences().getSortingOrderProperty();

        langResources.bindTextProperty(settingsLangLbl, "language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "sorting_ord");
        langResources.bindTextProperty(settingsLoginPasswordLbl, "login_pas");
        langResources.bindTextProperty(settingsLoginPasswordDesc, "login_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "drive_con");
        langResources.bindTextProperty(wip, "wip");

        // Language box

        SortedList<Locale> languages = getFXSortedList(Utils.SUPPORTED_LOCALE);

        settingsLangCB.setItems(languages);
        settingsLangCB.getSelectionModel().select(ioManager.getUserPreferences().getLocale());
        bindValueConverter(settingsLangCB, localeProperty, this::languageStringConverter);
        bindValueComparator(languages, localeProperty, settingsLangCB);

        selectedLangLbl.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    Locale locale = localeProperty.getValue();
                    return languageStringConverter(locale).toString(locale);
                },
                localeProperty));

        localeProperty.bind(settingsLangCB.valueProperty());

        // Sorting order box

        SortedList<SortingOrder> sortingOrders = getFXSortedList(SortingOrder.class.getEnumConstants());

        settingsOrderCB.setItems(sortingOrders);
        settingsOrderCB.getSelectionModel().select(ioManager.getUserPreferences().getSortingOrder());
        bindValueConverter(settingsOrderCB, localeProperty, this::sortingOrderStringConverter);
        bindValueComparator(sortingOrders, localeProperty, settingsOrderCB);

        selectedOrderLbl.textProperty().bind(Bindings.createStringBinding(
                () -> sortingOrderStringConverter(localeProperty.getValue()).toString(sortingOrderProperty.getValue()),
                localeProperty, sortingOrderProperty));

        sortingOrderProperty.bind(settingsOrderCB.valueProperty());

        // Login password

        EventHandler<ActionEvent> changeLoginPasswordEvent = event -> {
            if (checkTextFields(settingsLoginPasswordVisible, settingsLoginPasswordHidden)) {
                ioManager.changeLoginPassword(settingsLoginPasswordVisible.getText());
            }
        };
        settingsLoginPasswordVisible.setOnAction(changeLoginPasswordEvent);
        settingsLoginPasswordHidden.setOnAction(changeLoginPasswordEvent);
        bindPasswordFields(settingsLoginPasswordHidden, settingsLoginPasswordVisible);

        ObservableList<Node> passStrChildren = settingsLoginPassStr.getChildrenUnmodifiable();
        settingsLoginPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            if (passStrChildren.size() != 0) {
                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(settingsLoginPassStr.progressProperty(),
                                        settingsLoginPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(settingsLoginPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });
    }

    public void reset() {
        clearStyle(settingsLoginPasswordHidden, settingsLoginPasswordVisible);
        ioManager.displayLoginPassword(settingsLoginPasswordVisible, settingsLoginPasswordHidden);
    }

    private StringConverter<Locale> languageStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? capitalizeWord(item.getDisplayLanguage(item)) : null);
    }

    private StringConverter<SortingOrder> sortingOrderStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? langResources.getValue(item.getI18nKey()) : null);
    }
}
