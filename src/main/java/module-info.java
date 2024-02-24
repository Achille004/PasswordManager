module PasswordManager.main {
    // requires transitive java.desktop;
    // requires java.logging;
    
    requires org.jetbrains.annotations;

    requires javafx.base;
    requires javafx.controls;
    requires javafx.fxml;
    requires transitive javafx.graphics;
    requires javafx.web;
    
    requires jdk.jsobject;
    
    exports main;
    exports main.enums;
    exports main.security;
    exports main.utils;
    // exports main.views;
    
    opens main to javafx.fxml;
}
