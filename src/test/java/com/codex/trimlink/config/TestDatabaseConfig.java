package com.codex.trimlink.config;

import javax.sql.DataSource;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestDatabaseConfig {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
            .driverClassName("org.h2.Driver")
            .url("jdbc:h2:mem:testdb")
            .username("sa")
            .password("")
            .build();
    }
}
