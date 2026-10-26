# AGENTS.md — document-intake-service

Guidance for AI coding agents and humans working on this repository.

## Overview

Small HTTP service that receives documents as `multipart/form-data` uploads and
stores them on disk. Plain Java 21: the JDK's built-in HTTP server
(`com.sun.net.httpserver`) plus Apache Commons FileUpload for parsing. No
application framework, on purpose: one runtime dependency keeps the service and
its inventory small.

## Build and test

```bash
mvn -B test                  # all tests, must stay green
mvn -B -DskipTests package   # self-contained jar: target/document-intake-service-1.0.0.jar
```

Requires JDK 21 and Maven 3.9. Tests are deterministic and need no network or
external services: HTTP tests start the server on port 0 and use JUnit's
`@TempDir` for storage.

## Layout

| Path | What |
|---|---|
| `src/main/java/com/example/intake/Application.java` | Entry point; reads `PORT` and `STORAGE_DIR` |
| `.../IntakeServer.java` | Creates the HTTP server and its contexts |
| `.../DocumentsHandler.java` | `GET`/`POST /documents`; maps upload errors to HTTP statuses |
| `.../ExchangeRequestContext.java` | Adapts a JDK `HttpExchange` to FileUpload's `UploadContext` |
| `.../UploadLimits.java` | Size limits applied to every upload |
| `.../DocumentStore.java`, `StoredDocument.java` | Writing and listing documents on disk |
| `.../Json.java` | Minimal JSON output, no library |
| `src/test/java/.../MultipartBody.java` | Test helper that builds multipart request bodies |
| `sbom.json` | CycloneDX inventory of the built jar |

## Conventions

- Single-module Maven project; keep `pom.xml` at the root and `jar` packaging.
  The shade plugin replaces the plain jar with a runnable one under the same
  name; do not add a classifier or `finalName`.
- `javax.servlet-api` is `provided` only because FileUpload's API references
  servlet types; nothing in this service runs in a servlet container.
- HTTP status mapping: `413` size limits, `415` non-multipart, `400` malformed
  or empty upload, `500` storage failures (logged to stderr).
- Client-supplied file names go through `DocumentStore.sanitize`; never write a
  path derived from the request without it.
- New behaviour comes with a test in `src/test/java`; JUnit 5 (Jupiter API).
- Conventional Commits (`feat:`, `fix:`, `test:`, `docs:`, `chore:`), English.
- Regenerate `sbom.json` when runtime dependencies change.
