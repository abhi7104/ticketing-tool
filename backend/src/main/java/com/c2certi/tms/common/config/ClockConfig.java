package com.c2certi.tms.common.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

  @Bean
  Clock clock() {
    // PostgreSQL stores microseconds; ticking at that precision keeps API values stable.
    return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
  }
}
