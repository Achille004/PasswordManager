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

package password.manager.app;

import java.awt.Desktop;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javafx.scene.layout.Pane;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;

public final class Utils {
    private Utils() {} // Prevent instantiation

    public static final Locale[] SUPPORTED_LOCALE;
    public static final Locale DEFAULT_LOCALE;

    static {
        SUPPORTED_LOCALE = new Locale[] { Locale.ENGLISH, Locale.ITALIAN };

        final Locale systemLang = Locale.forLanguageTag(Locale.getDefault().getLanguage());
        DEFAULT_LOCALE = Arrays.asList(SUPPORTED_LOCALE).contains(systemLang)
                ? systemLang
                : Locale.ENGLISH;
    }

    private static final Encoder BASE64ENC = Base64.getEncoder();
    private static final Decoder BASE64DEC = Base64.getDecoder();

    private static final ReentrantReadWriteLock MEM_RW_LOCK = new ReentrantReadWriteLock(true); // fair mode
    private static final Lock MEM_READ_LOCK = MEM_RW_LOCK.readLock();
    private static final Lock MEM_WRITE_LOCK = MEM_RW_LOCK.writeLock();
    private static final float MEM_SAFETY_FACTOR = 0.8f;
    private static long reservedMemory = 0;

    @SafeVarargs
    public static <T> SortedList<T> getFXSortedList(T @NotNull... items) {
        return FXCollections.observableArrayList(items).sorted(null);
    }

    /**
     * Returns a string containing the index, preceded by zero to match up the same
     * number of digits of the size of the list.
     *
     * @param listSize The size of the list.
     * @param index    The index of the element.
     * @return The index.
     */
    public static @NotNull String addZerosToIndex(@NotNull Integer listSize, @NotNull Integer index) {
        final int listDigits = (int) Math.log10(listSize) + 1;
        return String.format("%0" + listDigits + "d", index);
    }

    public static String byteToBase64(byte[] src) {
        return BASE64ENC.encodeToString(src);
    }

    public static byte[] base64ToByte(@NotNull String src) {
        return BASE64DEC.decode(src);
    }

    public static @NotNull String capitalizeWord(@NotNull String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    @Contract("_, _ -> param1")
    public static @NotNull Alert setDefaultButton(@NotNull Alert alert, ButtonType defBtn) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes())
            ((Button) pane.lookupButton(t)).setDefaultButton(t == defBtn);
        return alert;
    }

    public static @Nullable FileWriter getFileWriter(@NotNull Path path, @NotNull Boolean append) {
        return getFileWriter(path.toFile(), append);
    }

    public static @Nullable FileWriter getFileWriter(@NotNull File file, @NotNull Boolean append) {
        try {
            return new FileWriter(file, append);
        } catch (IOException e) {
            return null;
        }
    }

    public static @NotNull Parent loadFxml(@NotNull String path, @NotNull Initializable controller) {
        final String uiElementPath = path.replace("/fxml/", "").replace(".fxml", "");
        Logger.getInstance().addDebug("Loading [" + uiElementPath + "] pane...");

        Parent parent = null;
        try {
            final FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(Utils.class.getResource(path)));
            loader.setController(controller);
            parent = loader.load();
        } catch (IOException e) {
            Logger.getInstance().addError(e);
        }

        final String outcome = (parent != null) ? "Success" : "Error";
        Logger.getInstance().addDebug(outcome + " [" + uiElementPath + "]");

        if(parent != null) return parent;

        // Since it's a one-time error, just create it during the error process
        final String errMsg = ObservableResourceFactory.getInstance().getValue("ui_error");
        final Alert alert = new Alert(AlertType.ERROR, errMsg, ButtonType.YES, ButtonType.NO);
        setDefaultButton(alert, ButtonType.NO);

        alert.showAndWait();
        if (alert.getResult() == ButtonType.YES) {
            Thread.startVirtualThread(() -> {
                try {
                    Desktop.getDesktop().open(Logger.getInstance().getLoggingPath().toFile());
                } catch (IOException e) {
                    Logger.getInstance().addError(e);
                }
            });
        }
        Platform.exit(); // Exit gracefully (saves data, etc.)
        return new Pane(); // return dummy pane
    }

    /**
     * Waits until sufficient memory is available for an operation.
     * Checks free memory and suggests garbage collection if needed.
     *
     * @param requiredMemory The amount of memory required in bytes.
     * @throws RuntimeException if interrupted while waiting.
     */
    public static void reserveMemory(int requiredMemory) {
        MEM_READ_LOCK.lock();
        try {
            Runtime runtime = Runtime.getRuntime();
            int attempts = 0;
            final int MAX_ATTEMPTS = 100;

            while (attempts < MAX_ATTEMPTS) {
                MEM_READ_LOCK.unlock();
                MEM_WRITE_LOCK.lock();

                try {
                    long maxMemory = runtime.maxMemory();
                    long allocatedMemory = runtime.totalMemory();
                    long freeMemory = runtime.freeMemory();
                    long availableMemory = (maxMemory - allocatedMemory) + freeMemory - reservedMemory;

                    if (availableMemory * MEM_SAFETY_FACTOR > requiredMemory) {
                        reservedMemory += requiredMemory;
                        return;
                    }
                } finally {
                    MEM_WRITE_LOCK.unlock();
                    MEM_READ_LOCK.lock();
                }

                try {
                    System.gc();
                    Thread.sleep(100);
                    attempts++;
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted while waiting for memory", e);
                }
            }

            throw new RuntimeException("Unable to allocate sufficient memory after " + MAX_ATTEMPTS + " attempts");
        } finally {
            MEM_READ_LOCK.unlock();
        }
    }

    /**
     * Releases reserved memory after an Argon2 operation completes.
     * Must be called after the operation that reserved memory via waitForSufficientMemory.
     *
     * @param requiredMemory The amount of memory to release in bytes.
     */
    public static void releaseMemory(int requiredMemory) {
        MEM_WRITE_LOCK.lock();
        try {
            reservedMemory -= requiredMemory;
            if (reservedMemory < 0) reservedMemory = 0;
        } finally {
            MEM_WRITE_LOCK.unlock();
        }
    }
}
