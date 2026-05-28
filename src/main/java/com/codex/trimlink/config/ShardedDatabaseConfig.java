package com.codex.trimlink.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.codex.trimlink.node.storage.ShardedRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class ShardedDatabaseConfig {

    private HikariDataSource buildHikariDataPool(String containerHost, String port) {
        HikariDataSource pool = new HikariDataSource();

        pool.setJdbcUrl("jdbc:postgresql://" + containerHost + ":" + port + "/trimlink_db");
        pool.setUsername("postgres_user");
        pool.setPassword("secure_db_password");
        pool.setDriverClassName("org.postgresql.Driver");

        pool.setMaximumPoolSize(20);
        pool.setMinimumIdle(2);
        pool.setIdleTimeout(30000);

        // Robust connection retry loop to handle startup timing race conditions
        int maxRetries = 15;
        int delayMs = 2000;
        boolean initialized = false;
        
        for (int i = 0; i < maxRetries; i++) {
            try (java.sql.Connection conn = pool.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS url_mappings (" +
                             "id BIGINT PRIMARY KEY, " +
                             "short_code VARCHAR(10) UNIQUE NOT NULL, " +
                             "long_url VARCHAR(2048) NOT NULL, " +
                             "created_at TIMESTAMP NOT NULL" +
                             ")");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_short_code ON url_mappings (short_code)");
                System.out.println("[DATABASE AUTO-INIT] Successfully initialized schema on shard: " + containerHost);
                initialized = true;
                break;
            } catch (Exception e) {
                System.out.printf("[DATABASE AUTO-INIT] Shard [%s] not ready (retry %d/%d): %s%n", 
                                  containerHost, (i + 1), maxRetries, e.getMessage());
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (!initialized) {
            System.err.printf("[DATABASE AUTO-INIT] CRITICAL: Failed to initialize schema on shard [%s] after %d retries.%n", 
                              containerHost, maxRetries);
        }

        return pool;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        Map<Object, Object> configuredPoolMap = new HashMap<>();

        HikariDataSource shard0 = buildHikariDataPool("postgres-shard-0", "5432");
        HikariDataSource shard1 = buildHikariDataPool("postgres-shard-1", "5432");
        HikariDataSource shard2 = buildHikariDataPool("postgres-shard-2", "5432");

        configuredPoolMap.put("postgres-shard-0", shard0);
        configuredPoolMap.put("postgres-shard-1", shard1);
        configuredPoolMap.put("postgres-shard-2", shard2);

        ShardedRoutingDataSource shardedRouter = new ShardedRoutingDataSource();
        shardedRouter.setTargetDataSources(configuredPoolMap);
        shardedRouter.setDefaultTargetDataSource(shard0);

        return shardedRouter;
    }
}
