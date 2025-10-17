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

package password.manager.app.singletons;

import static password.manager.app.Utils.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import password.manager.app.App;

public final class Logger {
    public static final String FOLDER_PREFIX, LOG_FILE_NAME, STACKTRACE_FILE_NAME;
    public static final int MAX_LOG_FILES;

    private static final DateTimeFormatter FILE_DTF, MSG_DTF;

    private static final String INITIAL_MESSAGE = String.format("""
    =========== %s %s by Francesco Marras ===========

                            %s

              --- Debug          >>> Info          !!! Error

            WARNING: Debug entries may contain sensitive data!

    ==================================================================

    """, App.APP_NAME, App.APP_VERSION, "%s");

    static {
        FOLDER_PREFIX = "log_";
        LOG_FILE_NAME = "report.log";
        STACKTRACE_FILE_NAME = "stacktrace.log";
        MAX_LOG_FILES = 5;

        FILE_DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
        MSG_DTF = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
    }

    private final Path currPath;
    private final FileWriter logWriter, stacktraceWriter;

    private Logger(Path filePath) {
        rotateLogs(filePath);

        this.currPath = filePath.resolve(FOLDER_PREFIX + FILE_DTF.format(LocalDateTime.now()));
        currPath.toFile().mkdirs();

        logWriter = getFileWriter(currPath.resolve(LOG_FILE_NAME), false);
        Objects.requireNonNull(logWriter, "logWriter must not be null");

        stacktraceWriter = getFileWriter(currPath.resolve(STACKTRACE_FILE_NAME), false);
        Objects.requireNonNull(stacktraceWriter, "stacktraceWriter must not be null");

        final String MSG = String.format(INITIAL_MESSAGE, MSG_DTF.format(LocalDateTime.now()));
        write(logWriter, new StringBuilder(MSG));
    }

    public Path getLoggingPath() {
        return currPath;
    }

    public void addDebug(String str) {
        StringBuilder logStrBuilder = new StringBuilder();
        logStrBuilder
                .append(MSG_DTF.format(LocalDateTime.now()))
                .append(" --- ")
                .append(str)
                .append("\n");

        write(logWriter, logStrBuilder);
    }

    public void addInfo(String str) {
        StringBuilder logStrBuilder = new StringBuilder();
        logStrBuilder
                .append(MSG_DTF.format(LocalDateTime.now()))
                .append(" >>> ")
                .append(str)
                .append("\n");

        write(logWriter, logStrBuilder);
    }

    public void addError(@NotNull Throwable e) {
        StringBuilder logStrBuilder = new StringBuilder();
        logStrBuilder
                .append(MSG_DTF.format(LocalDateTime.now()))
                .append(" !!! An exception has been thrown. See '")
                .append(STACKTRACE_FILE_NAME)
                .append("' for details.\n");

        // Write the stack trace to the stacktrace log file
        StringBuilder stacktraceStrBuilder = new StringBuilder();
        stacktraceStrBuilder
                .append(MSG_DTF.format(LocalDateTime.now()))
                .append(" => Exception thrown while executing '")
                .append(getCurrentMethodName(1))
                .append("', follows error and full stack trace:\n");

        try {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            stacktraceStrBuilder.append(sw).append("\n");
            pw.close();
            sw.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }

        write(logWriter, logStrBuilder);
        write(stacktraceWriter, stacktraceStrBuilder);
    }

    private static @NotNull String getCurrentMethodName(@NotNull Integer walkDist) {
        StackTraceElement[] sckTrc = Thread.currentThread().getStackTrace();
        if (sckTrc.length >= walkDist + 2) {
            StackTraceElement stckTrcElem = sckTrc[walkDist + 2];
            return stckTrcElem.getClassName() + '.' + stckTrcElem.getMethodName();
        } else {
            return "unknown";
        }
    }

    public void closeStreams() {
        try {
            addInfo("Closing logger streams");
            logWriter.close();
            stacktraceWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void write(@NotNull FileWriter writer, @NotNull StringBuilder builder) {
        try {
            writer.write(builder.toString());
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void rotateLogs(@NotNull Path filePath) {
        try {
            // Get the list of log directories
            File logDir = filePath.toFile();
            File[] logDirs = logDir.listFiles((_, name) -> name.startsWith(FOLDER_PREFIX));

            if (logDirs != null && logDirs.length > MAX_LOG_FILES - 1) {
                // Sort the directories by last modified date, oldest first
                Arrays.sort(logDirs, Comparator.comparingLong(File::lastModified));

                // Delete the oldest directories if the count exceeds MAX_LOG_FILES
                for (int i = 0; i < logDirs.length - MAX_LOG_FILES + 1; i++) {
                    deleteDirectory(logDirs[i].toPath());
                }
            }
        } catch (IOException e) {
            addError(e);
        }
    }

    private void deleteDirectory(Path path) throws IOException {
        try (var paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    // #region Singleton methods
    public static synchronized void createInstance(Path baseLogPath) throws IllegalStateException {
        Singletons.register(Logger.class, new Logger(baseLogPath));
    }

    public static @NotNull Logger getInstance() {
        return Singletons.get(Logger.class);
    }
    // #endregion
}
