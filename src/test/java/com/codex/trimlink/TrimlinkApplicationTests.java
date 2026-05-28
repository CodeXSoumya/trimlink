package com.codex.trimlink;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "zookeeper.connectionString=localhost:2181",
    "spring.data.redis.host=localhost"
})
class TrimlinkApplicationTests {

	@Test
	void contextLoads() {
	}

}
