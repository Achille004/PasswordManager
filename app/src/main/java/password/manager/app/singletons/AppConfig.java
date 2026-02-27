/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2026  Francesco Marras (2004marras@gmail.com)

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

package password.manager.app.singletons;

import java.nio.file.Path;

import lombok.Getter;
import password.manager.app.App;

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
