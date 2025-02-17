package org.example.deduplication;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.Set;

@Service
public class DuplicateDetector {
    private final Set<Long> chunkHashes = new HashSet<>();

    public boolean isDuplicate(byte[] chunk) {
        long hash = computeXXHash(chunk);
        if (chunkHashes.contains(hash)) {
            return true;
        }
        chunkHashes.add(hash);
        return false;
    }

    public long computeXXHash(byte[] data) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        XXHash64 xxHash64 = factory.hash64();
        long seed = 0;
        return xxHash64.hash(data, 0, data.length, seed);
    }
}