/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

module password.manager {
    requires java.base;
    requires java.desktop;
    
    requires javafx.base;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.web;

    // requires jdk.jsobject;
    
    requires static lombok;

    requires me.gosimple.nbvcxz;
    
    requires org.bouncycastle.provider;
    
    requires org.jetbrains.annotations;
    
    exports password.manager;
    exports password.manager.controllers;
    exports password.manager.controllers.extra;
    exports password.manager.controllers.views;
    exports password.manager.enums;
    exports password.manager.inerfaces;
    exports password.manager.security;
    exports password.manager.utils;
    
    opens password.manager to javafx.fxml;
}
