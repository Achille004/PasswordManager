package password.manager.app.singletons;

import java.nio.file.Path;

import lombok.Getter;
import password.manager.app.App;
import password.manager.app.base.Singleton;

public class AppConfig extends Singleton {

    public static final String INJECTED_BASE_PATH_KEY = "app.config.basePath";

    private static final String WINDOWS_PATH = Path.of("AppData", "Local", App.APP_NAME).toString();
    private static final String OS_FALLBACK_PATH = ".password-manager";

    private final @Getter String operatingSystem, userHome;
    private final @Getter Path basePath;
    
    // Let only package classes instantiate this
    AppConfig() {
        // gets system properties
        this.operatingSystem = System.getProperty("os.name");
        this.userHome = System.getProperty("user.home");

        // gets the path
        String injectedBasePath = System.getProperty(INJECTED_BASE_PATH_KEY);
        this.basePath = injectedBasePath == null || injectedBasePath.isBlank()
                ? Path.of(userHome, operatingSystem.toLowerCase().contains("windows") ? WINDOWS_PATH : OS_FALLBACK_PATH)
                : Path.of(injectedBasePath);
    }

    // #region Singleton methods
    @Override
    public void close() throws Exception {
        // Nothing to close
    }

    public static AppConfig getInstance() {
        return Singletons.get(AppConfig.class);
    }
    // #endregion
}
