package org.example.deduplication;

import net.jpountz.xxhash.XXHash64;
import net.jpountz.xxhash.XXHashFactory;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.stereotype.Service;
import java.util.logging.Logger;

@Service
public class DuplicateDetector implements DuplicateDetectorInterface {
    private static final Logger logger = Logger.getLogger(DuplicateDetector.class.getName());
    private final Cache<Long, Boolean> chunkHashes = Caffeine.newBuilder()
            .maximumSize(100_000)
            .expireAfterAccess(10, java.util.concurrent.TimeUnit.MINUTES)
            .build();

    @Override
    public boolean isDuplicate(byte[] chunk) {
        long hash = computeXXHash(chunk);
        boolean isDuplicate = chunkHashes.getIfPresent(hash) != null;

        if (!isDuplicate) {
            chunkHashes.put(hash, true);
        }
        return isDuplicate;
    }

    @Override
    public long computeXXHash(byte[] data) {
        XXHashFactory factory = XXHashFactory.unsafeInstance();
        XXHash64 xxHash64 = factory.hash64();
        return xxHash64.hash(data, 0, data.length, 0);
    }
}
