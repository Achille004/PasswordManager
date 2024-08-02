package password.manager.controllers;

import static password.manager.utils.Utils.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URL;
import java.util.LinkedList;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jetbrains.annotations.NotNull;

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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;
import javafx.util.StringConverter;
import password.manager.AppManager;
import password.manager.enums.SortingOrder;
import password.manager.security.Account;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class MainController extends AbstractController {
    public MainController(IOManager ioManager, ObservableResourceFactory langResources) {
        super(ioManager, langResources);
    }

    private Button lastSidebarButton = null;

    @FXML
    public Label psmgTitle, mainTitle;

    @FXML
    public Button homeButton;

    private static String[] titleStages;
    private Timeline titleAnimation;

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

    public void initialize(URL location, ResourceBundle resources) {
        initializeHome();
        initializeEncrypt();
        initializeDecrypt();
        initializeSettings();

        titleAnimation = new Timeline();

        for (int i = 0; i < titleStages.length; i++) {
            final String str = titleStages[i];
            titleAnimation.getKeyFrames().add(new KeyFrame(Duration.millis(8 * i), event -> {
                psmgTitle.setText(str);
            }));
        }
    }

    public void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    // #region Home
    @FXML
    public AnchorPane homePane;

    @FXML
    public Label homeDescTop, homeDescBtm;

    private void initializeHome() {
        langResources.bindTextProperty(homeDescTop, "home_desc.top");
        langResources.bindTextProperty(homeDescBtm, "home_desc.btm");
    }

    @FXML
    public void homeButton(ActionEvent event) {
        homePane.toFront();
        setMainTitle("");

        highlightSidebarButton(null);
    }
    // #endregion

    // #region Encrypt
    @FXML
    public GridPane encryptPane;

    @FXML
    public TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    public PasswordField encryptPasswordHidden;

    @FXML
    public Button encryptSubmitBtn;

    @FXML
    public Label encryptSoftwareLbl, encryptUsernameLbl, encryptPasswordLbl;

    @FXML
    public ProgressBar encryptPassStr;

    private void initializeEncrypt() {
        langResources.bindTextProperty(encryptSubmitBtn, "submit");
        langResources.bindTextProperty(encryptSoftwareLbl, "software");
        langResources.bindTextProperty(encryptUsernameLbl, "username");
        langResources.bindTextProperty(encryptPasswordLbl, "password");

        bindPasswordFields(encryptPasswordHidden, encryptPasswordVisible);

        ObservableList<Node> passStrChildren = encryptPassStr.getChildrenUnmodifiable();
        encryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            if (passStrChildren.size() != 0) {
                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(encryptPassStr.progressProperty(), encryptPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(encryptPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });
    }

    @FXML
    public void encryptSidebarButton(ActionEvent event) {
        encryptReset();

        encryptPane.toFront();
        setMainTitle("encryption");

        highlightSidebarButton(event);
    }

    private void encryptReset() {
        clearStyle(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
        clearTextFields(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
    }

    @FXML
    public void encryptSave(ActionEvent event) {
        if (checkTextFields(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden)) {
            // gets software, username and password written by the user
            String software = encryptSoftware.getText();
            String username = encryptUsername.getText();
            String password = encryptPasswordVisible.getText();
            // save the new account
            ioManager.addAccount(software, username, password);

            encryptReset();
        }
    }
    // #endregion

    // #region Decrypt
    @FXML
    public GridPane decryptPane;

    @FXML
    public ComboBox<Account> decryptCB;

    @FXML
    public TextField decryptSoftware, decryptUsername, decryptPasswordVisible;
    @FXML
    public PasswordField decryptPasswordHidden;

    @FXML
    public Button decryptDelete;
    private boolean decryptDeleteCounter = false;

    @FXML
    public Label decryptAccSelLbl, decryptSelectedAccLbl, decryptSoftwareLbl, decryptUsernameLbl, decryptPasswordLbl;

    @FXML
    public ProgressBar decryptPassStr;

    private void initializeDecrypt() {
        langResources.bindTextProperty(decryptAccSelLbl, "select_acc");
        langResources.bindTextProperty(decryptSoftwareLbl, "software");
        langResources.bindTextProperty(decryptUsernameLbl, "username");
        langResources.bindTextProperty(decryptPasswordLbl, "password");

        bindPasswordFields(decryptPasswordHidden, decryptPasswordVisible);

        ObservableList<Node> passStrChildren = decryptPassStr.getChildrenUnmodifiable();
        decryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            if (passStrChildren.size() != 0 && !decryptCB.getSelectionModel().isEmpty()) {
                double passwordStrength = passwordStrength(newValue);
                passwordStrength = Math.max(20d, passwordStrength);
                passwordStrength = Math.min(50d, passwordStrength);

                double progress = (passwordStrength - 20) / 30;

                Node bar = passStrChildren.filtered(node -> node.getStyleClass().contains("bar")).getFirst();
                bar.setStyle("-fx-background-color:" + passwordStrengthGradient(progress));

                Timeline timeline = new Timeline(
                        new KeyFrame(Duration.ZERO,
                                new KeyValue(decryptPassStr.progressProperty(), decryptPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(decryptPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });

        SortedList<Account> accountList = ioManager.getSortedAccountList();
        decryptCB.setItems(accountList);

        ObjectProperty<SortingOrder> sortingOrder = settingsOrderCB.valueProperty();
        decryptCB.converterProperty().bind(sortingOrder.map(this::accountStringConverter));
        accountList.comparatorProperty().bind(Bindings.createObjectBinding(
                () -> {
                    SortingOrder sortingOrderValue = sortingOrder.getValue();
                    return sortingOrderValue != null
                            ? sortingOrderValue.getComparator()
                            : SortingOrder.SOFTWARE.getComparator();
                },
                sortingOrder));

        decryptCB.getSelectionModel().selectedItemProperty().addListener(
                (options, oldItem, newItem) -> {
                    decryptReset();
                    if (newItem != null) {
                        // shows the software, username and account of the selected account
                        decryptSoftware.setText(newItem.getSoftware());
                        decryptUsername.setText(newItem.getUsername());
                        String accPassword = ioManager.getAccountPassword(newItem);
                        decryptPasswordVisible.setText(accPassword);
                        decryptPasswordHidden.setText(accPassword);
                    }
                });

        ObjectProperty<Account> selectedAccount = decryptCB.valueProperty();
        decryptSelectedAccLbl.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    SortingOrder sortingOrderValue = sortingOrder.getValue();
                    return sortingOrderValue != null
                            ? accountStringConverter(sortingOrderValue).toString(selectedAccount.getValue())
                            : null;
                },
                selectedAccount));
    }

    @FXML
    public void decryptSidebarButton(ActionEvent event) {
        decryptReset();
        decryptCB.getSelectionModel().clearSelection();

        decryptPane.toFront();
        setMainTitle("decryption");

        highlightSidebarButton(event);
    }

    private void decryptReset() {
        decryptDeleteCounter = false;
        clearStyle(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden, decryptDelete);
        clearTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden);
    }

    @FXML
    public void decryptSave(ActionEvent event) {
        Account account = selectedComboBoxItem(decryptCB);
        if (account == null) {
            return;
        }

        if (checkTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden)) {
            // get the new software, username and password
            String software = decryptSoftware.getText();
            String username = decryptUsername.getText();
            String password = decryptPasswordVisible.getText();

            decryptReset();
            decryptCB.getSelectionModel().clearSelection();

            // save the new attributes of the account
            ioManager.editAccount(account, software, username, password);
        }
    }

    @FXML
    public void decryptDelete(ActionEvent event) {
        Account account = selectedComboBoxItem(decryptCB);
        if (account == null) {
            return;
        }

        // when the deleteCounter is true it means that the user has confirmed the
        // elimination
        if (decryptDeleteCounter) {
            decryptReset();
            decryptCB.getSelectionModel().clearSelection();

            // removes the selected account from the list
            ioManager.removeAccount(account);
        } else {
            decryptDelete.setStyle("-fx-background-color: #ff5f5f");
            decryptDeleteCounter = true;
        }
    }

    private StringConverter<Account> accountStringConverter(SortingOrder order) {
        return new StringConverter<Account>() {
            @Override
            public Account fromString(String string) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String toString(@NotNull Account account) {
                return account != null ? order.convert(account) : null;
            }
        };
    }
    // #endregion

    // #region Settings
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

    public void initializeSettings() {
        // If account exists, its Locale will be used, else it will fall back to the
        // default value.
        ObjectProperty<Locale> locale = settingsLangCB.valueProperty();
        langResources.resourcesProperty().bind(Bindings.createObjectBinding(
                () -> {
                    Locale localeValue = locale.getValue();
                    return ResourceBundle.getBundle("/bundles/Lang",
                            localeValue != null ? localeValue : AppManager.DEFAULT_LOCALE);
                },
                locale));

        langResources.bindTextProperty(settingsLangLbl, "language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "sorting_ord");
        langResources.bindTextProperty(settingsLoginPasswordLbl, "login_pas");
        langResources.bindTextProperty(settingsLoginPasswordDesc, "login_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "drive_con");
        langResources.bindTextProperty(wip, "wip");

        // Language box

        SortedList<Locale> languages = getFXSortedList(AppManager.SUPPORTED_LOCALES);
        settingsLangCB.setItems(languages);
        settingsLangCB.getSelectionModel().select(ioManager.getLoginAccount() != null
                ? ioManager.getLoginAccount().getLocale()
                : AppManager.DEFAULT_LOCALE);
        bindValueConverter(settingsLangCB, settingsLangCB.valueProperty(), this::languageStringConverter);
        bindValueComparator(languages, settingsLangCB.valueProperty(), settingsLangCB);
        settingsLangCB.getSelectionModel().selectedItemProperty().addListener(
                (options, oldItem, newItem) -> ioManager.getLoginAccount().setLocale(newItem));

        ObjectProperty<Locale> language = settingsLangCB.valueProperty();
        selectedLangLbl.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    Locale languageValue = language.getValue();
                    return languageStringConverter(languageValue).toString(languageValue);
                },
                language));

        // Sorting order box

        SortedList<SortingOrder> sortingOrders = getFXSortedList(SortingOrder.class.getEnumConstants());
        settingsOrderCB.setItems(sortingOrders);
        settingsOrderCB.getSelectionModel().select(ioManager.getLoginAccount() != null
                ? ioManager.getLoginAccount().getSortingOrder()
                : SortingOrder.SOFTWARE);
        bindValueConverter(settingsOrderCB, settingsLangCB.valueProperty(), this::sortingOrderStringConverter);
        bindValueComparator(sortingOrders, settingsLangCB.valueProperty(), settingsOrderCB);
        settingsOrderCB.getSelectionModel().selectedItemProperty().addListener(
                (options, oldItem, newItem) -> ioManager.getLoginAccount().setSortingOrder(newItem));

        ObjectProperty<SortingOrder> sortingOrder = settingsOrderCB.valueProperty();
        selectedOrderLbl.textProperty().bind(Bindings.createStringBinding(
                () -> {
                    SortingOrder sortingOrderValue = sortingOrder.getValue();
                    return sortingOrderStringConverter(language.getValue()).toString(sortingOrderValue);
                },
                sortingOrder, language));

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

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
        clearStyle(settingsLoginPasswordHidden, settingsLoginPasswordVisible);
        ioManager.displayLoginPassword(settingsLoginPasswordVisible, settingsLoginPasswordHidden);

        settingsPane.toFront();
        setMainTitle("settings");

        highlightSidebarButton(event);
    }

    private StringConverter<Locale> languageStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? capitalizeWord(item.getDisplayLanguage(item)) : null);
    }

    private StringConverter<SortingOrder> sortingOrderStringConverter(Locale locale) {
        return toStringConverter(item -> item != null ? langResources.getValue(item.getI18nKey()) : null);
    }
    // #endregion

    @FXML
    public void folderSidebarButton(ActionEvent event) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            try {
                Desktop.getDesktop().open(ioManager.getFilePath().toFile());
            } catch (IOException e) {
                ioManager.getLogger().addError(e);
            }
        }
    }

    private void setMainTitle(String key) {
        boolean isNotHome = !key.isBlank();

        homeButton.setVisible(isNotHome);
        mainTitle.setVisible(isNotHome);
        langResources.bindTextProperty(mainTitle, key);
    }

    private void highlightSidebarButton(ActionEvent event) {
        if (lastSidebarButton != null) {
            lastSidebarButton.setStyle("-fx-background-color: #202428");
        }

        if (event != null) {
            lastSidebarButton = (Button) event.getSource();
            lastSidebarButton.setStyle("-fx-background-color: #42464a");
        }
    }
}
