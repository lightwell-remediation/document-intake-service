package com.example.intake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class IntakeServerTest {

  @TempDir Path storage;

  private IntakeServer server;
  private final HttpClient client = HttpClient.newHttpClient();

  @BeforeEach
  void startServer() throws Exception {
    server = IntakeServer.start(0, new DocumentStore(storage), new UploadLimits(4096, 1024));
  }

  @AfterEach
  void stopServer() {
    server.stop();
  }

  @Test
  void healthIsUp() throws Exception {
    HttpResponse<String> response = client.send(
        HttpRequest.newBuilder(uri("/health")).GET().build(), HttpResponse.BodyHandlers.ofString());

    assertEquals(200, response.statusCode());
    assertEquals("{\"status\":\"UP\"}", response.body());
  }

  @Test
  void storesAnUploadedDocument() throws Exception {
    MultipartBody body = new MultipartBody()
        .file("document", "contract.txt", "signed".getBytes(StandardCharsets.UTF_8));

    HttpResponse<String> response = post(body);

    assertEquals(201, response.statusCode());
    assertTrue(response.body().contains("\"name\":\"contract.txt\""), response.body());
    try (var files = Files.list(storage)) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void storesEveryDocumentInOneRequestAndIgnoresPlainFields() throws Exception {
    MultipartBody body = new MultipartBody()
        .field("submittedBy", "alice")
        .file("document", "a.txt", "first".getBytes(StandardCharsets.UTF_8))
        .file("document", "b.txt", "second".getBytes(StandardCharsets.UTF_8));

    HttpResponse<String> response = post(body);

    assertEquals(201, response.statusCode());
    HttpResponse<String> listing = client.send(
        HttpRequest.newBuilder(uri("/documents")).GET().build(), HttpResponse.BodyHandlers.ofString());
    assertEquals(200, listing.statusCode());
    assertTrue(listing.body().contains("\"name\":\"a.txt\""), listing.body());
    assertTrue(listing.body().contains("\"name\":\"b.txt\""), listing.body());
  }

  @Test
  void rejectsADocumentAboveTheSizeLimit() throws Exception {
    MultipartBody body = new MultipartBody().file("document", "big.bin", new byte[2048]);

    assertEquals(413, post(body).statusCode());
    try (var files = Files.list(storage)) {
      assertEquals(0, files.count());
    }
  }

  @Test
  void rejectsARequestWithoutDocuments() throws Exception {
    assertEquals(400, post(new MultipartBody().field("note", "nothing attached")).statusCode());
  }

  @Test
  void rejectsNonMultipartBodies() throws Exception {
    HttpRequest request = HttpRequest.newBuilder(uri("/documents"))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString("{}"))
        .build();

    assertEquals(415, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
  }

  @Test
  void rejectsOtherMethods() throws Exception {
    HttpRequest request = HttpRequest.newBuilder(uri("/documents")).DELETE().build();

    assertEquals(405, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
  }

  @Test
  void reportsAStorageFailureAsAServerError() throws Exception {
    Path notADirectory = Files.writeString(storage.resolve("occupied"), "x");
    IntakeServer broken = IntakeServer.start(0, new DocumentStore(notADirectory));
    try {
      MultipartBody body = new MultipartBody()
          .file("document", "a.txt", "first".getBytes(StandardCharsets.UTF_8));
      HttpRequest request = HttpRequest.newBuilder(
              URI.create("http://localhost:" + broken.port() + "/documents"))
          .header("Content-Type", body.contentType())
          .POST(body.publisher())
          .build();

      assertEquals(500, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
    } finally {
      broken.stop();
    }
  }

  private HttpResponse<String> post(MultipartBody body) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(uri("/documents"))
        .header("Content-Type", body.contentType())
        .POST(body.publisher())
        .build();
    return client.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + server.port() + path);
  }
}
