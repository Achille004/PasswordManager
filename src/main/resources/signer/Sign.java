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

package signer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.sigstore.KeylessSignature;
import dev.sigstore.KeylessSigner;
import dev.sigstore.bundle.BundleFactory;

public class Sign {

    static final Path SOURCE_PATH = Path.of("compiled");
    static final Path SAVE_PATH = Path.of(SOURCE_PATH.toString(), "verificationBundles");

    public static void main(String... args) throws Exception {
        for(File file: Objects.requireNonNull(SAVE_PATH.toFile().listFiles())) {
            file.delete();
            System.out.println("Deleted: " + file);
        }
        System.out.println();

        List<Path> PATHS_LIST = Files.walk(SOURCE_PATH).filter(Files::isRegularFile).collect(Collectors.toList());

        KeylessSigner SIGNER = KeylessSigner.builder().sigstorePublicDefaults().build();
        Map<Path, KeylessSignature> RESULT = SIGNER.signFiles(PATHS_LIST);
        
        System.out.println();
        RESULT.forEach(Sign::readSignature);
    }

    public static void readSignature(Path path, KeylessSignature keylessSignature) {
        try {
            String bundle = BundleFactory.createBundle(keylessSignature);

            Path savePath = Path.of(SAVE_PATH.toString(), path.getFileName().toString() + ".json");
            System.out.println("Signed: " + savePath);

            PrintWriter pw = new PrintWriter(savePath.toFile());
            pw.write(bundle);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
}
