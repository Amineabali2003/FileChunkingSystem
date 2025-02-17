package org.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "chunks", indexes = {
        @Index(name = "idx_chunk_hash", columnList = "hash")
})
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String hash;
    private String filePath;
    private int orderIndex;

    public Chunk() {
    }

    public Chunk(Long id, String hash, String filePath, int orderIndex) {
        this.id = id;
        this.hash = hash;
        this.filePath = filePath;
        this.orderIndex = orderIndex;
    }

    public Long getId() {
        return id;
    }

    public String getHash() {
        return hash;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getOrderIndex() {
        return orderIndex;
    }
}