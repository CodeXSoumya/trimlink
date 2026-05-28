package com.codex.trimlink;

import com.codex.trimlink.node.storage.ShardedRoutingDataSource;
import com.codex.trimlink.node.kgs.ZookeeperRangeKgsClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
class TrimlinkApplicationTests {

	// Replace the real beans that connect to external services with mocks
	@MockBean
	private ShardedRoutingDataSource shardedRoutingDataSource;

	@MockBean
	private ZookeeperRangeKgsClient zookeeperRangeKgsClient;

	@MockBean
	private StringRedisTemplate stringRedisTemplate;

	@Test
	void contextLoads() {
		// This test will now pass because the application can load its context.
		// The beans that tried to connect to Postgres, Zookeeper, and Redis
		// have been replaced with empty, non-functional mock objects.
	}

}