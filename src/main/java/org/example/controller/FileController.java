package org.example.controller;

import org.example.model.Chunk;
import org.example.reconstruction.FileReconstructor;
import org.example.service.FileProcessor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/files")
public class FileController {
    private static final Logger logger = Logger.getLogger(FileController.class.getName());
    private final FileProcessor fileProcessor;
    private final FileReconstructor fileReconstructor;

    public FileController(FileProcessor fileProcessor, FileReconstructor fileReconstructor) {
        this.fileProcessor = fileProcessor;
        this.fileReconstructor = fileReconstructor;
    }

    @PostMapping("/process")
    public ResponseEntity<List<Chunk>> processFile(@RequestParam String filePath) {
        try {
            List<Chunk> processedChunks = fileProcessor.processFile(filePath);
            return ResponseEntity.ok(processedChunks);
        } catch (Exception e) {
            logger.severe("❌ Erreur lors du traitement du fichier : " + e.getMessage());
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping
    public ResponseEntity<List<Chunk>> getAllChunks() {
        return ResponseEntity.ok(fileProcessor.getAllChunks());
    }

    @PostMapping("/reconstruct")
    public ResponseEntity<String> reconstructFile(@RequestParam String filePath) {
        String result = fileReconstructor.reconstructFile(filePath);
        return result.startsWith("Erreur") ? ResponseEntity.badRequest().body(result) : ResponseEntity.ok(result);
    }
}