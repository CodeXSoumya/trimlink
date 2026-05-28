package com.codex.trimlink.node.storage;

public class ShardContext {
    // Thread-safe way to flag which shortcode we are currently processing
    // Tells Hibernate which DB Shard to talk to
    private static final ThreadLocal<String> CURRENT_SHARD_KEY = new ThreadLocal<>();

    public static void setShardKey(String shortCode) {
        CURRENT_SHARD_KEY.set(shortCode);
    }

    public static String getShardKey() {
        return CURRENT_SHARD_KEY.get();
    }

    public static void clear() {
        CURRENT_SHARD_KEY.remove();
    }
}
