package com.codex.trimlink.node.storage;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "url_mappings", indexes = {@Index(name="idx_short_code", columnList = "shortCode")})
public class UrlMapping { // Relational Data schema

    @Id
    private Long id;
    
    @Column(nullable = false, length = 10, unique = true) 
    private String shortCode;

    @Column(nullable = false, length = 2048)
    private String longUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UrlMapping() {} // Default Constructor

    public UrlMapping(Long id, String shortCode, String longUrl) {
        this.id = id;
        this.shortCode = shortCode;
        this.longUrl = longUrl;
    }

    public Long getId() {
        return this.id;
    }

    public String getShortcode() {
        return this.shortCode;
    }

    public String getLongUrl() {
        return this.longUrl;
    }

}