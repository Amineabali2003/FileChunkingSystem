package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.example.chunking.FastCDCChunker;
import org.example.compression.CompressionService;
import org.example.deduplication.DuplicateDetector;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Service
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private static final int SEGMENT_SIZE = 10_000_000;
    private static final int THREAD_COUNT = Math.max(4, Runtime.getRuntime().availableProcessors() * 2);

    private final BlockingQueue<byte[]> segmentsQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<byte[]> chunkQueue = new ArrayBlockingQueue<>(50);
    private final ExecutorService executor;

    private final FastCDCChunker chunker;
    private final DuplicateDetector deduplicator;
    private final CompressionService compressor;
    private final ChunkRepository chunkRepository;

    private final Cache<String, Boolean> deduplicationCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    public FileProcessor(FastCDCChunker chunker, DuplicateDetector deduplicator, CompressionService compressor, ChunkRepository chunkRepository) {
        this.chunker = chunker;
        this.deduplicator = deduplicator;
        this.compressor = compressor;
        this.chunkRepository = chunkRepository;
        this.executor = new ThreadPoolExecutor(THREAD_COUNT, THREAD_COUNT * 2,
                60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(100));
    }

    public List<Chunk> processFile(String filePath) throws Exception {
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            logger.severe("❌ Le fichier n'existe pas : " + filePath);
            throw new Exception("Fichier introuvable : " + filePath);
        }

        if (chunkRepository.existsByFilePath(filePath)) {
            logger.info("ℹ️ Le fichier a déjà été traité : " + filePath);
            return chunkRepository.findByFilePath(filePath);
        }

        logger.info("🔄 Début du traitement du fichier : " + filePath);
        List<Chunk> resultChunks = new CopyOnWriteArrayList<>();

        executor.submit(() -> readFileIntoQueue(filePath));
        startChunkerThreads();
        startWorkerThreads(resultChunks, filePath);

        executor.shutdown();
        executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);

        logger.info("✅ Fichier traité avec succès : " + filePath);
        return new ArrayList<>(resultChunks);
    }

    private void readFileIntoQueue(String filePath) {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ)) {
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            logger.info("📖 Lecture du fichier commencée...");
            while (buffer.hasRemaining()) {
                int remaining = Math.min(buffer.remaining(), SEGMENT_SIZE);
                byte[] segment = new byte[remaining];
                buffer.get(segment);
                segmentsQueue.put(segment);
                logger.info("📥 Segment ajouté (taille : " + remaining + " octets)");
            }
            segmentsQueue.put(new byte[0]); // Signal de fin
            logger.info("✅ Fin de la lecture du fichier.");
        } catch (Exception e) {
            logger.severe("❌ Erreur lors de la lecture du fichier : " + e.getMessage());
        }
    }

    private void startChunkerThreads() {
        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadId = i + 1;
            executor.submit(() -> {
                try {
                    logger.info("🔧 Thread-Chunker " + threadId + " démarré.");
                    while (true) {
                        byte[] segment = segmentsQueue.take();
                        if (segment.length == 0) {
                            segmentsQueue.put(segment);
                            break;
                        }
                        List<byte[]> chunks = chunker.chunkData(segment);
                        for (byte[] ch : chunks) {
                            chunkQueue.put(ch);
                            if (threadId == 1) {
                                logger.info("🧩 Chunk ajouté (taille : " + ch.length + " octets)");
                            }
                        }
                    }
                    chunkQueue.put(new byte[0]);
                    logger.info("✅ Thread-Chunker " + threadId + " terminé.");
                } catch (Exception e) {
                    logger.severe("❌ Erreur dans le thread-Chunker " + threadId + " : " + e.getMessage());
                }
            });
        }
    }

    private void startWorkerThreads(List<Chunk> resultChunks, String filePath) {
        AtomicInteger orderIndex = new AtomicInteger(0);
        for (int i = 0; i < THREAD_COUNT; i++) {
            int threadId = i + 1;
            executor.submit(() -> {
                try {
                    logger.info("💼 Thread-Worker " + threadId + " démarré.");
                    while (true) {
                        byte[] chunk = chunkQueue.take();
                        if (chunk.length == 0) {
                            chunkQueue.put(chunk);
                            break;
                        }

                        String hash = Long.toString(deduplicator.computeXXHash(chunk));
                        if (deduplicationCache.getIfPresent(hash) == null) {
                            deduplicationCache.put(hash, true);
                            byte[] compressed = compressor.compress(chunk);
                            Chunk chunkEntity = new Chunk(null, hash, filePath, orderIndex.getAndIncrement());
                            chunkRepository.save(chunkEntity);
                            resultChunks.add(chunkEntity);
                            if (threadId == 1) {
                                logger.info("💾 Chunk compressé et sauvegardé (Hash : " + hash + ")");
                            }
                        } else {
                            if (threadId == 1) {
                                logger.info("🔁 Chunk ignoré (déjà existant).");
                            }
                        }
                    }
                    logger.info("✅ Thread-Worker " + threadId + " terminé.");
                } catch (Exception e) {
                    logger.severe("❌ Erreur dans le thread-Worker " + threadId + " : " + e.getMessage());
                }
            });
        }
    }

    public List<Chunk> getAllChunks() {
        return chunkRepository.findAll();
    }
}