package org.example.reconstruction;

import com.github.luben.zstd.Zstd;
import org.example.model.Chunk;
import org.example.repository.ChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Logger;

@Service
public class FileReconstructor {
    private static final Logger logger = Logger.getLogger(FileReconstructor.class.getName());
    private static final String OUTPUT_DIR = "reconstructed_files/";

    private final ChunkRepository chunkRepository;

    public FileReconstructor(ChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public String reconstructFile(String filePath) {
        try {
            List<Chunk> chunks = chunkRepository.findByFilePathOrderByOrderIndex(filePath);

            if (chunks.isEmpty()) {
                logger.warning("⚠️ Aucun chunk trouvé pour " + filePath);
                return "Erreur : Aucun chunk trouvé.";
            }

            String outputFileName = OUTPUT_DIR + Paths.get(filePath).getFileName();
            Files.createDirectories(Paths.get(OUTPUT_DIR));

            try (FileOutputStream fos = new FileOutputStream(outputFileName)) {
                for (Chunk chunk : chunks) {
                    byte[] decompressedData = decompressData(chunk.getData());
                    fos.write(decompressedData);
                }
            }

            logger.info("✅ Reconstruction terminée : " + outputFileName);
            return "Fichier reconstruit avec succès : " + outputFileName;

        } catch (IOException e) {
            logger.severe("❌ Erreur lors de la reconstruction : " + e.getMessage());
            return "Erreur lors de la reconstruction.";
        }
    }

    private byte[] decompressData(byte[] compressedData) {
        long decompressedSize = Zstd.decompressedSize(compressedData);
        return Zstd.decompress(compressedData, (int) decompressedSize);
    }
}