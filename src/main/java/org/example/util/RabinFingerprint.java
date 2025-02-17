package org.example.util;

public class RabinFingerprint {
    private static final long POLYNOMIAL = 0x3DA3358B4DC173L;
    private long fingerprint = 0;

    public void update(int b) {
        fingerprint = (fingerprint << 1) ^ POLYNOMIAL ^ b;
    }

    public boolean isBoundary() {
        return (fingerprint & 0xFFF) == 0;
    }

    public void reset() {
        fingerprint = 0;
    }
}