package org.example.deduplication;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class DuplicateDetector {
    private final Set<String> chunkHashes = new HashSet<>();

    public boolean isDuplicate(byte[] chunk) throws NoSuchAlgorithmException {
        String hash = computeSHA256(chunk);
        if (chunkHashes.contains(hash)) {
            return true;
        }
        chunkHashes.add(hash);
        return false;
    }

    public String computeSHA256(byte[] data) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data);
        return Base64.getEncoder().encodeToString(hash);
    }
}