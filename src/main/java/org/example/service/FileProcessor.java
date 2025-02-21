package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.chunking.ChunkerInterface;
import org.example.chunking.FastCDCChunker;
import org.example.compression.CompressionServiceInterface;
import org.example.deduplication.DuplicateDetectorInterface;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Service
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private final ChunkerInterface chunker;
    private final DuplicateDetectorInterface deduplicator;
    private final CompressionServiceInterface compressor;
    private final ChunkRepository chunkRepository;
    private final FastCDCChunker textChunker;

    private final Cache<String, Boolean> deduplicationCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(5, java.util.concurrent.TimeUnit.MINUTES)
            .build();

    public FileProcessor(ChunkerInterface chunker, DuplicateDetectorInterface deduplicator,
                         CompressionServiceInterface compressor, ChunkRepository chunkRepository, FastCDCChunker textChunker) {
        this.chunker = chunker;
        this.deduplicator = deduplicator;
        this.compressor = compressor;
        this.chunkRepository = chunkRepository;
        this.textChunker = textChunker;
    }

    public synchronized List<Chunk> processFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new Exception("Fichier introuvable : " + filePath);
        }

        if (chunkRepository.existsByFilePath(filePath)) {
            return chunkRepository.findByFilePath(filePath);
        }

        byte[] fileData = Files.readAllBytes(Path.of(filePath));
        boolean isText = isTextFile(filePath);
        List<byte[]> chunks = isText ? textChunker.chunkTextData(new String(fileData)) : chunker.chunkData(fileData);

        AtomicInteger orderIndex = new AtomicInteger(0);
        List<Chunk> resultChunks = new ArrayList<>();

        for (byte[] chunk : chunks) {
            String chunkHash = Long.toString(deduplicator.computeXXHash(chunk));
            if (deduplicationCache.getIfPresent(chunkHash) == null) {
                deduplicationCache.put(chunkHash, true);
                byte[] compressed = compressor.compress(chunk);
                Chunk chunkEntity = new Chunk(chunkHash, filePath, orderIndex.getAndIncrement(), compressed);
                chunkRepository.save(chunkEntity);
                resultChunks.add(chunkEntity);
            }
        }

        return resultChunks;
    }

    private boolean isTextFile(String filePath) {
        return filePath.endsWith(".txt") || filePath.endsWith(".log") || filePath.endsWith(".csv");
    }

    public List<Chunk> getAllChunks() {
        return chunkRepository.findAll();
    }
}