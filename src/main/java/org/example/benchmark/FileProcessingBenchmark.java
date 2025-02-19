package org.example.benchmark;

import org.example.chunking.RabinChunker;
import org.example.compression.CompressionService;
import org.example.deduplication.DuplicateDetector;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
public class FileProcessingBenchmark {
    private static final String TEST_FILE = "/Users/abalimohammedamine/Downloads/test.txt";
    private RabinChunker chunker;
    private DuplicateDetector deduplicator;
    private CompressionService compressor;
    private List<byte[]> chunks;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        chunker = new RabinChunker();
        deduplicator = new DuplicateDetector();
        compressor = new CompressionService();
        byte[] fileData = Files.readAllBytes(Paths.get(TEST_FILE));
        chunks = chunker.chunkData(fileData);
    }

    @Benchmark
    @Threads(4)
    public List<byte[]> benchmarkChunking() throws IOException {
        return chunker.chunkData(chunks.get(0));
    }

    @Benchmark
    public boolean benchmarkDeduplication() {
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