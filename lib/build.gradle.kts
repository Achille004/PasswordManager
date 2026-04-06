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
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "2.3.20"

    // Apply the application plugin to add support for building a CLI application in Java.
    id("application")

    id("org.openjfx.javafxplugin") version "0.1.0"

    // https://github.com/gradlex-org/java-module-dependencies
    id("org.gradlex.java-module-dependencies") version "1.12.1"

    id("com.adarshr.test-logger") version "4.0.0"
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

// Apply a specific Java toolchain to ease working on different environments.
java {
    sourceCompatibility = JavaVersion.VERSION_25
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    // Define the main class for the application.
    mainModule = "password.manager.lib"
    mainClass = "password.manager.lib.Main"

    // JVM arguments
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics")
}

testing.suites {
    val test by getting(JvmTestSuite::class) {
        useJUnitJupiter("6.0.3")
    }
}

javafx {
    version = "25.0.2"
    modules = listOf("javafx.base", "javafx.controls", "javafx.fxml", "javafx.graphics")
}

val appName: String by project

tasks.compileJava {
    dependsOn(tasks.compileKotlin)
}

tasks.jar {
    archiveBaseName.set(rootProject.name + "Components")
    manifest {
        attributes("Implementation-Title" to "Custom components for $appName")
    }
}
