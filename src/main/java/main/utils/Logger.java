/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2024  Francesco Marras

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

package main.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Scanner;

import org.jetbrains.annotations.NotNull;

public class Logger {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT,
            FormatStyle.MEDIUM);

    private final File logFile;
    private final StringBuilder logHistory;

    public Logger(File logFile) {
        this.logFile = logFile;
        logHistory = new StringBuilder();
    }

    public void addInfo(String str) {
        logHistory
                .append(dtf.format(LocalDateTime.now()))
                .append(" >>> ")
                .append(str)
                .append("\n");
    }

    public void addError(@NotNull Exception e) {
        logHistory
                .append(dtf.format(LocalDateTime.now()))
                .append(" !!! ")
                .append(e.getMessage())
                .append("\n");
    }

    public String getLogHistory() {
        return logHistory.toString();
    }

    public boolean readFile() {
        if (!logFile.exists()) {
            addInfo("File not found: '" + logFile.toString() + "'");
            return false;
        }

        try (Scanner scanner = new Scanner(logFile)) {
            logHistory.append(scanner.useDelimiter("\\Z").next()).append("\n\n");
        } catch (IOException e) {
            addError(e);
            return false;
        }

        addInfo("File loaded: '" + logFile + "'");
        return true;
    }

    public boolean save() {
        try (FileWriter w = new FileWriter(logFile)) {
            addInfo("Log file saved");

            w.write(logHistory.toString());
            w.close();

            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
