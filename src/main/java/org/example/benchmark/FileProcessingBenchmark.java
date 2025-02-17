package org.example.benchmark;

import org.example.chunking.RabinChunker;
import org.example.compression.CompressionService;
import org.example.deduplication.DuplicateDetector;
import org.openjdk.jmh.annotations.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class FileProcessingBenchmark {
    private static final String TEST_FILE = "/Users/abalimohammedamine/Downloads/test.txt";
    private RabinChunker chunker;
    private DuplicateDetector deduplicator;
    private CompressionService compressor;
    private List<byte[]> chunks;

    @Setup(Level.Invocation)
    public void setUp() throws Exception {
        chunker = new RabinChunker();
        deduplicator = new DuplicateDetector();
        compressor = new CompressionService();
        chunks = chunker.chunkFile(TEST_FILE);
    }

    @Benchmark
    public List<byte[]> benchmarkChunking() throws IOException {
        return chunker.chunkFile(TEST_FILE);
    }

    @Benchmark
    public boolean benchmarkDeduplication() throws NoSuchAlgorithmException {
        boolean hasDuplicate = false;
        for (byte[] chunk : chunks) {
            if (deduplicator.isDuplicate(chunk)) {
                hasDuplicate = true;
            }
        }
        return hasDuplicate;
    }

    @Benchmark
    public byte[] benchmarkCompression() throws IOException {
        return compressor.compress(chunks.get(0));
    }
}