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

    // Apply the signing
    id("signing")

    // TODO REMOVE
    id("io.franzbecker.gradle-lombok") version "5.0.0"
}

dependencies {
    implementation(project(":lib"))
}

mainModuleInfo {
    // TODO REMOVE
    annotationProcessor("lombok")
}

testModuleInfo {
    // TODO REMOVE
    annotationProcessor("lombok")
}

// #region Common configuration and utilities

val appName: String by project
val appVersion: String by project
val appVendor: String by project
val debugMode: String by project

val BUILD_GROUP: String = "build"
val DISTRIBUTION_GROUP: String = "distribution"

// https://github.com/controlsfx/controlsfx/wiki/Using-ControlsFX-with-JDK-9-and-above
val defaultJvmArgs: List<String> = listOf(
    "--enable-native-access=javafx.graphics",
    "--add-exports", "javafx.base/com.sun.javafx.event=org.controlsfx.controls",
    "-Dapp.name=$appName",
    "-Dapp.version=$appVersion"
)

val resourceDir: Directory = layout.projectDirectory.dir("src/main/resources")
resourceDir.asFile.mkdirs()

val compileDir: Directory = rootProject.layout.projectDirectory.dir("compiled")
compileDir.asFile.mkdirs()

// #endregion

application {
    // Define the main class for the application.
    mainModule = "password.manager.app"
    mainClass = "password.manager.app.App"

    // JVM arguments
    applicationDefaultJvmArgs = defaultJvmArgs
}

tasks.jar {
    archiveBaseName.set(rootProject.name)
    manifest {
        attributes(
            "Implementation-Title" to appName,
            "Main-Class" to "password.manager.app.App"
        )
    }
}

///// Packaging tasks and utilities /////

apply(from = "portables.gradle.kts")
apply(from = "installers.gradle.kts")

tasks.register("assembleAll") {
    group = DISTRIBUTION_GROUP
    dependsOn(tasks.named("assemblePortables"))
    dependsOn(tasks.named("assembleInstallers"))
}