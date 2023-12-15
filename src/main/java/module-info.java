module PasswordManager.main {
    requires transitive java.desktop;
    requires java.logging;

    requires org.jetbrains.annotations;

    exports main;
    exports main.security;
    exports main.utils;
    exports main.views;
}
