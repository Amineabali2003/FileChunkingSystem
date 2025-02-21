package org.example.reconstruction;

import org.example.compression.CompressionService;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

@Service
public class FileReconstructor {
    private static final Logger logger = Logger.getLogger(FileReconstructor.class.getName());
    private final ChunkRepository chunkRepository;
    private final CompressionService compressionService;

    public FileReconstructor(ChunkRepository chunkRepository, CompressionService compressionService) {
        this.chunkRepository = chunkRepository;
        this.compressionService = compressionService;
    }

    public String reconstructFile(String filePath) {
        List<Object[]> rawChunks = chunkRepository.findByFilePathOrderByOrderIndexNative(filePath);

        if (rawChunks.isEmpty()) {
            return "Erreur: Aucun chunk trouvé pour " + filePath;
        }

        List<Chunk> chunks = new ArrayList<>();
        for (Object[] rawChunk : rawChunks) {
            Long id = ((Number) rawChunk[0]).longValue();
            String hash = (String) rawChunk[1];
            String path = (String) rawChunk[2];
            int orderIndex = ((Number) rawChunk[3]).intValue();
            byte[] data = (byte[]) rawChunk[4];  // 🚀 Correction : Assurer qu'on récupère byte[]

            if (data == null) {
                logger.warning("⚠️ Chunk " + id + " est vide. Il sera ignoré.");
                continue;
            }

            chunks.add(new Chunk(hash, path, orderIndex, data));
        }

        String reconstructedFilePath = filePath + ".reconstructed";
        File outputFile = new File(reconstructedFilePath);

        try {
            if (outputFile.exists()) {
                Files.delete(Paths.get(reconstructedFilePath));
            }

            boolean isText = isTextFile(filePath);

            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                for (Chunk chunk : chunks) {
                    byte[] chunkData = chunk.getData();

                    if (chunkData == null || chunkData.length == 0) {
                        logger.warning("⚠️ Un chunk vide détecté, il est ignoré.");
                        continue;
                    }

                    byte[] decompressedData;
                    try {
                        decompressedData = compressionService.decompress(chunkData);
                    } catch (Exception e) {
                        logger.warning("⚠️ Échec de la décompression, utilisation des données brutes.");
                        decompressedData = chunkData;
                    }

                    if (isText) {
                        String text = new String(decompressedData, StandardCharsets.UTF_8);
                        fos.write(text.getBytes(StandardCharsets.UTF_8));
                        fos.write(" ".getBytes(StandardCharsets.UTF_8));
                    } else {
                        fos.write(decompressedData);
                    }
                }
            }

            logger.info("✅ Fichier reconstruit avec succès : " + reconstructedFilePath);
            return "Fichier reconstruit avec succès : " + reconstructedFilePath;

        } catch (Exception e) {
            logger.severe("❌ Erreur lors de la reconstruction du fichier : " + e.getMessage());

            if (outputFile.exists()) {
                outputFile.delete();
            }

            return "Erreur lors de la reconstruction : " + e.getMessage();
        }
    }

    private boolean isTextFile(String filePath) {
        return filePath.endsWith(".txt") || filePath.endsWith(".log") || filePath.endsWith(".csv");
    }
}