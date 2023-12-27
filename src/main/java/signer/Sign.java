package signer;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertPath;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import dev.sigstore.KeylessSignature;
import dev.sigstore.KeylessSigner;
import dev.sigstore.bundle.BundleFactory;
import dev.sigstore.encryption.certificates.Certificates;

public class Sign {

    static final Path SOURCE_PATH = Path.of("compiled");
    static final Path SAVE_PATH = Path.of(SOURCE_PATH.toString(), "verificationBundles");

    public static void main(String... args) throws Exception {
        for(File file: SAVE_PATH.toFile().listFiles()) {
            file.delete();
            System.out.println("Deleted: " + file.toString());
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
            System.out.println("Signed: " + savePath.toString());

            PrintWriter pw = new PrintWriter(savePath.toFile());
            pw.write(bundle);
            pw.flush();
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } 
    }
}
