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

package password.manager.controllers;

import java.io.IOException;
import java.util.Objects;

import javafx.application.HostServices;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import password.manager.controllers.extra.EulaController;
import password.manager.utils.IOManager;
import password.manager.utils.ObservableResourceFactory;

public abstract class AbstractController implements Initializable {
    protected final IOManager ioManager;
    protected final ObservableResourceFactory langResources;
    protected final HostServices hostServices;
    protected final Stage eulaStage;

    protected AbstractController(@NotNull IOManager ioManager, @NotNull ObservableResourceFactory langResources, @NotNull HostServices hostServices) {
        this.ioManager = ioManager;
        this.langResources = langResources;
        this.hostServices = hostServices;

        eulaStage = new Stage();
        eulaStage.setTitle(langResources.getValue("terms_credits"));
        eulaStage.getIcons()
                .add(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/images/locker.png"))));
        eulaStage.setResizable(false);
        eulaStage.setScene(new Scene(loadFxml("/fxml/extra/eula.fxml", new EulaController(ioManager, hostServices)), 900, 600));
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
            ioManager.getLogger().addError(new IOException("Could not load 'eula.fxml'"));
            return;
        }

        eulaStage.show();
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
}