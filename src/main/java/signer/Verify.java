package signer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import dev.sigstore.KeylessSignature;
import dev.sigstore.KeylessVerificationException;
import dev.sigstore.KeylessVerificationRequest;
import dev.sigstore.KeylessVerificationRequest.CertificateIdentity;
import dev.sigstore.KeylessVerificationRequest.VerificationOptions;
import dev.sigstore.KeylessVerifier;
import dev.sigstore.bundle.BundleFactory;

public class Verify {

    static final Path SOURCE_PATH = Path.of("compiled", "verificationBundles");

    static final VerificationOptions verificationOptions = VerificationOptions.builder()
            // verify online? (connect to rekor for inclusion proof)
            .alwaysUseRemoteRekorEntry(false)
            // optionally add certificate policy
            .addCertificateIdentities(
                CertificateIdentity.builder()
                    .issuer("https://github.com/login/oauth")
                    .subjectAlternativeName("2004marras@gmail.com")
                    .build()
                )
            .build();

    public static void main(String... args) throws Exception {
        List<Path> PATHS_LIST = Files.walk(SOURCE_PATH).filter(Files::isRegularFile).collect(Collectors.toList());
        PATHS_LIST.forEach(Verify::verifySignature);
    }

    public static void verifySignature(Path bundleFile) {
        try {
            KeylessSignature keylessSignature = BundleFactory.readBundle(Files.newBufferedReader(bundleFile, StandardCharsets.UTF_8));

            Path artifact = Path.of(bundleFile.toString().replace("verificationBundles" + File.separator, "").replace(".json", ""));

            KeylessVerifier verifier = new KeylessVerifier.Builder().sigstorePublicDefaults().build();
            verifier.verify(
                artifact,
                KeylessVerificationRequest.builder()
                    .keylessSignature(keylessSignature)
                    .verificationOptions(verificationOptions)
                    .build());

            System.out.println("Passed verification of: " + artifact.toString());
        } catch (Exception  e) {
            System.out.println("Failed verification of: " + e.toString());
        }
    }
}