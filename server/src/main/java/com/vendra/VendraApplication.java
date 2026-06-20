package com.vendra;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Vendra marketplace API — Spring Boot resource server over the Supabase-owned schema. */
@SpringBootApplication
public class VendraApplication {
  public static void main(String[] args) {
    SpringApplication.run(VendraApplication.class, args);
  }
}
