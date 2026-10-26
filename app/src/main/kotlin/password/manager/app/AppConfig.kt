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
package password.manager.app

import java.nio.file.Path
import java.util.*

object AppConfig {
    const val INJECTED_BASE_PATH_KEY: String = "app.config.basePath"

    @JvmStatic
    private val WINDOWS_PATH = Path.of("AppData", "Local", App.APP_NAME).toString()
    private const val OS_FALLBACK_PATH = ".password-manager"

    @JvmStatic
    val operatingSystem: String = System.getProperty("os.name")
    @JvmStatic
    val userHome: String = System.getProperty("user.home")
    @JvmStatic
    val basePath: Path = run {
        // Initialize this alongside the other object properties so the first read is always safe.
        val injectedBasePath = System.getProperty(INJECTED_BASE_PATH_KEY)
        if (injectedBasePath.isNullOrBlank()) {
            Path.of(
                userHome,
                if (operatingSystem.lowercase(Locale.ROOT).contains("windows"))
                    WINDOWS_PATH
                else
                    OS_FALLBACK_PATH
            )
        } else {
            Path.of(injectedBasePath)
        }
    }
}
