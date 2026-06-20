package com.vendra.shop;

import java.util.Locale;

/** Tiny slug helper: lowercase, non-alphanumeric runs collapsed to a single '-'. */
public final class Slugs {
  private Slugs() {}

  public static String slugify(String name) {
    if (name == null) {
      return "";
    }
    String slug =
        name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-+)|(-+$)", "");
    return slug.isBlank() ? "item" : slug;
  }
}
