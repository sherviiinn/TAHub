package ir.TAHub.TAHub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on @Scheduled methods (jobs that run by themselves at fixed times, like the nightly menu cleanup). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
