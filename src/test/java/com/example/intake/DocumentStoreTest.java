package com.example.intake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DocumentStoreTest {

  @TempDir Path storage;

  @Test
  void savesAndListsDocuments() throws Exception {
    DocumentStore store = new DocumentStore(storage);

    StoredDocument saved = store.save("invoice.pdf", stream("pdf-bytes"));

    assertEquals("invoice.pdf", saved.name());
    assertEquals(9, saved.size());
    List<StoredDocument> listed = store.list();
    assertEquals(List.of(saved), listed);
  }

  @Test
  void listIsEmptyBeforeAnythingIsStored() throws Exception {
    assertTrue(new DocumentStore(storage.resolve("missing")).list().isEmpty());
  }

  @Test
  void clientPathsNeverLeaveTheStorageDirectory() throws Exception {
    DocumentStore store = new DocumentStore(storage);

    StoredDocument saved = store.save("../../etc/passwd", stream("x"));

    assertEquals("passwd", saved.name());
    try (var files = Files.list(storage)) {
      assertEquals(1, files.count());
    }
  }

  @Test
  void sanitizesNames() {
    assertEquals("report.txt", DocumentStore.sanitize("C:\\Users\\me\\report.txt"));
    assertEquals("my_file__1_.txt", DocumentStore.sanitize("my file (1).txt"));
    assertEquals("document", DocumentStore.sanitize(".."));
    assertEquals("document", DocumentStore.sanitize(""));
    assertEquals("document", DocumentStore.sanitize(null));
  }

  private static ByteArrayInputStream stream(String content) {
    return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
  }
}
