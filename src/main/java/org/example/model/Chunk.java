package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chunks")
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String hash;
    private String filePath;
    private int orderIndex;

    public Chunk() {}

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