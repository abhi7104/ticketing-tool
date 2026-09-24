package com.c2certi.tms.common.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Trims every incoming JSON string so whitespace-only input counts as empty. Unknown properties are
 * rejected via {@code spring.jackson.deserialization.fail-on-unknown-properties}.
 */
@Configuration
public class JacksonConfig {

  @Bean
  Module trimmingStringModule() {
    SimpleModule module = new SimpleModule("trimming-strings");
    module.addDeserializer(
        String.class,
        new StringDeserializer() {
          @Override
          public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String value = super.deserialize(p, ctxt);
            return value == null ? null : value.strip();
          }
        });
    return module;
  }
}
