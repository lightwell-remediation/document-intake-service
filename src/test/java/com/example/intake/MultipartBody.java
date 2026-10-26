package com.example.intake;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Builds {@code multipart/form-data} request bodies for tests. */
final class MultipartBody {

  private final String boundary = "----test-" + UUID.randomUUID();
  private final ByteArrayOutputStream body = new ByteArrayOutputStream();

  MultipartBody file(String field, String fileName, byte[] content) {
    write("--" + boundary + "\r\n");
    write("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + fileName + "\"\r\n");
    write("Content-Type: application/octet-stream\r\n\r\n");
    body.writeBytes(content);
    write("\r\n");
    return this;
  }

  MultipartBody field(String name, String value) {
    write("--" + boundary + "\r\n");
    write("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n");
    write(value + "\r\n");
    return this;
  }

  String contentType() {
    return "multipart/form-data; boundary=" + boundary;
  }

  HttpRequest.BodyPublisher publisher() {
    ByteArrayOutputStream complete = new ByteArrayOutputStream();
    complete.writeBytes(body.toByteArray());
    complete.writeBytes(("--" + boundary + "--\r\n").getBytes(StandardCharsets.US_ASCII));
    return HttpRequest.BodyPublishers.ofByteArray(complete.toByteArray());
  }

  private void write(String text) {
    body.writeBytes(text.getBytes(StandardCharsets.UTF_8));
  }
}
