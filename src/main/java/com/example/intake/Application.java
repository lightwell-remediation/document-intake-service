package com.example.intake;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Entry point. Configuration comes from the environment:
 *
 * <ul>
 *   <li>{@code PORT} — port to listen on (default 8080)
 *   <li>{@code STORAGE_DIR} — where received documents are written (default {@code ./data})
 * </ul>
 */
public final class Application {

  private Application() {}

  public static void main(String[] args) throws IOException {
    int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    Path storage = Path.of(System.getenv().getOrDefault("STORAGE_DIR", "data"));

    IntakeServer server = IntakeServer.start(port, new DocumentStore(storage));
    Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
    System.out.printf("document-intake-service listening on port %d, storing in %s%n",
        server.port(), storage.toAbsolutePath());
  }
}
