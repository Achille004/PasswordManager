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
import org.panteleyev.jpackage.JPackageTask

class FullJPackageTask extends JPackageTask {
    static def isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
    static def isMac = Os.isFamily(Os.FAMILY_MAC)
    static def isUnix = Os.isFamily(Os.FAMILY_UNIX) && !isMac

    static List<ImageType> addImgType(String command, String toolsetName, List<ImageType> types) {
        def success = command.execute().waitFor() == 0
        if (!success) return []

        println " - ${toolsetName} found"
        return types
    }

    @TaskAction
    @Override
    void action() {
        println "Packaging ${appName} ${appVersion}"
        List<ImageType> types = []

        if (isWindows) {
            println "Targeting Windows"
            types += addImgType('cmd /C wix --version', "WiX toolset", [ImageType.EXE, ImageType.MSI])
        } else if (isMac) {
            println "Targeting Mac"
            types += [ImageType.DMG, ImageType.PKG]
        } else if (isUnix) {
            println "Targeting Linux/Unix"
            types += addImgType('bash -c which dpkg-deb', "DEB package manager", [ImageType.DEB])
            types += addImgType('bash -c which rpmbuild', "RPM package manager", [ImageType.RPM])
        }

        if (types.isEmpty()) {
            println "OS not listed as target, building only the app image"
            types += ImageType.APP_IMAGE
        }

        println "Building"
        types.each { type ->
            print " - ${type}..."
            System.out.flush()

            setType(type)
            super.action()

            println " OK"
        }

        def arch = System.getProperty('os.arch')
        println "Renaming files for ${arch}"

        def outDir = new File(destination)
        outDir.listFiles().each { f ->
            if (!f.name.contains("-${arch}")) {
                def name = f.name
                def dot  = name.lastIndexOf('.')
                def newName = (dot >= 0)
                    ? "${name.substring(0, dot)}-${arch}${name.substring(dot)}"
                    : "${name}-${arch}"

                def newFile = new File(f.parentFile, newName)
                println " - ${f.name} => ${newFile.name}"
                f.renameTo(newFile)

                // Make sure the new file is writable (WiX was building read-only EXEs
                // but ok MSIs, and I'm not gonna dive into that rabbit hole right now)
                if (!newFile.canWrite()) {
                    newFile.setWritable(true)
                    println "   L Was read-only, removed attribute"
                } 
            }
        }
    }
}