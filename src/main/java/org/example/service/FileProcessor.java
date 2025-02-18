package org.example.service;

import org.example.chunking.RabinChunker;
import org.example.compression.CompressionService;
import org.example.deduplication.DuplicateDetector;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Service
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private static final int SEGMENT_SIZE = 10000000;
    private final BlockingQueue<byte[]> segmentsQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<byte[]> chunkQueue = new ArrayBlockingQueue<>(50);
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

    public List<Chunk> processFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            logger.severe("Le fichier spécifié n'existe pas : " + filePath);
            throw new Exception("Fichier introuvable : " + filePath);
        }

        if (chunkRepository.existsByFilePath(filePath)) {
            logger.info("Le fichier a déjà été traité : " + filePath);
            return chunkRepository.findByFilePath(filePath);
        }

        logger.info("Début du traitement du fichier : " + filePath);
        List<Chunk> resultChunks = new CopyOnWriteArrayList<>();
        ExecutorService executor = Executors.newCachedThreadPool();

        executor.submit(() -> {
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] buffer = new byte[SEGMENT_SIZE];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    byte[] segment = new byte[bytesRead];
                    System.arraycopy(buffer, 0, segment, 0, bytesRead);
                    segmentsQueue.put(segment);
                }
                segmentsQueue.put(new byte[0]);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        int chunkerThreads = 2;
        for (int i = 0; i < chunkerThreads; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        byte[] segment = segmentsQueue.take();
                        if (segment.length == 0) {
                            segmentsQueue.put(segment);
                            break;
                        }
                        List<byte[]> chunks = chunker.chunkData(segment);
                        for (byte[] ch : chunks) {
                            chunkQueue.put(ch);
                        }
                    }
                    chunkQueue.put(new byte[0]);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        int workerThreads = 2;
        AtomicInteger orderIndex = new AtomicInteger(0);
        for (int i = 0; i < workerThreads; i++) {
            executor.submit(() -> {
                try {
                    while (true) {
                        byte[] chunk = chunkQueue.take();
                        if (chunk.length == 0) {
                            chunkQueue.put(chunk);
                            break;
                        }

                        if (!deduplicator.isDuplicate(chunk)) {
                            logger.info("Chunk unique, compression en cours...");
                            byte[] compressed = compressor.compress(chunk);
                            long hashVal = deduplicator.computeXXHash(chunk);
                            Chunk chunkEntity = new Chunk(null, Long.toString(hashVal), filePath, orderIndex.getAndIncrement());
                            chunkRepository.save(chunkEntity);
                            resultChunks.add(chunkEntity);
                        } else {
                            logger.info("Chunk ignoré (déjà existant).");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        logger.info("Fichier traité avec succès : " + filePath);
        return new ArrayList<>(resultChunks);
    }

    public List<Chunk> getAllChunks() {
        return chunkRepository.findAll();
    }
}