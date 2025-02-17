package org.example.service;

import org.example.chunking.RabinChunker;
import org.example.compression.CompressionService;
import org.example.deduplication.DuplicateDetector;
import org.example.repository.ChunkRepository;
import org.example.model.Chunk;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());

    private final RabinChunker chunker;
    private final DuplicateDetector deduplicator;
    private final CompressionService compressor;
    private final ChunkRepository chunkRepository;

    public FileProcessor(RabinChunker chunker, DuplicateDetector deduplicator, CompressionService compressor, ChunkRepository chunkRepository) {
        this.chunker = chunker;
        this.deduplicator = deduplicator;
        this.compressor = compressor;
        this.chunkRepository = chunkRepository;
    }

    public List<Chunk> processFile(String filePath) throws IOException, NoSuchAlgorithmException {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            logger.severe("Le fichier spécifié n'existe pas : " + filePath);
            throw new IOException("Fichier introuvable : " + filePath);
        }

        logger.info("Début du traitement du fichier : " + filePath);

        List<byte[]> chunks = chunker.chunkFile(filePath);
        List<Chunk> savedChunks = new ArrayList<>();
        int orderIndex = 0;

        for (byte[] chunk : chunks) {
            if (!deduplicator.isDuplicate(chunk)) {
                byte[] compressedChunk = compressor.compress(chunk);
                String hash = deduplicator.computeSHA256(chunk);
                Chunk chunkEntity = new Chunk(null, hash, filePath, orderIndex++);
                chunkRepository.save(chunkEntity);
                savedChunks.add(chunkEntity);
            }
        }

        logger.info("Fichier traité avec succès : " + filePath);
        return savedChunks;
    }

    public List<Chunk> getAllChunks() {
        return chunkRepository.findAll();
    }
}