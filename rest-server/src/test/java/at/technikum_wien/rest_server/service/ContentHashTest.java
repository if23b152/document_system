package at.technikum_wien.rest_server.service;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ContentHashTest {

    @Test
    void sha256_returnsLowercaseHexOfContent() throws Exception {
        var content = new ByteArrayInputStream("abc".getBytes(StandardCharsets.US_ASCII));

        // Known SHA-256 test vector for "abc"
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ContentHash.sha256(content));
    }

    @Test
    void normalize_acceptsUppercaseAndWhitespace() {
        String upper = "  BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD ";

        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                ContentHash.normalize(upper));
    }

    @Test
    void normalize_rejectsInvalidValues() {
        assertNull(ContentHash.normalize(null));
        assertNull(ContentHash.normalize("abc"));
        assertNull(ContentHash.normalize("zz7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"));
        assertNull(ContentHash.normalize("../../etc/passwd"));
    }
}
