package at.technikum_wien.rest_server.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * SHA-256 of a document's content, as lowercase hex. It identifies a document independently
 * of its file name, so clients (e.g. the Explorer integration) can find a local file in the DMS.
 */
public final class ContentHash {

    private static final Pattern SHA256_HEX = Pattern.compile("[0-9a-f]{64}");

    private ContentHash() {
    }

    public static String sha256(InputStream content) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = content.read(buffer)) != -1) {
            digest.update(buffer, 0, read);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    /** Normalizes a client-supplied hash; returns null if it is not a valid SHA-256 hex string. */
    public static String normalize(String hash) {
        if (hash == null) {
            return null;
        }
        String lower = hash.trim().toLowerCase();
        return SHA256_HEX.matcher(lower).matches() ? lower : null;
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", e);
        }
    }
}
