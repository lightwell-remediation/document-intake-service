package com.example.intake;

import com.sun.net.httpserver.HttpExchange;
import java.io.InputStream;
import org.apache.commons.fileupload.UploadContext;

/** Presents a JDK {@link HttpExchange} to Commons FileUpload. */
final class ExchangeRequestContext implements UploadContext {

  private final HttpExchange exchange;

  ExchangeRequestContext(HttpExchange exchange) {
    this.exchange = exchange;
  }

  @Override
  public String getCharacterEncoding() {
    String contentType = getContentType();
    if (contentType == null) {
      return null;
    }
    for (String parameter : contentType.split(";")) {
      String trimmed = parameter.trim();
      if (trimmed.regionMatches(true, 0, "charset=", 0, 8)) {
        return trimmed.substring(8).replace("\"", "");
      }
    }
    return null;
  }

  @Override
  public String getContentType() {
    return exchange.getRequestHeaders().getFirst("Content-Type");
  }

  @Override
  @Deprecated
  public int getContentLength() {
    long length = contentLength();
    return length > Integer.MAX_VALUE ? -1 : (int) length;
  }

  @Override
  public long contentLength() {
    String header = exchange.getRequestHeaders().getFirst("Content-Length");
    if (header == null) {
      return -1;
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return -1;
    }
  }

  @Override
  public InputStream getInputStream() {
    return exchange.getRequestBody();
  }
}
