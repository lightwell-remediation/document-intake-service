package com.example.intake;

/** A document as kept by the store. */
public record StoredDocument(String id, String name, long size) {

  String toJson() {
    return "{\"id\":\"" + Json.escape(id) + "\",\"name\":\"" + Json.escape(name)
        + "\",\"size\":" + size + "}";
  }
}
