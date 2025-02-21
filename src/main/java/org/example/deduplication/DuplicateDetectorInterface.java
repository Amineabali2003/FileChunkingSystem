package org.example.deduplication;

public interface DuplicateDetectorInterface {
    boolean isDuplicate(byte[] chunk);

    long computeXXHash(byte[] data);
}
