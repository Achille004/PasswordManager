package sign;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.cert.CertPath;

import dev.sigstore.KeylessSignature;
import dev.sigstore.KeylessSigner;
import dev.sigstore.bundle.BundleFactory;
import dev.sigstore.encryption.certificates.Certificates;

public class Sign {
    public static void main(String... args) {
        Path testArtifact = Paths.get("path/to/my/file.jar");

        KeylessSigner signer = KeylessSigner.builder().sigstorePublicDefaults().build();
        KeylessSignature result = signer.sign(testArtifact);

        // resulting signature information

        // artifact digest
        byte[] digest = result.getDigest();

        // certificate from fulcio
        CertPath certs = result.getCertPath(); // java representation of a certificate path
        byte[] certsBytes = Certificates.toPemBytes(result.getCertPath()); // converted to PEM encoded byte array

        // artifact signature
        byte[] sig = result.getSignature();

        // sigstore bundle format (json string)
        String bundle = BundleFactory.createBundle(result);
    }
}
