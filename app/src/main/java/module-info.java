/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras

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
    ///// REQUIRES /////

    requires java.desktop;

    requires javafx.base;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    // requires javafx.web;

    requires org.controlsfx.controls;

    requires com.fasterxml.jackson.annotation;
    requires tools.jackson.core;
    requires tools.jackson.databind;

    requires static lombok;

    requires org.bouncycastle.provider;

    requires org.jetbrains.annotations;

    requires password.manager.lib;

    ///// EXPORTS /////

    exports password.manager.app.base;
    exports password.manager.app.controllers;
    exports password.manager.app.controllers.extra;
    exports password.manager.app.controllers.main;
    exports password.manager.app.persistence;
    exports password.manager.app.security;
    exports password.manager.app.singletons;
    exports password.manager.app;

    ///// OPENS /////

    opens password.manager.app to javafx.fxml;
    opens password.manager.app.controllers to javafx.fxml;
    opens password.manager.app.controllers.extra to javafx.fxml;
    opens password.manager.app.controllers.main to javafx.fxml;

    opens password.manager.app.base to tools.jackson.databind;
    opens password.manager.app.security to tools.jackson.databind;
    opens password.manager.app.singletons to tools.jackson.databind;
}
