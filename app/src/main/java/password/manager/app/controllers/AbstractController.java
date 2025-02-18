/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

package password.manager.app.controllers;

import static password.manager.app.utils.Utils.*;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.HostServices;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import javafx.util.Duration;
import password.manager.app.controllers.extra.EulaController;
import password.manager.app.utils.IOManager;
import password.manager.app.utils.ObservableResourceFactory;

public abstract class AbstractController implements Initializable {
    protected final IOManager ioManager;
    protected final ObservableResourceFactory langResources;
    protected final HostServices hostServices;
    protected Stage eulaStage;

    protected AbstractController(@NotNull IOManager ioManager, @NotNull ObservableResourceFactory langResources, @NotNull HostServices hostServices) {
        this.ioManager = ioManager;
        this.langResources = langResources;
        this.hostServices = hostServices;

        eulaStage = null;
    }

    @FXML
    public void showPassword(@NotNull MouseEvent event) {
        Object obj = event.getSource();

        if (obj instanceof Node) {
            ((Node) obj).getParent().toBack();
        }
    }

    @FXML
    public void showEula(MouseEvent event) {
        if (eulaStage == null) {
            ioManager.getLogger().addError(new UnsupportedOperationException("Eula stage not initialized."));
        } else {
            eulaStage.show();
        }
    }

    protected void loadEula() {
        eulaStage = new Stage();
        eulaStage.setTitle(langResources.getValue("terms_credits"));
        eulaStage.getIcons().add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/locker.png"))));
        eulaStage.setResizable(false);

        Parent eulaParent = loadFxml("/fxml/extra/eula.fxml", new EulaController(ioManager, hostServices));
        if (eulaParent == null) {
            eulaStage = null;
            ioManager.getLogger().addError(new IOException("Could not load 'eula.fxml'"));
        } else {
            eulaStage.setScene(new Scene(eulaParent, 900, 600));
        }
    }

    protected <S extends Initializable> Parent loadFxml(String path, S controller) {
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(getClass().getResource(path)));
            loader.setController(controller);
            return loader.load();
        } catch (IOException e) {
            ioManager.getLogger().addError(e);
            return null;
        }
    }

    @SafeVarargs
    protected static <T extends TextInputControl> boolean checkTextFields(T @NotNull... fields) {
        boolean nonEmpty = true;

        for (@NotNull T field : fields) {
            if (field.getText().isBlank()) {
                nonEmpty = false;
                field.setStyle("-fx-border-color: #ff5f5f");
            } else {
                field.setStyle("-fx-border-color: #a7acb1");
            }
        }

        return nonEmpty;
    }

    @SafeVarargs
    protected static <T extends Node> void clearStyle(T @NotNull... nodes) {
        for (@NotNull T node : nodes) {
            node.setStyle("");
        }
    }

    @SafeVarargs
    protected static <T extends TextInputControl> void clearTextFields(T @NotNull... fields) {
        for (@NotNull T field : fields) {
            field.clear();
        }
    }
}