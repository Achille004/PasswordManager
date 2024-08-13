/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras (2004marras@gmail.com)

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

package password.manager.utils;

import static password.manager.utils.Utils.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Arrays;
import java.util.Comparator;

import org.jetbrains.annotations.NotNull;

public final class Logger {
    private static final String FOLDER_PREFIX, FILE_NAME;
    private static final int MAX_LOG_FILES;

    private static final DateTimeFormatter FILE_DTF;
    private static final DateTimeFormatter DTF;

    static {
        FOLDER_PREFIX = "log_";
        FILE_NAME = "report.log";
        MAX_LOG_FILES = 5;

        FILE_DTF = DateTimeFormatter.ofPattern("yyyy.MM.dd_HH.mm.ss");
        DTF = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.MEDIUM);
    }

    private final Path currPath;
    private final FileWriter writer;
    private final StringBuilder strBuilder;

    public Logger(Path filePath) {
        rotateLogs(filePath);
        
        this.currPath = filePath.resolve(FOLDER_PREFIX + FILE_DTF.format(LocalDateTime.now()));
        currPath.toFile().mkdirs();

        writer = getFileWriter(currPath.resolve(FILE_NAME), false);
        strBuilder = new StringBuilder();
    }

    public @NotNull Boolean addInfo(String str) {
        strBuilder
                .append(DTF.format(LocalDateTime.now()))
                .append(" >>> ")
                .append(str)
                .append("\n");

        return write();
    }

    public @NotNull Boolean addError(@NotNull Exception e) {
        strBuilder
                .append(DTF.format(LocalDateTime.now()))
                .append(" !!! An exception has been thrown, stack trace:\n")
                .append(e.getClass().getName())
                .append(": ")
                .append(e.getMessage())
                .append("\n");

        for (StackTraceElement element : e.getStackTrace()) {
            strBuilder.append("        ").append(element).append('\n');
        }

        return write();
    }

    public void closeStream() {
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private @NotNull Boolean write() {
        try {
            writer.write(strBuilder.toString());
            writer.flush();
            strBuilder.setLength(0);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void rotateLogs(Path filePath) {
        try {
            // Get the list of log directories
            File logDir = filePath.toFile();
            File[] logDirs = logDir.listFiles((dir, name) -> name.startsWith(FOLDER_PREFIX));

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
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
    }
}
