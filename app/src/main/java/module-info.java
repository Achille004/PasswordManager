/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras

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

module password.manager.app {
    requires java.desktop;

    requires javafx.base;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    // requires javafx.web;

    requires org.controlsfx.controls;

    requires com.fasterxml.jackson.databind;
    // requires org.json;

    requires static lombok;

    requires org.bouncycastle.provider;

    requires org.jetbrains.annotations;

    requires password.manager.lib;

    exports password.manager.app;
    exports password.manager.app.controllers;
    exports password.manager.app.controllers.extra;
    exports password.manager.app.controllers.main;
    exports password.manager.app.enums;
    exports password.manager.app.security;
    exports password.manager.app.singletons;

    opens password.manager.app to javafx.fxml;
    opens password.manager.app.controllers to javafx.fxml;
    opens password.manager.app.controllers.extra to javafx.fxml;
    opens password.manager.app.controllers.main to javafx.fxml;

    opens password.manager.app.security to com.fasterxml.jackson.databind;
    opens password.manager.app.singletons to com.fasterxml.jackson.databind;
}
