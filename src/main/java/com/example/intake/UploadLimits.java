package com.example.intake;

/**
 * Size limits applied to every upload.
 *
 * @param maxRequestBytes largest accepted request body
 * @param maxFileBytes largest accepted single document
 */
public record UploadLimits(long maxRequestBytes, long maxFileBytes) {

  /** 50 MiB per request, 10 MiB per document. */
  public static final UploadLimits DEFAULT = new UploadLimits(50L * 1024 * 1024, 10L * 1024 * 1024);
}
