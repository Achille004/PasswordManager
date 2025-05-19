import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.Internal
import org.panteleyev.jpackage.ImageType
import org.panteleyev.jpackage.JPackageTask

class FullJPackageTask extends JPackageTask {
    @Internal
    def isWindows = Os.isFamily(Os.FAMILY_WINDOWS)
    @Internal
    def isMac = Os.isFamily(Os.FAMILY_MAC)
    @Internal
    def isLinux = Os.isFamily(Os.FAMILY_UNIX) && !isMac

    @TaskAction
    @Override
    void action() {
        List<ImageType> types = []
        def devNull = OutputStream.nullOutputStream()

        println "Packaging ${appName} ${appVersion}"

        if (isWindows) {
            println "Targeting Windows"
            
            def hasWix = project.exec {
                ignoreExitValue = true
                standardOutput = devNull
                errorOutput    = devNull
                executable 'cmd'
                args '/C', 'wix --version'
            }
            if (hasWix.getExitValue() == 0) {
                println " - WiX toolset found"
                types += [ImageType.EXE, ImageType.MSI]
            }
        } else if (isMac) {
            println "Targeting Mac"
            types += [ImageType.DMG, ImageType.PKG]
        } else if (isLinux) {
            println "Targeting Linux/Unix"

            def hasDeb = project.exec {
                ignoreExitValue = true
                standardOutput = devNull
                errorOutput    = devNull
                executable 'bash'
                args '-c', 'which dpkg-deb'
            }
            if (hasDeb.getExitValue() == 0) {
                println " - Debian package manager found"
                types += ImageType.DEB
            }

            def hasRpm = project.exec {
                ignoreExitValue = true
                standardOutput = devNull
                errorOutput    = devNull
                executable 'bash'
                args '-c', 'which rpmbuild'
            }
            if (hasRpm.getExitValue() == 0) {
                println " - RPM package manager found"
                types += ImageType.RPM
            }
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
                f.renameTo(new File(f.parentFile, newName))
            }
        }
    }
}