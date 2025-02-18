package org.example.controller;

import org.example.model.Chunk;
import org.example.service.FileProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Logger logger = Logger.getLogger(FileController.class.getName());
    private final FileProcessor fileProcessor;

    public FileController(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    @PostMapping("/process")
    public ResponseEntity<List<Chunk>> processFile(@RequestParam String filePath) {
        try {
            List<Chunk> processedChunks = fileProcessor.processFile(filePath);
            return ResponseEntity.ok(processedChunks);
        } catch (Exception e) {
            logger.severe("Erreur lors du traitement du fichier : " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<Chunk>> getAllChunks() {
        return ResponseEntity.ok(fileProcessor.getAllChunks());
    }
}