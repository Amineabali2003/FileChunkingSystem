package org.example.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.example.chunking.FastCDCChunker;
import org.example.compression.CompressionService;
import org.example.deduplication.DuplicateDetector;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

@Service
public class FileProcessor {
    private static final Logger logger = Logger.getLogger(FileProcessor.class.getName());
    private static final int SEGMENT_SIZE = 4_194_304;
    private static final int THREAD_COUNT = Math.max(4, Runtime.getRuntime().availableProcessors());

    private final BlockingQueue<byte[]> segmentsQueue = new LinkedBlockingQueue<>(THREAD_COUNT * 2);
    private final BlockingQueue<byte[]> chunkQueue = new LinkedBlockingQueue<>(THREAD_COUNT * 10);
    private ExecutorService chunkerExecutor;
    private ExecutorService compressionExecutor;
    private ExecutorService dedupExecutor;

    private final FastCDCChunker chunker;
    private final DuplicateDetector deduplicator;
    private final CompressionService compressor;
    private final ChunkRepository chunkRepository;
    private final MeterRegistry meterRegistry;

    private final Cache<String, Boolean> deduplicationCache = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(5, TimeUnit.MINUTES)
            .build();

    public FileProcessor(FastCDCChunker chunker, DuplicateDetector deduplicator, CompressionService compressor,
                         ChunkRepository chunkRepository, MeterRegistry meterRegistry) {
        this.chunker = chunker;
        this.deduplicator = deduplicator;
        this.compressor = compressor;
        this.chunkRepository = chunkRepository;
        this.meterRegistry = meterRegistry;
        initializeExecutors();
    }

    private void initializeExecutors() {
        chunkerExecutor = Executors.newFixedThreadPool(THREAD_COUNT);
        compressionExecutor = Executors.newFixedThreadPool(THREAD_COUNT / 2);
        dedupExecutor = Executors.newFixedThreadPool(THREAD_COUNT / 2);
    }

    public synchronized List<Chunk> processFile(String filePath) throws Exception {
        if (chunkerExecutor.isShutdown() || compressionExecutor.isShutdown() || dedupExecutor.isShutdown()) {
            initializeExecutors();
        }

        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            throw new Exception("Fichier introuvable : " + filePath);
        }

        meterRegistry.gauge("file.original.size", Tags.of("filePath", filePath), file, File::length);

        if (chunkRepository.existsByFilePath(filePath)) {
            List<Chunk> existingChunks = chunkRepository.findByFilePath(filePath);
            if (!existingChunks.isEmpty()) {
                registerChunkMetrics(filePath);
                return existingChunks;
            }
        }

        List<Chunk> resultChunks = new CopyOnWriteArrayList<>();
        boolean isText = isTextFile(filePath);

        chunkerExecutor.submit(() -> readFileIntoQueue(filePath));
        startChunkerThreads(isText);
        startWorkerThreads(resultChunks, filePath);

        shutdownExecutors();
        registerChunkMetrics(filePath);

        return resultChunks;
    }

    private void registerChunkMetrics(String filePath) {
        Long totalChunkSize = chunkRepository.getTotalSizeByFilePath(filePath);
        double chunkSize = (totalChunkSize != null) ? totalChunkSize.doubleValue() : 0.0;
        meterRegistry.gauge("file.chunks.totalSize", Tags.of("filePath", filePath), chunkSize);
        meterRegistry.gauge("file.chunks.count", Tags.of("filePath", filePath), chunkRepository.countByFilePath(filePath));
    }

    private void readFileIntoQueue(String filePath) {
        try (FileChannel fileChannel = FileChannel.open(Paths.get(filePath), StandardOpenOption.READ)) {
            MappedByteBuffer buffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());
            while (buffer.hasRemaining()) {
                int remaining = Math.min(buffer.remaining(), SEGMENT_SIZE);
                byte[] segment = new byte[remaining];
                buffer.get(segment);
                if (!segmentsQueue.offer(segment, 5, TimeUnit.SECONDS)) {
                    logger.warning("Timeout lors de l'ajout du segment dans la queue.");
                }
            }
            segmentsQueue.put(new byte[0]);
        } catch (Exception e) {
            logger.severe("Erreur lors de la lecture du fichier : " + e.getMessage());
        }
    }

    private void startChunkerThreads(boolean isText) {
        for (int i = 0; i < THREAD_COUNT; i++) {
            chunkerExecutor.submit(() -> {
                try {
                    while (true) {
                        byte[] segment = segmentsQueue.take();
                        if (segment.length == 0) {
                            segmentsQueue.put(segment);
                            break;
                        }

                        List<byte[]> chunks = chunker.chunkData(segment, isText);

                        for (byte[] chunk : chunks) {
                            if (!chunkQueue.offer(chunk, 5, TimeUnit.SECONDS)) {
                                logger.warning("Timeout lors de l'ajout d'un chunk dans la queue.");
                            }
                        }
                    }
                    chunkQueue.put(new byte[0]);
                } catch (Exception e) {
                    logger.severe("Erreur dans un thread-Chunker : " + e.getMessage());
                }
            });
        }
    }

    private void startWorkerThreads(List<Chunk> resultChunks, String filePath) {
        AtomicInteger orderIndex = new AtomicInteger(0);
        for (int i = 0; i < THREAD_COUNT; i++) {
            compressionExecutor.submit(() -> {
                try {
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
                            Chunk chunkEntity = new Chunk(hash, filePath, orderIndex.getAndIncrement(), compressed);
                            dedupExecutor.submit(() -> {
                                chunkRepository.save(chunkEntity);
                                logger.info("Chunk enregistré en base : " + filePath + " | Hash: " + hash);
                            });
                        }
                    }
                } catch (Exception e) {
                    logger.severe("Erreur dans un thread-Worker : " + e.getMessage());
                }
            });
        }
    }

    private void shutdownExecutors() throws InterruptedException {
        shutdownExecutorService(chunkerExecutor, "ChunkerExecutor");
        shutdownExecutorService(compressionExecutor, "CompressionExecutor");
        shutdownExecutorService(dedupExecutor, "DedupExecutor");
    }

    private void shutdownExecutorService(ExecutorService executor, String name) throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
            executor.shutdownNow();
        }
    }

    private boolean isTextFile(String filePath) {
        return filePath.endsWith(".txt") || filePath.endsWith(".log") || filePath.endsWith(".csv");
    }

    public List<Chunk> getAllChunks() {
        return chunkRepository.findAll();
    }

    public boolean verifyChunkIntegrity(String filePath) {
        long originalSize = new File(filePath).length();
        long chunkedSize = chunkRepository.findByFilePathOrderByOrderIndex(filePath)
                .stream()
                .mapToLong(chunk -> chunk.getData().length)
                .sum();
        return originalSize == chunkedSize;
    }
}