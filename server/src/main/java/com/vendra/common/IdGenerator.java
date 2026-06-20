package com.vendra.common;

import java.util.UUID;

/**
 * Generates readable, prefixed string ids matching the DB convention (e.g. {@code prd_9f2c…}). The
 * shape mirrors the Postgres {@code vendra_id()} helper so ids are consistent regardless of writer.
 */
public final class IdGenerator {

  private IdGenerator() {}

  public static String of(String prefix) {
    return prefix + "_" + UUID.randomUUID().toString().replace("-", "");
  }
}
