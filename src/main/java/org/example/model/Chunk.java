package org.example.model;

import jakarta.persistence.*;

@Entity
@Table(name = "chunks")
public class Chunk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String hash;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private int orderIndex;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = false, columnDefinition = "BLOB")
    private byte[] data;

    public Chunk() {}

    public Chunk(String hash, String filePath, int orderIndex, byte[] data) {
        this.hash = hash;
        this.filePath = filePath;
        this.orderIndex = orderIndex;
        this.data = data;
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

    public byte[] getData() {
        return data;
    }
}