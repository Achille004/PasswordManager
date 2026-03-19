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

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal
import org.panteleyev.jpackage.ImageType

class PackagingUtils {
    static boolean isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
    static boolean isMac = Os.isFamily(Os.FAMILY_MAC)
    static boolean isUnix = Os.isFamily(Os.FAMILY_UNIX) && !isMac

    static String getProbeCommand(ImageType type) {
        switch (type) {
            case ImageType.EXE:
            case ImageType.MSI:
                return "wix --version"
            case ImageType.DEB:
                return "which dpkg-deb"
            case ImageType.RPM:
                return "which rpmbuild"
            default:
                return null
        }
    }

    static boolean isToolAvailableFor(ImageType type) {
        String command = getProbeCommand(type)
        if (command == null) return true

        return command.execute().waitFor() == 0
    }

    static List<ImageType> getTypes() {
        List<ImageType> types = [ImageType.APP_IMAGE]

        if (isWindows) {
            println "Targeting Windows"
            types += [ImageType.EXE, ImageType.MSI]
        } else if (isMac) {
            println "Targeting Mac"
            types += [ImageType.DMG, ImageType.PKG]
        } else if (isUnix) {
            println "Targeting Linux/Unix"
            types += [ImageType.DEB, ImageType.RPM]
        }

        if (types.isEmpty()) println "No package builder found"
        return types
    }
}