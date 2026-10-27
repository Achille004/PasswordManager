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
package password.manager.app.singletons

import password.manager.app.App
import password.manager.app.AppConfig
import password.manager.app.Utils
import java.io.*
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

// Let only package classes instantiate this
object Logger : Singleton() {
    private const val FOLDER_PREFIX = "log_"
    private const val LOG_FILE_NAME = "report.log"
    private const val STACKTRACE_FILE_NAME = "stacktrace.log"
    private const val MAX_LOG_FILES = 5

    private val INITIAL_MESSAGE = String.format(
        """
            =========== %s %s by Francesco Marras ===========

                                    %s

                      --- Debug          >>> Info          !!! Error

                    WARNING: Debug entries may contain sensitive data!

            ==================================================================

            """.trimIndent(), App.APP_NAME, App.APP_VERSION, "%s"
    )

    private val FILE_DTF: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
    private val MSG_DTF: DateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM)

    val loggingPath: Path
    private val logWriter: FileWriter
    private val stacktraceWriter: FileWriter
    private val errCounter: AtomicInteger = AtomicInteger(0)

    init {
        val filePath: Path = AppConfig.basePath.resolve("logs")
        rotateLogs(filePath)

        this.loggingPath = filePath.resolve(FOLDER_PREFIX + FILE_DTF.format(LocalDateTime.now()))
        loggingPath.toFile().mkdirs()

        logWriter = Utils.getFileWriter(loggingPath.resolve(LOG_FILE_NAME), false)
        stacktraceWriter = Utils.getFileWriter(loggingPath.resolve(STACKTRACE_FILE_NAME), false)

        val msg: String = String.format(INITIAL_MESSAGE, MSG_DTF.format(LocalDateTime.now())) + "\n"
        write(logWriter, StringBuilder(msg))

        // Log here since doing it in AppConfig would result in circular dependency
        addDebug("os.name: '${AppConfig.operatingSystem}'")
        addDebug("user.home: '${AppConfig.userHome}'")

        registerSingleton()
    }

    fun addDebug(str: String, vararg args: Any?) {
        val formattedStr = String.format(Locale.ROOT, str, *args)

        val logStrBuilder = StringBuilder()
        logStrBuilder
            .append(MSG_DTF.format(LocalDateTime.now()))
            .append(" --- ")
            .append(formattedStr)
            .append("\n")

        write(logWriter, logStrBuilder)
    }

    fun addInfo(str: String, vararg args: Any?) {
        val formattedStr = String.format(Locale.ROOT, str, *args)

        val logStrBuilder = StringBuilder()
        logStrBuilder
            .append(MSG_DTF.format(LocalDateTime.now()))
            .append(" >>> ")
            .append(formattedStr)
            .append("\n")

        write(logWriter, logStrBuilder)
    }

    fun addError(e: Throwable) {
        val currentErrCount = errCounter.incrementAndGet()

        val logStrBuilder = StringBuilder()
        logStrBuilder
            .append(MSG_DTF.format(LocalDateTime.now()))
            .append(" !!! An exception has been thrown, id=$currentErrCount. See '")
            .append(STACKTRACE_FILE_NAME)
            .append("' for details.\n")

        // Write the stack trace to the stacktrace log file
        val stacktraceStrBuilder = StringBuilder()
        stacktraceStrBuilder
            .append(MSG_DTF.format(LocalDateTime.now()))
            .append(" => Exception id=$currentErrCount thrown while executing '")
            .append(getCurrentMethodName(1))
            .append("', follows error and full stack trace:\n")

        try {
            StringWriter().use { sw -> PrintWriter(sw).use { pw ->
                e.printStackTrace(pw)
                stacktraceStrBuilder.append(sw).append("\n")
            } }
        } catch (e1: IOException) {
            e1.printStackTrace()
        }

        write(logWriter, logStrBuilder)
        write(stacktraceWriter, stacktraceStrBuilder)
    }

    // #region Private methods
    @Synchronized
    private fun write(writer: FileWriter, buffer: StringBuilder) =
        synchronized(writer) {
            try {
                writer.write(buffer.toString())
                writer.flush()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

    private fun rotateLogs(filePath: Path) {
        // Get the list of log directories
        val logDir = filePath.toFile()
        val logDirs = logDir.listFiles { _, name: String? -> name?.startsWith(FOLDER_PREFIX) ?: false }
        if (logDirs == null) return

        // Delete the oldest directories if the count exceeds MAX_LOG_FILES - 1 (the -1 is for the current log)
        val limit: Int = logDirs.size - MAX_LOG_FILES + 1
        if (limit <= 0) return

        logDirs.sortedBy(File::lastModified)
            .take(limit)
            .forEach { directory -> deleteDirectory(directory.toPath()) }
    }

    private fun deleteDirectory(path: Path) =
        try {
            Files.walk(path).use { paths ->
                val toDelete = paths.sorted(Comparator.reverseOrder<Path?>()).toList()
                for (p in toDelete) Files.delete(p)
            }
        } catch (e: IOException) {
            addError(e)
        }

    @JvmStatic
    private fun getCurrentMethodName(walkDist: Int): String {
        val sckTrc = Thread.currentThread().stackTrace
        if (sckTrc.size < walkDist + 2) return "<unknown>"

        val stckTrcElem = sckTrc[walkDist + 2]
        return stckTrcElem.className + '.' + stckTrcElem.methodName
    }
    // #endregion

    override fun close() =
        try {
            addInfo("Closing logger streams")
            logWriter.close()
            stacktraceWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
}
