package org.example.controller;

import org.example.model.Chunk;
import org.example.service.FileProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private final FileProcessor fileProcessor;

    public FileController(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    @PostMapping("/process")
    public ResponseEntity<List<Chunk>> processFile(@RequestParam String filePath) {
        try {
            List<Chunk> processedChunks = fileProcessor.processFile(filePath);
            return ResponseEntity.ok(processedChunks);
        } catch (IOException | NoSuchAlgorithmException e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<Chunk>> getAllChunks() {
        List<Chunk> chunks = fileProcessor.getAllChunks();
        return ResponseEntity.ok(chunks);
    }
}