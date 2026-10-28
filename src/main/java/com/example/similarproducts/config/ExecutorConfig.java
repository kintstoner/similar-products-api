package com.example.similarproducts.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfig {

    /**
     * One virtual thread per product detail lookup: blocking on a slow downstream costs no OS thread.
     */
    @Bean(destroyMethod = "close")
    ExecutorService productLookupExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
