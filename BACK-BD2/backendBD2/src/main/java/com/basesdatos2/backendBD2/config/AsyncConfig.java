package com.basesdatos2.backendBD2.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

// Habilita @Async: los logs se escriben en MongoDB en segundo plano para que
// la respuesta al usuario no espere a Mongo (ni falle si Mongo esta caido).
@Configuration
@EnableAsync
public class AsyncConfig {
}
