/*
    Password Manager: Manages accounts given by user with encrypted password.
    Copyright (C) 2022-2023  Francesco Marras

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
package main;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class Logger {
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final File logFile;
    private final StringBuilder logHistory;

    public Logger(String filePath) {
        logFile = new File(filePath + "report.log");
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
        try (Scanner scanner = new Scanner(logFile)) {
            logHistory.append(scanner.useDelimiter("\\Z").next()).append("\n");
            scanner.close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean save() {
        try (FileWriter w = new FileWriter(logFile)) {
            w.write(logHistory.toString());
            w.close();

            return true;
        } catch (IOException ex) {
            return false;
        }
    }
}
