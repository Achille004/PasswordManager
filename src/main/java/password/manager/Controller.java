/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras (2004marras@gmail.com)

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

package password.manager;

import static password.manager.utils.Utils.*;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Locale;
import java.util.Objects;
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
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import password.manager.enums.SortingOrder;
import password.manager.security.Account;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public class Controller implements Initializable {
    public static final Locale[] SUPPORTED_LOCALES;
    public static final Locale DEFAULT_LOCALE;

    static {
        SUPPORTED_LOCALES = new Locale[] { Locale.ENGLISH, Locale.ITALIAN };

        Locale systemLang = Locale.forLanguageTag(Locale.getDefault().getLanguage());
        DEFAULT_LOCALE = Arrays.asList(SUPPORTED_LOCALES).contains(systemLang)
                ? systemLang
                : Locale.ENGLISH;
    }

    private final @Getter IOManager ioManager;
    private final @Getter ObservableResourceFactory langResources;

    private Stage eulaStage = null;

    public Controller() {
        this.ioManager = new IOManager();
        this.langResources = new ObservableResourceFactory();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // If account exists, its Locale will be used, else it will fall back to the
        // default value.
        ObjectProperty<Locale> locale = settingsLangCB.valueProperty();
        langResources.resourcesProperty().bind(Bindings.createObjectBinding(
                () -> {
                    Locale localeValue = locale.getValue();
                    return ResourceBundle.getBundle("/bundles/Lang",
                            localeValue != null ? localeValue : DEFAULT_LOCALE);
                },
                locale));

        boolean firstRun = ioManager.loadDataFile(langResources);

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/eula.fxml"));
            loader.setController(new EULAController(ioManager));

            Parent root = loader.load();
            eulaStage = new Stage();
            eulaStage.setTitle(langResources.getValue("terms_credits"));
            eulaStage.getIcons()
                    .add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/locker.png"))));
            eulaStage.setResizable(false);
            eulaStage.setScene(new Scene(root, 900, 600));
        } catch (IOException e) {
            ioManager.getLogger().addError(e);
        }

        if (firstRun) {
            initializeFirstRun();
        } else {
            initializeLogin();
        }

        initializeMain();
    }

    // #region First Run
    @FXML
    private AnchorPane firstRunPane;

    @FXML
    private Label firstRunTitle, firstRunDescTop, firstRunDescBtm, firstRunTermsCred, firstRunDisclaimer;

    @FXML
    private TextField firstRunPasswordVisible;

    @FXML
    private PasswordField firstRunPasswordHidden;

    @FXML
    private CheckBox firstRunCheckBox;

    @FXML
    private Button firstRunSubmitBtn;

    @FXML
    private ProgressBar firstRunPassStr;

    private void initializeFirstRun() {
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

        firstRunPane.toFront();
    }

    @FXML
    public void doFirstRun() {
        if (checkTextFields(firstRunPasswordVisible, firstRunPasswordHidden) && firstRunCheckBox.isSelected()) {
            ioManager.setLoginAccount(SortingOrder.SOFTWARE, DEFAULT_LOCALE, firstRunPasswordVisible.getText());
            firstRunPane.toBack();
            mainTitleAnimation();
        }
    }
    // #endregion

    // #region Login
    @FXML
    private AnchorPane loginPane;

    @FXML
    private Label loginTitle;

    @FXML
    private TextField loginPasswordVisible;

    @FXML
    private PasswordField loginPasswordHidden;

    @FXML
    private Button loginSubmitBtn;

    private Timeline wrongPasswordsTimeline;

    private void initializeLogin() {
        wrongPasswordsTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, evt -> loginSubmitBtn.setStyle("-fx-border-color: #ff5f5f")),
                new KeyFrame(Duration.seconds(1), evt -> clearStyle(loginSubmitBtn)));

        langResources.bindTextProperty(loginTitle, "welcome_back");
        langResources.bindTextProperty(loginSubmitBtn, "lets_go");

        bindPasswordFields(loginPasswordHidden, loginPasswordVisible);

        loginPane.toFront();
    }

    @FXML
    public void doLogin() {
        if (checkTextFields(loginPasswordVisible, loginPasswordHidden)) {
            wrongPasswordsTimeline.stop();
            ioManager.authenticate(loginPasswordVisible.getText());

            if (ioManager.isAuthenticated()) {
                loginPane.toBack();
                mainTitleAnimation();
            } else {
                wrongPasswordsTimeline.play();
            }

            clearTextFields(loginPasswordHidden, loginPasswordVisible);
        }
    }
    // #endregion
    
    // #region Main
    private Button lastSidebarButton = null;

    @FXML
    private Label psmgTitle, mainTitle;

    @FXML
    private Button homeButton;

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

    private void initializeMain() {
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

    private void mainTitleAnimation() {
        psmgTitle.setText("");
        titleAnimation.play();
    }

    // #region Home
    @FXML
    private AnchorPane homePane;

    @FXML
    private Label homeDescTop, homeDescBtm;    

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
    private GridPane encryptPane;

    @FXML
    private TextField encryptSoftware, encryptUsername, encryptPasswordVisible;

    @FXML
    private PasswordField encryptPasswordHidden;

    @FXML
    private Button encryptSubmitBtn;

    @FXML
    private Label encryptSoftwareLbl, encryptUsernameLbl, encryptPasswordLbl;

    @FXML
    private ProgressBar encryptPassStr;

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
        encryptResetStyle();
        encryptClear();

        encryptPane.toFront();
        setMainTitle("encryption");

        highlightSidebarButton(event);
    }

    private void encryptClear() {
        clearTextFields(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
    }

    private void encryptResetStyle() {
        clearStyle(encryptSoftware, encryptUsername, encryptPasswordVisible, encryptPasswordHidden);
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

            encryptClear();
        }
    }
    // #endregion

    // #region Decrypt
    @FXML
    private GridPane decryptPane;

    @FXML
    private ComboBox<Account> decryptCB;

    @FXML
    private TextField decryptSoftware, decryptUsername, decryptPasswordVisible;
    @FXML
    private PasswordField decryptPasswordHidden;

    @FXML
    private Button decryptDelete;
    private boolean decryptDeleteCounter = false;

    @FXML
    private Label decryptAccSelLbl, decryptSelectedAccLbl, decryptSoftwareLbl, decryptUsernameLbl, decryptPasswordLbl;

    @FXML
    private ProgressBar decryptPassStr;

    private void initializeDecrypt() {
        langResources.bindTextProperty(decryptAccSelLbl, "select_acc");
        langResources.bindTextProperty(decryptSoftwareLbl, "software");
        langResources.bindTextProperty(decryptUsernameLbl, "username");
        langResources.bindTextProperty(decryptPasswordLbl, "password");

        bindPasswordFields(decryptPasswordHidden, decryptPasswordVisible);

        ObservableList<Node> passStrChildren = decryptPassStr.getChildrenUnmodifiable();
        decryptPasswordVisible.textProperty().addListener((observable, oldValue, newValue) -> {
            double passwordStrength = passwordStrength(newValue);
            passwordStrength = Math.max(20d, passwordStrength);
            passwordStrength = Math.min(50d, passwordStrength);

            double progress = (passwordStrength - 20) / 30;
            if (passStrChildren.size() != 0) {
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
                    decryptDeleteCounter = false;
                    decryptResetStyle();

                    if (newItem != null) {
                        // shows the software, username and account of the selected account
                        decryptSoftware.setText(newItem.getSoftware());
                        decryptUsername.setText(newItem.getUsername());
                        String accPassword = ioManager.getAccountPassword(newItem);
                        decryptPasswordVisible.setText(accPassword);
                        decryptPasswordHidden.setText(accPassword);
                    } else {
                        decryptClear();
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
        decryptCB.getSelectionModel().clearSelection();
        decryptResetStyle();
        decryptClear();

        decryptPane.toFront();
        setMainTitle("decryption");

        highlightSidebarButton(event);
    }

    private void decryptClear() {
        clearTextFields(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden);
    }

    private void decryptResetStyle() {
        clearStyle(decryptSoftware, decryptUsername, decryptPasswordVisible, decryptPasswordHidden, decryptDelete);
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

            clearStyle(decryptDelete);
            decryptClear();

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
            decryptResetStyle();
            decryptClear();

            // removes the selected account from the list
            ioManager.removeAccount(account);
        } else {
            decryptDelete.setStyle("-fx-background-color: #ff5f5f");
        }

        decryptDeleteCounter = !decryptDeleteCounter;
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
    private GridPane settingsPane;

    @FXML
    private ComboBox<Locale> settingsLangCB;
    @FXML
    private ComboBox<SortingOrder> settingsOrderCB;

    @FXML
    private TextField settingsLoginPasswordVisible;
    @FXML
    private PasswordField settingsLoginPasswordHidden;

    @FXML
    private Label settingsLangLbl, selectedLangLbl, settingsSortingOrderLbl, selectedOrderLbl,
            settingsLoginPasswordLbl, settingsLoginPasswordDesc, settingsDriveConnLbl, wip;

    @FXML
    private ProgressBar settingsLoginPassStr;

    public void initializeSettings() {
        langResources.bindTextProperty(settingsLangLbl, "language");
        langResources.bindTextProperty(settingsSortingOrderLbl, "sorting_ord");
        langResources.bindTextProperty(settingsLoginPasswordLbl, "login_pas");
        langResources.bindTextProperty(settingsLoginPasswordDesc, "login_pas.desc");
        langResources.bindTextProperty(settingsDriveConnLbl, "drive_con");
        langResources.bindTextProperty(wip, "wip");

        SortedList<Locale> languages = getFXSortedList(SUPPORTED_LOCALES);
        settingsLangCB.setItems(languages);
        settingsLangCB.getSelectionModel().select(ioManager.getLoginAccount() != null
                ? ioManager.getLoginAccount().getLocale()
                : DEFAULT_LOCALE);
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
                                new KeyValue(settingsLoginPassStr.progressProperty(), settingsLoginPassStr.getProgress())),
                        new KeyFrame(new Duration(200),
                                new KeyValue(settingsLoginPassStr.progressProperty(), progress)));

                timeline.play();
            }
        });
    }

    @FXML
    public void settingsSidebarButton(ActionEvent event) {
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
    // #endregion

    @FXML
    public void showPassword(MouseEvent event) {
        Object obj = event.getSource();

        if (obj instanceof Node) {
            ((Node) obj).getParent().toBack();
        }
    }

    @FXML
    public void showEula(MouseEvent event) {
        if (eulaStage == null) {
            return;
        }

        eulaStage.show();
    }

    @RequiredArgsConstructor
    public class EULAController implements Initializable {
        public static final URI FM_LINK = URI.create("https://github.com/Achille004"),
                SS_LINK = URI.create("https://github.com/samustocco");

        private final @Getter IOManager ioManager;

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
}