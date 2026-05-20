package com.codex.trimlink.hashing;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.TreeMap;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

public class Murmur3Strategy implements ConsistentHashingStrategy {
	
	// Non-cryptographic fast lightweight hash function from Google Guava
	private final HashFunction hashFunction = Hashing.murmur3_128();
	
	private final int REPLICA_COUNT; // Number of replicas in the ring - Virtual Nodes
	private final TreeMap<Long, String> circle = new TreeMap<>(); // The Hash Ring
	
	// Initialization of the Replica Count, and virtual nodes are added in the hash ring
	public Murmur3Strategy(int numberOfReplicas, Collection<String> nodes) {
		this.REPLICA_COUNT = numberOfReplicas;
		for (String node : nodes) {
			add(node);
		}
	}

	// Check if the hash ring is empty (no nodes available)
	public boolean isEmpty() {
		return circle.isEmpty();
	}
	
	// Addition of Virtual Nodes to the Hash Ring in a distributed manner
	public void add(String node) {
		for (int i = 0; i < REPLICA_COUNT; i++) {
			String key = node + ":V" + i;
			long hash = hashFunction.hashString(key, StandardCharsets.UTF_8).asLong();
			circle.put(hash, node);
		}
	}
	
	// Removal of Virtual Nodes from the Hash Ring on node removal
	public void remove(String node) {
		for (int i = 0; i < REPLICA_COUNT; i++) {
			String key = node + ":V" + i;
			long hash = hashFunction.hashString(key, StandardCharsets.UTF_8).asLong();
			circle.remove(hash);
		}
	}
	
	// Calculating the appropriate node when a key (provided by client) is processed or stored
	public String get(String key) {
		if (circle.isEmpty()) return null;
		
		long hash = hashFunction.hashString(key, StandardCharsets.UTF_8).asLong();
		Long nodeHash = circle.ceilingKey(hash);
		if (nodeHash == null) nodeHash = circle.firstKey();
		
		return circle.get(nodeHash);
	}
	
	/*
	public static void main(String[] args) {
		List<String> nodes = Arrays.asList("Node-A", "Node-B", "Node-C", "Node-D", "Node-E", 
                "Node-F", "Node-G", "Node-H", "Node-I", "Node-J");

		// Try changing 200 to 1 and see how the distribution gets worse!
		Murmur3Strategy ring = new Murmur3Strategy(200, nodes);
		
		Map<String, Integer> distribution = new HashMap<>();
		int totalRequests = 100_000;
		
		for (int i = 0; i < totalRequests; i++) {
			String url = "https://google.com/search?q=" + UUID.randomUUID();
			String assignedNode = ring.get(url);
			distribution.put(assignedNode, distribution.getOrDefault(assignedNode, 0) + 1);
		}
		
		System.out.println("--- Node Load Distribution (100k URLs) ---");
		distribution.forEach((node, count) -> {
			double percentage = (count / (double) totalRequests) * 100;
			System.out.printf("%s: %d requests (%.2f%%)%n", node, count, percentage);
		});
	}
	*/

}
