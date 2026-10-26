package com.example.intake;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** The HTTP server: {@code /documents} and {@code /health}. */
public final class IntakeServer {

  private final HttpServer server;
  private final ExecutorService executor;

  private IntakeServer(HttpServer server, ExecutorService executor) {
    this.server = server;
    this.executor = executor;
  }

  /** Starts on {@code port} (0 picks a free one) with the default limits. */
  public static IntakeServer start(int port, DocumentStore store) throws IOException {
    return start(port, store, UploadLimits.DEFAULT);
  }

  public static IntakeServer start(int port, DocumentStore store, UploadLimits limits)
      throws IOException {
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
    ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    server.setExecutor(executor);
    server.createContext("/documents", new DocumentsHandler(store, limits));
    server.createContext("/health", exchange -> {
      try (exchange) {
        DocumentsHandler.respond(exchange, 200, "{\"status\":\"UP\"}");
      }
    });
    server.start();
    return new IntakeServer(server, executor);
  }

  public int port() {
    return server.getAddress().getPort();
  }

  public void stop() {
    server.stop(0);
    executor.shutdown();
  }
}
