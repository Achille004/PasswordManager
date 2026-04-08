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

plugins {
    id("common-buildscript")
}

application {
    // Define the main class for the application.
    mainModule = "password.manager.lib"
    mainClass = "password.manager.lib.Main"

    // JVM arguments
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}

val appName: String by project

tasks.jar {
    archiveBaseName.set(rootProject.name + "Components")
    manifest {
        attributes("Implementation-Title" to "Custom components for $appName")
    }
}
