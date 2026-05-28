package com.codex.trimlink.node.storage;

import java.util.List;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

import com.codex.trimlink.hashing.ConsistentHashingStrategy;
import com.codex.trimlink.hashing.Murmur3Strategy;

public class ShardedRoutingDataSource extends AbstractRoutingDataSource {

    private final ConsistentHashingStrategy dbHashRing;

    public ShardedRoutingDataSource() {
        // Building dedicated virtual consistent hash ring matching our 3 database shards
        List<String> shardIdentifiers = List.of(
            "postgres-shard-0", "postgres-shard-1", "postgres-shard-2"
        );
        // 200 virtual tokens per shard to ensure perfectly uniform distribution
        this.dbHashRing = new Murmur3Strategy(200, shardIdentifiers);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        String activeShortCode = ShardContext.getShardKey();

        // Fallback: Default
        if (activeShortCode == null) {
            return "postgres-shard-0";
        }
        
        // Run the shortcode string through your Murmur3 hash ring to resolve the owner shard string
        return dbHashRing.get(activeShortCode);
    }
    
}
