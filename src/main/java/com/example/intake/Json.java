package com.example.intake;

import java.util.List;
import java.util.stream.Collectors;

/** The little JSON this service emits, without pulling in a library for it. */
final class Json {

  private Json() {}

  static String escape(String value) {
    StringBuilder out = new StringBuilder(value.length());
    for (char c : value.toCharArray()) {
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format("\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    return out.toString();
  }

  static String documents(List<StoredDocument> documents) {
    return documents.stream()
        .map(StoredDocument::toJson)
        .collect(Collectors.joining(",", "{\"documents\":[", "]}"));
  }

  static String error(String message) {
    return "{\"error\":\"" + escape(message) + "\"}";
  }
}
