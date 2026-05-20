package com.codex.trimlink.node.storage;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrlMappingRepository extends JpaRepository<UrlMapping, Long> {
    // SQL query: "SELECT * FROM url_mappings WHERE short_code = ?"
    Optional<UrlMapping> findByShortCode(String shortCode);
}
