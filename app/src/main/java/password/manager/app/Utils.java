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
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.Pane;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import password.manager.app.controllers.AbstractController;
import password.manager.app.singletons.Logger;
import password.manager.app.singletons.ObservableResourceFactory;

public final class Utils {

    private static final Encoder BASE64ENC = Base64.getEncoder();
    private static final Decoder BASE64DEC = Base64.getDecoder();

    private static final float MEM_SAFETY_FACTOR = 0.8f;
    private static final AtomicLong reservedMemory = new AtomicLong(0);

    private Utils() {} // Prevent instantiation

    /**
     * Returns a SortedList containing the given items, sorted using the default comparator.
     * @param <T> The type of the items.
     * @param items The items to include in the SortedList.
     * @return A SortedList containing the given items.
     */
    @SafeVarargs
    public static <T> SortedList<T> getFXSortedList(@NotNull T... items) {
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

    /**
     * Encodes a byte array to a Base64 string.
     * @param src The byte array to encode.
     * @return The Base64 encoded string.
     */
    public static String byteToBase64(byte[] src) {
        return BASE64ENC.encodeToString(src);
    }
    
    /**
     * Decodes a Base64 string to a byte array.
     * @param src The Base64 string to decode.
     * @return The decoded byte array.
     */
    public static byte[] base64ToByte(@NotNull String src) {
        return BASE64DEC.decode(src);
    }

    /**
     * Capitalizes the first letter of the given word and makes the rest lowercase.
     * @param str The word to capitalize.
     * @return The capitalized word.
     */
    public static @NotNull String capitalizeWord(@NotNull String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Sets the default button for the given Alert dialog.
     * @param alert  The Alert dialog to modify.
     * @param defBtn The ButtonType to set as default.
     * @return The modified Alert dialog.
     */
    @Contract("_, _ -> param1")
    public static @NotNull Alert setDefaultButton(@NotNull Alert alert, ButtonType defBtn) {
        DialogPane pane = alert.getDialogPane();
        for (ButtonType t : alert.getButtonTypes())
            ((Button) pane.lookupButton(t)).setDefaultButton(t == defBtn);
        return alert;
    }

    /**
     * Gets a FileWriter for the specified path.
     * @param path The path to the file.
     * @param append Whether to append to the file.
     * @return A FileWriter instance or null if an error occurs.
     */
    public static @Nullable FileWriter getFileWriter(@NotNull Path path, @NotNull Boolean append) {
        return getFileWriter(path.toFile(), append);
    }

    /**
     * Gets a FileWriter for the specified file.
     * @param file The file to write to.
     * @param append Whether to append to the file.
     * @return A FileWriter instance or null if an error occurs.
     */
    public static @Nullable FileWriter getFileWriter(@NotNull File file, @NotNull Boolean append) {
        try {
            return new FileWriter(file, append);
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Loads an FXML file and sets its controller.
     * @param controller The controller to set for the FXML.
     * @return The loaded Parent node.
     */
    public static <T extends AbstractController> @NotNull Parent loadFxml(@NotNull T controller) {
        final String path = controller.getFxmlPath();

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
        return new Pane(); // return non-null dummy pane
    }

    /**
     * Waits until sufficient memory is available for an operation.
     * Checks free memory and suggests garbage collection if needed.
     *
     * @param requiredMemory The amount of memory required in bytes.
     * @throws RuntimeException if interrupted while waiting.
     */
    public static void reserveMemory(int requiredMemory) {
        Runtime runtime = Runtime.getRuntime();
        int nextBackoff = 8; // Initial backoff: 8 ms
        final int MAX_BACKOFF = 8192; // Maximum backoff: 8 s

        while (nextBackoff < MAX_BACKOFF) {
            long maxMemory = runtime.maxMemory();
            long allocatedMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long currentReserved = reservedMemory.get();
            long availableMemory = (maxMemory - allocatedMemory) + freeMemory - currentReserved;

            if (availableMemory * MEM_SAFETY_FACTOR > requiredMemory) {
                // Try to atomically reserve the memory
                if (reservedMemory.compareAndSet(currentReserved, currentReserved + requiredMemory)) return;

                // CAS failed, another thread modified reservedMemory, retry
                continue;
            }

            try {
                System.gc();
                Thread.sleep(nextBackoff);
                nextBackoff *= 2; // Exponential backoff
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for memory", e);
            }
        }

        throw new RuntimeException("Unable to allocate sufficient memory after exponential backoff (last attempted: " + nextBackoff + "ms).");
    }

    /**
     * Releases reserved memory after an Argon2 operation completes.
     * Must be called after the operation that reserved memory via waitForSufficientMemory.
     *
     * @param requiredMemory The amount of memory to release in bytes.
     */
    public static void releaseMemory(int requiredMemory) {
        reservedMemory.updateAndGet(current -> Math.max(0, current - requiredMemory));
    }
}
