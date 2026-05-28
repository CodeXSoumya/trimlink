package com.codex.trimlink.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.codex.trimlink.node.storage.ShardedRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;

@Configuration
public class ShardedDatabaseConfig {

    @Value("${TRIMLINK_DB_SHARD_0_URL:jdbc:postgresql://postgres-shard-0:5432/trimlink_db}")
    private String shard0JdbcUrl;

    @Value("${TRIMLINK_DB_SHARD_1_URL:jdbc:postgresql://postgres-shard-1:5432/trimlink_db}")
    private String shard1JdbcUrl;

    @Value("${TRIMLINK_DB_SHARD_2_URL:jdbc:postgresql://postgres-shard-2:5432/trimlink_db}")
    private String shard2JdbcUrl;

    @Value("${TRIMLINK_DB_USERNAME:postgres_user}")
    private String dbUsername;

    @Value("${TRIMLINK_DB_PASSWORD:secure_db_password}")
    private String dbPassword;

    @Value("${TRIMLINK_DB_DRIVER:org.postgresql.Driver}")
    private String dbDriverClass;

    @Value("${TRIMLINK_DB_INIT_RETRIES:15}")
    private int initRetries;

    @Value("${TRIMLINK_DB_INIT_RETRY_DELAY_MS:2000}")
    private int initRetryDelayMs;

    private HikariDataSource buildHikariDataPool(String shardName, String jdbcUrl) {
        HikariDataSource pool = new HikariDataSource();

        pool.setJdbcUrl(jdbcUrl);
        pool.setUsername(dbUsername);
        pool.setPassword(dbPassword);
        pool.setDriverClassName(dbDriverClass);

        pool.setMaximumPoolSize(20);
        pool.setMinimumIdle(2);
        pool.setIdleTimeout(30000);

        // Robust connection retry loop to handle startup timing race conditions
        boolean initialized = false;
        
        for (int i = 0; i < initRetries; i++) {
            try (java.sql.Connection conn = pool.getConnection();
                 java.sql.Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS url_mappings (" +
                             "id BIGINT PRIMARY KEY, " +
                             "short_code VARCHAR(10) UNIQUE NOT NULL, " +
                             "long_url VARCHAR(2048) NOT NULL, " +
                             "created_at TIMESTAMP NOT NULL" +
                             ")");
                stmt.execute("CREATE INDEX IF NOT EXISTS idx_short_code ON url_mappings (short_code)");
                System.out.println("[DATABASE AUTO-INIT] Successfully initialized schema on shard: " + shardName);
                initialized = true;
                break;
            } catch (Exception e) {
                System.out.printf("[DATABASE AUTO-INIT] Shard [%s] not ready (retry %d/%d): %s%n", 
                                  shardName, (i + 1), initRetries, e.getMessage());
                try {
                    Thread.sleep(initRetryDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        if (!initialized) {
            System.err.printf("[DATABASE AUTO-INIT] CRITICAL: Failed to initialize schema on shard [%s] after %d retries.%n", 
                              shardName, initRetries);
        }

        return pool;
    }

    @Bean
    @Primary
    public DataSource dataSource() {
        Map<Object, Object> configuredPoolMap = new HashMap<>();

        HikariDataSource shard0 = buildHikariDataPool("postgres-shard-0", shard0JdbcUrl);
        HikariDataSource shard1 = buildHikariDataPool("postgres-shard-1", shard1JdbcUrl);
        HikariDataSource shard2 = buildHikariDataPool("postgres-shard-2", shard2JdbcUrl);

        configuredPoolMap.put("postgres-shard-0", shard0);
        configuredPoolMap.put("postgres-shard-1", shard1);
        configuredPoolMap.put("postgres-shard-2", shard2);

        ShardedRoutingDataSource shardedRouter = new ShardedRoutingDataSource();
        shardedRouter.setTargetDataSources(configuredPoolMap);
        shardedRouter.setDefaultTargetDataSource(shard0);

        return shardedRouter;
    }
}
