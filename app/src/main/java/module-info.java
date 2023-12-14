module main {
    requires transitive java.desktop;
    requires java.logging;
    
    exports main;
    exports main.security;
    exports main.utils;
    exports main.views;
}
