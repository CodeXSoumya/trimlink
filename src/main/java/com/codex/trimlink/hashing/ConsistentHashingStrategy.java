package com.codex.trimlink.hashing;

public interface ConsistentHashingStrategy {
	void add(String node);
	void remove(String node);
	String get(String key);
	boolean isEmpty();
}
