import java.io.File
import org.gradle.plugins.signing.SigningExtension
import org.gradle.api.tasks.bundling.Tar
import org.gradle.api.tasks.bundling.Zip

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

val gpgExists = listOf("gpg", "gpg.exe").any { executable ->
    System.getenv("PATH").split(File.pathSeparator).any { path -> File(path, executable).exists() }
}
val gpgPropertiesExist = (project.hasProperty("signing.keyId")
        && project.hasProperty("signing.password")
        && project.hasProperty("signing.secretKeyRingFile"))

configure<SigningExtension> {
    if(!gpgExists) {
        println("Gpg executable not found in PATH")
    } else if(!gpgPropertiesExist) {
        println("Gpg signing properties missing")
    } else {
        println("Gpg signing enabled")
        useGpgCmd()
        sign(tasks.named<Zip>("distZip").get())
        sign(tasks.named<Tar>("distTar").get())
    }
}

val gpgSignable = gpgExists && gpgPropertiesExist
val portableDistDir = compileDir.dir("portables")

tasks.register<Copy>("assemblePortableTar") {
    group = DISTRIBUTION_GROUP

    dependsOn(tasks.named("distTar"))
    if(gpgSignable) dependsOn(tasks.named("signDistTar"))

    from(tasks.named("distTar"))
    if(gpgSignable) from(tasks.named("signDistTar"))
    into(portableDistDir.dir("tar"))
}

tasks.register<Copy>("assemblePortableZip") {
    group = DISTRIBUTION_GROUP

    dependsOn(tasks.named("distZip"))
    if(gpgSignable) dependsOn(tasks.named("signDistZip"))

    from(tasks.named("distZip"))
    if(gpgSignable) from(tasks.named("signDistZip"))
    into(portableDistDir.dir("zip"))
}

tasks.register("assemblePortables") {
    group = DISTRIBUTION_GROUP

    dependsOn(tasks.named("assemblePortableTar"), tasks.named("assemblePortableZip"))
}

tasks.register<Copy>("readyAllJars") {
    group = BUILD_GROUP

    dependsOn(tasks.named("jar"))

    from(configurations.named("runtimeClasspath"))
    from(tasks.named<Jar>("jar").flatMap { it.archiveFile })
    into(layout.buildDirectory.dir("jars"))
}