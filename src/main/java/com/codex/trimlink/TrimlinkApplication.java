package com.codex.trimlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.codex.trimlink.node.storage")
public class TrimlinkApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrimlinkApplication.class, args);
	}

}
