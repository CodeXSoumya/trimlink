package com.codex.trimlink;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import com.codex.trimlink.config.TestDatabaseConfig;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabaseConfig.class)
class TrimlinkApplicationTests {

	@Test
	void contextLoads() {
	}

}