package org.example.deduplication;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

@Service
public class DuplicateDetector {
    private static final Logger logger = Logger.getLogger(DuplicateDetector.class.getName());
    private final Set<Long> chunkHashes = ConcurrentHashMap.newKeySet();

    public boolean isDuplicate(byte[] chunk) {
        long hash = computeXXHash(chunk);
        boolean isDuplicate = !chunkHashes.add(hash);

        if (isDuplicate) {
            logger.info("Chunk détecté comme doublon: " + hash);
        } else {
            logger.info("Nouveau chunk ajouté: " + hash);
        }
        return isDuplicate;
    }

    public long computeXXHash(byte[] data) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        XXHash64 xxHash64 = factory.hash64();
        long seed = 0;
        return xxHash64.hash(data, 0, data.length, seed);
    }
}