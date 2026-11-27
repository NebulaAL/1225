package io.github.nebulaal.fwdata;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.datasource.url=jdbc:h2:mem:collection;MODE=MySQL;DATABASE_TO_LOWER=TRUE",
    "spring.datasource.username=sa", "spring.datasource.password=", "fwdata.admin.username=admin"
})
class CollectionAccessTest {
    private static final String PASSWORD = UUID.randomUUID().toString();
    private final ObjectMapper json = new ObjectMapper();
    @LocalServerPort int port;

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry properties) {
        properties.add("fwdata.admin.password", () -> PASSWORD);
    }

    private class Session {
        private final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();

        private HttpRequest.Builder request(String path) {
            return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api" + path));
        }

        JsonNode send(HttpRequest.Builder req, int status) throws Exception {
            var response = client.send(req.build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(status, response.statusCode(), () -> req.build().uri() + " " + response.body());
            return response.body().isBlank() ? null : json.readTree(response.body());
        }

        JsonNode get(String path, int status) throws Exception { return send(request(path).GET(), status); }

        JsonNode write(String method, String path, Object value, int status) throws Exception {
            var csrf = get("/auth/csrf", 200);
            var req = request(path).header(csrf.get("headerName").asString(), csrf.get("token").asString());
            var body = HttpRequest.BodyPublishers.noBody();
            if (value instanceof String form) {
                req.header("Content-Type", "application/x-www-form-urlencoded");
                body = HttpRequest.BodyPublishers.ofString(form);
            } else if (value != null) {
                req.header("Content-Type", "application/json");
                body = HttpRequest.BodyPublishers.ofString(json.writeValueAsString(value));
            }
            return send(req.method(method, body), status);
        }

        void login(String username) throws Exception {
            write("POST", "/auth/login", "username=" + username + "&password=" + PASSWORD, 204);
        }
    }

    @Test
    void responsesRemainReadableOnlyToStaffAndDeletedSessionsCannotBeReused() throws Exception {
        var admin = new Session();
        var visitor = new Session();
        var viewer = new Session();
        visitor.get("/submissions", 401);
        admin.login("admin");
        var account = Map.of("username", "viewer", "displayName", "查阅员", "role", "USER", "password", PASSWORD);
        admin.write("POST", "/accounts", account, 201);
        viewer.login("viewer");
        long viewerId = viewer.get("/auth/me", 200).get("id").asLong();
        var field = Map.of("label", "参加环节", "kind", "MULTIPLE", "required", true, "options", List.of("讨论", "演示"));
        var form = Map.of("name", "活动反馈", "fields", List.of(field));
        viewer.write("POST", "/manage/datasets", form, 403);
        viewer.get("/accounts", 403);
        long datasetId = admin.write("POST", "/manage/datasets", form, 201).get("id").asLong();
        var source = admin.write("POST", "/manage/datasets/" + datasetId + "/sources", Map.of("name", "展会"), 201);
        String path = "/public/forms/" + source.get("publicKey").asString();
        var question = visitor.get(path, 200).get("fields").get(0);
        var optionIds = List.of(question.get("options").get(0).get("id").asLong(), question.get("options").get(1).get("id").asLong());
        String key = UUID.randomUUID().toString().replace("-", "");
        visitor.write("POST", path + "/submissions", Map.of("requestKey", key, "answers", List.of()), 400);
        var invalidChoice = Map.of("fieldId", question.get("id").asLong(), "optionIds", List.of(Long.MAX_VALUE));
        visitor.write("POST", path + "/submissions", Map.of("requestKey", key, "answers", List.of(invalidChoice)), 400);
        var answer = Map.of("fieldId", question.get("id").asLong(), "optionIds", optionIds);
        var payload = Map.of("requestKey", key, "answers", List.of(answer));
        long id = visitor.write("POST", path + "/submissions", payload, 201).get("id").asLong();
        assertEquals(id, visitor.write("POST", path + "/submissions", payload, 201).get("id").asLong());
        visitor.get("/submissions/" + id, 401);
        String filter = "/submissions?datasetId=" + datasetId + "&sourceId=" + source.get("id").asLong();
        assertEquals(1, viewer.get(filter, 200).get("total").asLong());
        var saved = viewer.get("/submissions/" + id, 200);
        assertEquals("展会", saved.get("submission").get("sourceName").asString());
        assertEquals(List.of("讨论", "演示"), saved.get("answers").get(0).get("selected").valueStream().map(JsonNode::asString).toList());
        viewer.write("DELETE", "/accounts/" + viewerId, null, 403);
        admin.write("DELETE", "/accounts/" + admin.get("/auth/me", 200).get("id").asLong(), null, 409);
        admin.write("DELETE", "/accounts/" + viewerId, null, 204);
        admin.write("POST", "/accounts", account, 201);
        viewer.get(path, 200);
        viewer.get("/submissions", 401);
        assertEquals(id, admin.get("/submissions/" + id, 200).get("submission").get("id").asLong());
    }
}
