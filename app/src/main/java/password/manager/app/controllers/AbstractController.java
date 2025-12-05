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

import static password.manager.app.Utils.*;

import org.jetbrains.annotations.NotNull;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.image.Image;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import password.manager.app.App;
import password.manager.app.controllers.extra.EulaController;
import password.manager.app.singletons.ObservableResourceFactory;

public abstract class AbstractController implements Initializable {
    // Store EULA stage as singleton to avoid multiple instances
    private static Stage eulaStage = null;

    public void reset() { /* do nothing */ }

    @FXML
    protected final void showEula(MouseEvent event) {
        AbstractController.loadEula();
        eulaStage.show();
        eulaStage.toFront();
    }

    private static void loadEula() {
        if (eulaStage != null) return;

        eulaStage = new Stage();
        ObservableResourceFactory.getInstance().bindTitleProperty(eulaStage, "terms_credits");
        eulaStage.getIcons().add(new Image(App.MAIN_ICON));
        eulaStage.setResizable(false);

        AnchorPane eulaParent = (AnchorPane) loadFxml("/fxml/extra/eula.fxml", new EulaController());
        eulaStage.setScene(new Scene(eulaParent, 900, 600));
    }

    protected static final boolean checkTextFields(TextInputControl @NotNull... fields) {
        boolean nonEmpty = true;

        for (@NotNull TextInputControl field : fields) {
            if (field.getText().isBlank()) {
                nonEmpty = false;
                field.setStyle("-fx-border-color: -fx-color-red");
            } else {
                field.setStyle("-fx-border-color: -fx-color-grey");
            }
        }

        return nonEmpty;
    }

    protected static final void clearStyle(Node @NotNull... nodes) {
        for (@NotNull Node node : nodes) node.setStyle("");
    }

    protected static final void clearTextFields(TextInputControl @NotNull... fields) {
        for (@NotNull TextInputControl field : fields) field.clear();
    }
}