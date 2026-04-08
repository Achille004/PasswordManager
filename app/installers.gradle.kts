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

import org.gradle.api.file.Directory
import org.gradle.api.tasks.Delete
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask
import java.io.File

// #region Common configuration and utilities

val appName: String by project
val appVersion: String by project
val appVendor: String by project
val debugMode: String by project

val BUILD_GROUP = "build"
val DISTRIBUTION_GROUP = "distribution"

// https://github.com/controlsfx/controlsfx/wiki/Using-ControlsFX-with-JDK-9-and-above
val defaultJvmArgs = listOf(
    "--enable-native-access=javafx.graphics",
    "--add-exports", "javafx.base/com.sun.javafx.event=org.controlsfx.controls",
    "-Dapp.name=$appName",
    "-Dapp.version=$appVersion"
)

val resourceDir = layout.projectDirectory.dir("src/main/resources")
resourceDir.asFile.mkdirs()

val compileDir = rootProject.layout.projectDirectory.dir("compiled")
compileDir.asFile.mkdirs()

// #endregion

// appName and appVersion are inherited from JPackageTask
val osArch = System.getProperty("os.arch")
val basename = appName.replace(" ", "") + "-" + appVersion + "-" + osArch

val types = PackagingUtils.getTypes()

val installerDir = compileDir.dir("installers")
var dest: Directory? = null

types.forEach { t ->
    val isAppImage = (t == ImageType.APP_IMAGE)
    val appImgDest = installerDir.dir(appName)
    println("Configuring packaging task for $t")

    tasks.register<JPackageTask>("jpackage_$t") {
        group = DISTRIBUTION_GROUP

        onlyIf { task ->
            if (PackagingUtils.isToolAvailableFor(t)) return@onlyIf true
            println("Skipping ${task.path}: required tool for $t is not available")
            return@onlyIf false
        }

        outputs.upToDateWhen { false }
        dependsOn(tasks.named("readyAllJars"))

        if (!isAppImage) dependsOn(tasks.named("jpackage_APP_IMAGE"))

        type = t

        appDescription = appName
        this.appName = appName
        this.appVersion = appVersion
        copyright = "© $appVendor"
        vendor = appVendor

        if (isAppImage) {
            dependsOn(tasks.named("delAppImage"))

            javaOptions = listOf("-Dfile.encoding=UTF-8") + defaultJvmArgs
            module = "password.manager.app/password.manager.app.App"
            modulePaths = files(layout.buildDirectory.dir("jars"))
            runtimeImage = file(System.getProperty("java.home"))
        } else {
            aboutUrl = "https://github.com/Achille004/PasswordManager"
            appImage = compileDir.dir("appimage").asFile.toString()
            licenseFile = resourceDir.file("license.txt")
        }

        windows {
            dest = installerDir.dir("windows")
            icon = resourceDir.file("icon.ico")

            winConsole = debugMode.toBoolean()

            if (!isAppImage) {
                dependsOn(tasks.named("delWindowsInstallers"))

                winDirChooser = true
                winHelpUrl = "https://github.com/Achille004/PasswordManager/issues"
                winMenu = true
                winMenuGroup = appVendor
                winShortcut = true
                winShortcutPrompt = true
            }
        }

        mac {
            dest = installerDir.dir("mac")
            icon = resourceDir.file("icon.icns")

            if (!isAppImage) {
                dependsOn(tasks.named("delMacInstallers"))

                macAppCategory = "security"
                macAppStore = false
                macPackageName = appName
                macPackageIdentifier = ((appName as String).replace(" ", "") + "-" + appVersion)
                macSigningKeyUserName = appVendor
            }
        }

        linux {
            dest = installerDir.dir("linux")
            icon = resourceDir.file("icon.png")

            if (!isAppImage) {
                dependsOn(tasks.named("delLinuxInstallers"))

                linuxAppCategory = "security"
                linuxDebMaintainer = "2004marras@gmail.com"
                linuxMenuGroup = appVendor
                linuxRpmLicenseType = "GPLv3"
                linuxShortcut = true
            }
        }

        destination = if (isAppImage) compileDir else dest

        // Rename the file to the desired format
        doLast {
            val outFile = destination.get().asFile.listFiles()
                ?.find { file ->
                    val toMatch = (if (isAppImage) appImgDest.asFile.name else t.toString()).lowercase()
                    file.name.lowercase().contains(toMatch)
                }

            if (outFile == null) return@doLast

            val targetName = if (isAppImage) {
                "appimage"
            } else {
                val dot = outFile.name.lastIndexOf(".")
                if (dot >= 0) basename + outFile.name.substring(dot) else basename
            }

            val newFile = File(outFile.parentFile, targetName)
            outFile.renameTo(newFile)
            outFile.delete()

            println("Renamed '${outFile.name}' to '${newFile.name}'")

            // Make sure the new file is writable (WiX was building read-only EXEs
            // but ok MSIs, and I'm not gonna dive into that rabbit hole right now)
            if (!newFile.canWrite()) {
                newFile.setWritable(true)
                println("  L Was read-only, removed attribute")
            }
        }
    }
}

tasks.register("assembleInstallers") {
    group = DISTRIBUTION_GROUP

    installerDir.asFile.mkdirs()

    println("Packaging $appName $appVersion")

    println("Building candidates: ${types.joinToString(", ")}")
    dependsOn(types.map { type -> tasks.named("jpackage_$type") })
}

tasks.register("dryAssembleInstallers") {
    group = DISTRIBUTION_GROUP

    System.setProperty("jpackage.dryRun", "true")
    finalizedBy(tasks.named("assembleInstallers"))
}

///// CLEANUP TASKS /////

tasks.register<Delete>("delAppImage") {
    group = BUILD_GROUP
    outputs.upToDateWhen { false }

    val appImageDir = compileDir.dir("appimage").asFile
    if (!appImageDir.exists()) return@register

    delete(appImageDir)
    println("Removed app image directory")
}

listOf("windows", "mac", "linux").forEach { platform ->
    tasks.register<Delete>("del${platform.replaceFirstChar { it.uppercase() }}Installers") {
        group = BUILD_GROUP
        outputs.upToDateWhen { false }

        val platformDir = installerDir.dir(platform).asFile
        if (!platformDir.exists()) return@register

        delete(platformDir.listFiles())
        println("Cleaned ${platform.replaceFirstChar { it.uppercase() }} directory")
    }
}