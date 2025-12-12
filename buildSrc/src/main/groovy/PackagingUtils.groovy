/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2025  Francesco Marras (2004marras@gmail.com)

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

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal
import org.panteleyev.jpackage.ImageType

class PackagingUtils {
    static def isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
    static def isMac = Os.isFamily(Os.FAMILY_MAC)
    static def isUnix = Os.isFamily(Os.FAMILY_UNIX) && !isMac

    static List<ImageType> addImgType(String command, String toolsetName, List<ImageType> types) {
        def success = command.execute().waitFor() == 0
        if (!success) return []

        println " - ${toolsetName} found"
        return types
    }

    static List<ImageType> getTypes() {
        List<ImageType> types = [ImageType.APP_IMAGE]

        if (isWindows) {
            println "Targeting Windows"
            types += addImgType("wix --version", "WiX toolset", [ImageType.EXE, ImageType.MSI])
        } else if (isMac) {
            println "Targeting Mac"
            types += [ImageType.DMG, ImageType.PKG]
        } else if (isUnix) {
            println "Targeting Linux/Unix"
            types += addImgType("which dpkg-deb", "DEB package manager", [ImageType.DEB])
            types += addImgType("which rpmbuild", "RPM package manager", [ImageType.RPM])
        }

        if (types.isEmpty()) println "No package builder found"
        return types
    }
}