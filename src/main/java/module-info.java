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

module PasswordManager.main {
    requires jdk.compiler;
    // requires jdk.jsobject;
    requires java.base;
    requires java.desktop;
    requires static lombok;

    requires org.jetbrains.annotations;

    requires org.bouncycastle.provider;

    requires javafx.base;
    requires transitive javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.web;

    // TODO switch to sign/sigstore plugin
    requires sigstore.java;
    
    exports main;
    exports main.enums;
    exports main.inerfaces;
    exports main.security;
    exports main.utils;
    
    opens main to javafx.fxml;
}
