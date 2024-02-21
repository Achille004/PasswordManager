package main.utils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

import org.jetbrains.annotations.NotNull;

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
