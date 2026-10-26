package com.example.intake;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/** Stores received documents in a directory, one file per document. */
public final class DocumentStore {

  private final Path root;

  public DocumentStore(Path root) {
    this.root = root;
  }

  /** Writes the content under a generated id and returns what was stored. */
  public StoredDocument save(String originalName, InputStream content) throws IOException {
    Files.createDirectories(root);
    String name = sanitize(originalName);
    String id = UUID.randomUUID().toString();
    Path target = root.resolve(id + "_" + name);
    long size = Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    return new StoredDocument(id, name, size);
  }

  /** Lists stored documents, ordered by name. */
  public List<StoredDocument> list() throws IOException {
    if (!Files.isDirectory(root)) {
      return List.of();
    }
    try (Stream<Path> files = Files.list(root)) {
      return files
          .filter(Files::isRegularFile)
          .map(DocumentStore::describe)
          .sorted(Comparator.comparing(StoredDocument::name))
          .toList();
    }
  }

  /**
   * Keeps only the last path segment and a conservative character set, so a
   * client-supplied name can never point outside the storage directory.
   */
  static String sanitize(String originalName) {
    if (originalName == null) {
      return "document";
    }
    String base = originalName.replace('\\', '/');
    base = base.substring(base.lastIndexOf('/') + 1);
    base = base.replaceAll("[^A-Za-z0-9._-]", "_");
    if (base.isBlank() || base.chars().allMatch(c -> c == '.')) {
      return "document";
    }
    return base;
  }

  private static StoredDocument describe(Path file) {
    String fileName = file.getFileName().toString();
    int separator = fileName.indexOf('_');
    String id = separator > 0 ? fileName.substring(0, separator) : fileName;
    String name = separator > 0 ? fileName.substring(separator + 1) : fileName;
    try {
      return new StoredDocument(id, name, Files.size(file));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
