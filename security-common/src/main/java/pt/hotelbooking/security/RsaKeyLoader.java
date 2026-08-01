package pt.hotelbooking.security;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public final class RsaKeyLoader {
    private RsaKeyLoader() {
    }

    public static RSAPrivateKey privateKey(String base64Key) {
        try {
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(
                    new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64Key)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT private key is not a valid PKCS#8 RSA key.", exception);
        }
    }

    public static RSAPublicKey publicKey(String base64Key) {
        try {
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
                    new X509EncodedKeySpec(Base64.getDecoder().decode(base64Key)));
        } catch (Exception exception) {
            throw new IllegalStateException("JWT public key is not a valid X.509 RSA key.", exception);
        }
    }
}
