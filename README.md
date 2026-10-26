# document-intake-service

A small HTTP service that receives documents as `multipart/form-data` uploads
and stores them on disk. Plain Java 21: the JDK's built-in HTTP server plus
Apache Commons FileUpload for parsing.

> Sample service for demonstrations. Not intended for production use.

## Prerequisites

- JDK 21
- Maven 3.9

## Build and test

```bash
mvn -B test        # unit and HTTP tests
mvn -B package     # self-contained jar in target/
```

## Run

```bash
STORAGE_DIR=/tmp/documents PORT=8080 java -jar target/document-intake-service-1.0.0.jar
```

| Variable | Default | Meaning |
|---|---|---|
| `PORT` | `8080` | Port to listen on |
| `STORAGE_DIR` | `./data` | Where received documents are written |

## API

| Method | Path | Description |
|---|---|---|
| `POST` | `/documents` | Upload one or more documents (`multipart/form-data`). Returns `201` with what was stored |
| `GET` | `/documents` | List stored documents |
| `GET` | `/health` | Liveness check |

```bash
curl -F "document=@invoice.pdf" http://localhost:8080/documents
curl http://localhost:8080/documents
```

Requests above 50 MiB or documents above 10 MiB are rejected with `413`.

## Software bill of materials

`sbom.json` is a CycloneDX inventory of the built jar.

## License

[Apache License 2.0](LICENSE)
