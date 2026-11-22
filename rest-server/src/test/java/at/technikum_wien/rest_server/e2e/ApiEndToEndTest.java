package at.technikum_wien.rest_server.e2e;

import at.technikum_wien.rest_server.service.ContentHash;
import at.technikum_wien.rest_server.service.ContentHashBackfill;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayInputStream;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Real HTTP requests through the real security chain and database. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("e2e")
@Import(InMemoryInfrastructure.class)
class ApiEndToEndTest {

    private static final String TEXT = "text/plain; charset=utf-8";

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ContentHashBackfill backfill;

    @Test
    void tokenLogsInWithoutCreatingASession() throws Exception {
        User alice = register();
        String secret = alice.createToken();

        HttpResponse<String> me = withToken(secret).get("/api/auth/me");

        assertEquals(200, me.statusCode());
        assertEquals(alice.name(), read(me).get("username").asText());
        assertTrue(me.headers().firstValue("Set-Cookie").isEmpty(), "token requests must not create a session");
    }

    @Test
    void onlyTheHashOfATokenIsStoredAndItsUseIsRecorded() throws Exception {
        String secret = register().createToken();
        withToken(secret).get("/api/documents");

        String prefix = secret.substring(0, 10);
        assertEquals(sha256(secret),
                jdbc.queryForObject("select secret_hash from api_tokens where prefix = ?", String.class, prefix));
        assertNotNull(jdbc.queryForObject("select last_used_at from api_tokens where prefix = ?", Object.class, prefix));
    }

    @Test
    void unknownExpiredAndRevokedTokensAreRejected() throws Exception {
        assertEquals(401, anonymous().get("/api/documents").statusCode());
        assertEquals(401, withToken("dms_unknown").get("/api/documents").statusCode());
        assertEquals(401, withToken("dms_unknown").get("/api/auth/me").statusCode());

        User alice = register();
        String expired = alice.createToken();
        jdbc.update("update api_tokens set expires_at = ? where prefix = ?",
                LocalDateTime.now().minusMinutes(1), expired.substring(0, 10));
        assertEquals(401, withToken(expired).get("/api/documents").statusCode());

        String revoked = alice.createToken();
        long id = 0;
        for (JsonNode token : read(alice.http().get("/api/tokens"))) {
            assertFalse(token.has("secret"), "the list must never contain secrets");
            if (revoked.startsWith(token.get("prefix").asText())) {
                id = token.get("id").asLong();
            }
        }
        assertEquals(200, withToken(revoked).get("/api/documents").statusCode());
        assertEquals(204, alice.http().send("DELETE", "/api/tokens/" + id, null, null).statusCode());
        assertEquals(401, withToken(revoked).get("/api/documents").statusCode());
    }

    @Test
    void cookiesFromTokenRequestsDoNotOutliveTheToken() throws Exception {
        User alice = register();
        String secret = alice.createToken();
        HttpClient withCookies = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        assertEquals(200, new Http(withCookies, port, secret).get("/api/documents").statusCode());

        jdbc.update("delete from api_tokens where prefix = ?", secret.substring(0, 10));

        assertEquals(401, new Http(withCookies, port, null).get("/api/documents").statusCode());
    }

    @Test
    void sessionLoginStillWorksAndChangesTheSessionId() throws Exception {
        User alice = register();
        CookieManager cookies = new CookieManager();
        Http browser = new Http(HttpClient.newBuilder().cookieHandler(cookies).build(), port, null);
        String login = "{\"username\":\"%s\",\"password\":\"Test-%s-pw1\"}".formatted(alice.name(), alice.name());

        assertEquals(200, browser.send("POST", "/api/auth/login", "application/json", login).statusCode());
        String first = cookies.getCookieStore().getCookies().get(0).getValue();
        assertEquals(200, browser.get("/api/documents").statusCode());
        assertEquals(200, browser.send("POST", "/api/auth/login", "application/json", login).statusCode());
        assertNotEquals(first, cookies.getCookieStore().getCookies().get(0).getValue(), "session fixation protection");

        assertEquals(204, browser.send("POST", "/api/auth/logout", null, null).statusCode());
        assertEquals(401, browser.get("/api/documents").statusCode());
    }

    @Test
    void streamedPdfCanBeDownloadedWithAToken() throws Exception {
        Http alice = withToken(register().createToken());
        String pdf = uniquePdf();
        long id = read(alice.upload(pdf)).get("id").asLong();

        HttpResponse<String> file = alice.get("/api/documents/" + id + "/file");

        assertEquals(200, file.statusCode());
        assertEquals(pdf, file.body());
    }

    @Test
    void aTokenCannotManageTokensAndGuestsCannotCreateThem() throws Exception {
        Http token = withToken(register().createToken());
        assertEquals(403, token.get("/api/tokens").statusCode());
        assertEquals(403, token.send("POST", "/api/tokens", "application/json", "{\"name\":\"more\"}").statusCode());

        Http guest = anonymous();
        assertEquals(200, guest.send("POST", "/api/auth/try", null, null).statusCode());
        assertEquals(403, guest.send("POST", "/api/tokens", "application/json", "{\"name\":\"guest\"}").statusCode());
    }

    @Test
    void uploadedDocumentIsFoundByHashOnlyByItsOwner() throws Exception {
        Http alice = withToken(register().createToken());
        Http bob = withToken(register().createToken());
        String pdf = uniquePdf();
        String hash = sha256(pdf);

        assertEquals(404, alice.get("/api/documents/by-hash/" + hash).statusCode());
        HttpResponse<String> upload = alice.upload(pdf);
        assertEquals(201, upload.statusCode(), upload.body());
        assertEquals(hash, read(upload).get("contentSha256").asText());

        HttpResponse<String> found = alice.get("/api/documents/by-hash/" + hash.toUpperCase());
        assertEquals(200, found.statusCode());
        assertEquals(read(upload).get("id"), read(found).get("id"));
        assertEquals(404, bob.get("/api/documents/by-hash/" + hash).statusCode());
        assertEquals(400, alice.get("/api/documents/by-hash/nonsense").statusCode());
    }

    @Test
    void documentsWithoutHashAreFilledInOnStartup() throws Exception {
        Http alice = withToken(register().createToken());
        String pdf = uniquePdf();
        long id = read(alice.upload(pdf)).get("id").asLong();
        jdbc.update("update documents set content_sha256 = null where id = ?", id);
        assertEquals(404, alice.get("/api/documents/by-hash/" + sha256(pdf)).statusCode());

        backfill.run(null);

        assertEquals(200, alice.get("/api/documents/by-hash/" + sha256(pdf)).statusCode());
    }

    @Test
    void commentsCanBeEditedOnlyByTheDocumentOwner() throws Exception {
        Http alice = withToken(register().createToken());
        Http bob = withToken(register().createToken());
        long document = read(alice.upload(uniquePdf())).get("id").asLong();
        JsonNode comment = read(alice.send("POST", "/api/comments/document/" + document, TEXT, "Größe prüfen"));
        assertEquals("Größe prüfen", comment.get("content").asText());
        assertTrue(comment.get("updatedAt").isNull());
        String path = "/api/comments/" + comment.get("id").asLong();

        assertEquals(404, bob.send("PUT", path, TEXT, "hacked").statusCode());
        HttpResponse<String> edited = alice.send("PUT", path, TEXT, "geändert");
        assertEquals(200, edited.statusCode(), edited.body());

        JsonNode list = read(alice.get("/api/comments/document/" + document));
        assertEquals("geändert", list.get(0).get("content").asText());
        assertFalse(list.get(0).get("updatedAt").isNull());
        assertEquals(400, alice.send("PUT", path, TEXT, " ").statusCode());
    }

    // ---------------------------------------------------------------- helpers

    private record User(String name, Http http, ObjectMapper json) {

        String createToken() throws Exception {
            HttpResponse<String> response = http.send("POST", "/api/tokens", "application/json", "{\"name\":\"e2e\"}");
            assertEquals(201, response.statusCode(), response.body());
            return json.readTree(response.body()).get("secret").asText();
        }
    }

    private User register() throws Exception {
        String name = "u" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        Http http = anonymous();
        HttpResponse<String> response = http.send("POST", "/api/auth/register", "application/json",
                "{\"username\":\"%s\",\"email\":\"%s@example.org\",\"password\":\"Test-%s-pw1\"}".formatted(name, name, name));
        assertEquals(201, response.statusCode(), response.body());
        return new User(name, http, json);
    }

    private Http anonymous() {
        return new Http(HttpClient.newBuilder().cookieHandler(new CookieManager()).build(), port, null);
    }

    private Http withToken(String secret) {
        return new Http(HttpClient.newHttpClient(), port, secret);
    }

    private JsonNode read(HttpResponse<String> response) throws Exception {
        return json.readTree(response.body());
    }

    private static String uniquePdf() {
        return "%PDF-1.4 " + UUID.randomUUID();
    }

    private static String sha256(String text) throws Exception {
        return ContentHash.sha256(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
    }

    private record Http(HttpClient client, int port, String token) {

        HttpResponse<String> get(String path) throws Exception {
            return send("GET", path, null, null);
        }

        HttpResponse<String> upload(String pdf) throws Exception {
            String body = "--b\r\nContent-Disposition: form-data; name=\"file\"; filename=\"e2e.pdf\"\r\n"
                    + "Content-Type: application/pdf\r\n\r\n" + pdf + "\r\n--b--\r\n";
            return send("POST", "/api/documents/upload", "multipart/form-data; boundary=b", body);
        }

        HttpResponse<String> send(String method, String path, String type, String body) throws Exception {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                    .method(method, body == null
                            ? HttpRequest.BodyPublishers.noBody()
                            : HttpRequest.BodyPublishers.ofString(body));
            if (type != null) {
                request.header("Content-Type", type);
            }
            if (token != null) {
                request.header("Authorization", "Bearer " + token);
            }
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
        }
    }
}
