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

package password.manager.app.controllers.extra;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.HostServices;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import password.manager.app.utils.IOManager;

public class EulaController implements Initializable {
    public static final String FM_LINK = "https://github.com/Achille004", SS_LINK = "https://github.com/samustocco";

    private final IOManager ioManager;
    private final HostServices hostServices;

    public EulaController(IOManager ioManager, HostServices hostServices) {
        this.ioManager = ioManager;
        this.hostServices = hostServices;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void githubFM(ActionEvent event) {
        browse(FM_LINK);
    }

    @FXML
    public void githubSS(ActionEvent event) {
        browse(SS_LINK);
    }

    private void browse(String uri) {
        Thread.startVirtualThread(() -> hostServices.showDocument(uri));
    }
}