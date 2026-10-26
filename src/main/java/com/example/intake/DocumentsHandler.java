package com.example.intake;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUpload;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;

/**
 * {@code POST /documents} receives one or more documents as
 * {@code multipart/form-data}; {@code GET /documents} lists what is stored.
 */
final class DocumentsHandler implements HttpHandler {

  /** Parts up to this size stay in memory; larger ones are buffered to a temp file. */
  private static final int IN_MEMORY_THRESHOLD = 64 * 1024;

  private final DocumentStore store;
  private final FileUpload upload;

  DocumentsHandler(DocumentStore store, UploadLimits limits) {
    this.store = store;
    DiskFileItemFactory factory =
        new DiskFileItemFactory(IN_MEMORY_THRESHOLD, new File(System.getProperty("java.io.tmpdir")));
    this.upload = new FileUpload(factory);
    this.upload.setSizeMax(limits.maxRequestBytes());
    this.upload.setFileSizeMax(limits.maxFileBytes());
  }

  @Override
  public void handle(HttpExchange exchange) throws IOException {
    try {
      switch (exchange.getRequestMethod()) {
        case "GET" -> respond(exchange, 200, Json.documents(store.list()));
        case "POST" -> receive(exchange);
        default -> {
          exchange.getResponseHeaders().set("Allow", "GET, POST");
          respond(exchange, 405, Json.error("method not allowed"));
        }
      }
    } catch (IOException e) {
      System.err.printf("%s %s failed: %s%n",
          exchange.getRequestMethod(), exchange.getRequestURI(), e);
      if (exchange.getResponseCode() == -1) {
        respond(exchange, 500, Json.error("internal error"));
      }
    } finally {
      exchange.close();
    }
  }

  private void receive(HttpExchange exchange) throws IOException {
    ExchangeRequestContext request = new ExchangeRequestContext(exchange);
    if (!FileUploadBase.isMultipartContent(request)) {
      respond(exchange, 415, Json.error("expected multipart/form-data"));
      return;
    }

    List<FileItem> items;
    try {
      items = upload.parseRequest(request);
    } catch (FileUploadBase.SizeLimitExceededException
        | FileUploadBase.FileSizeLimitExceededException e) {
      respond(exchange, 413, Json.error("upload too large"));
      return;
    } catch (FileUploadException e) {
      respond(exchange, 400, Json.error("malformed upload"));
      return;
    }

    List<StoredDocument> stored = new ArrayList<>();
    try {
      for (FileItem item : items) {
        if (item.isFormField()) {
          continue;
        }
        try (InputStream content = item.getInputStream()) {
          stored.add(store.save(item.getName(), content));
        }
      }
    } finally {
      items.forEach(FileItem::delete);
    }

    if (stored.isEmpty()) {
      respond(exchange, 400, Json.error("no documents in request"));
      return;
    }
    respond(exchange, 201, Json.documents(stored));
  }

  static void respond(HttpExchange exchange, int status, String json) throws IOException {
    byte[] body = json.getBytes(StandardCharsets.UTF_8);
    exchange.getResponseHeaders().set("Content-Type", "application/json");
    exchange.sendResponseHeaders(status, body.length);
    try (OutputStream out = exchange.getResponseBody()) {
      out.write(body);
    }
  }
}
