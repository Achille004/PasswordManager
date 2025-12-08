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

package testing;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import password.manager.app.singletons.Logger;

public class TestingUtils {

    // Define a common log path for all tests, starting from the project root
    // (Path is hardcoded, but I guess it's fine for testing purposes)
    public static final Path LOG_PATH = Path.of("build/test-logs/");

    static {
        try {
            Files.createDirectories(LOG_PATH);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create test log directory", e);
        }
    }

    public static void initLogger() {
        Optional<StackWalker.StackFrame> method = StackWalker.getInstance()
                .walk(frames -> frames.skip(1).findFirst());

        String className = "UnknownClass", methodName = "UnknownMethod";

        if (method.isPresent()) {
            StackWalker.StackFrame frame = method.get();
            className = frame.getClassName();
            methodName = frame.getMethodName();
        }

        Logger.createInstance(LOG_PATH.resolve(className).resolve(methodName));
    }

    public static String readBlnsLine(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null && (line.startsWith("#") || line.trim().isEmpty())) ;
        return line;
    }
}
