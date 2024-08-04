package password.manager.controllers;

import java.io.IOException;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public abstract class AbstractController implements Initializable {
    protected final IOManager ioManager;
    protected final ObservableResourceFactory langResources;
    protected final Stage eulaStage;

    protected AbstractController(IOManager ioManager, ObservableResourceFactory langResources) {
        this.ioManager = ioManager;
        this.langResources = langResources;

        eulaStage = new Stage();
        eulaStage.setTitle(langResources.getValue("terms_credits"));
        eulaStage.getIcons()
                .add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/locker.png"))));
        eulaStage.setResizable(false);
        eulaStage.setScene(new Scene(loadFxml("/fxml/extra/eula.fxml", new EulaController(ioManager)), 900, 600));
    }

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
            ioManager.getLogger().addError(new IOException("Could not load 'eula.fxml'"));
            return;
        }

        eulaStage.show();
    }

    protected <T, S extends Initializable> Parent loadFxml(String path, S controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(path)));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            ioManager.getLogger().addError(e);
            return null;
        }
    }
}