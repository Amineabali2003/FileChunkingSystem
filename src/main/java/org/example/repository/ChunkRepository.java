package org.example.repository;

import org.example.model.Chunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChunkRepository extends JpaRepository<Chunk, Long> {

    @Query("SELECT COUNT(c) > 0 FROM Chunk c WHERE c.filePath = :filePath")
    boolean existsByFilePath(@Param("filePath") String filePath);

    @Query("SELECT c FROM Chunk c WHERE c.filePath = :filePath")
    List<Chunk> findByFilePath(@Param("filePath") String filePath);

    @Query("SELECT c FROM Chunk c WHERE c.filePath = :filePath ORDER BY c.orderIndex ASC")
    List<Chunk> findByFilePathOrderByOrderIndex(@Param("filePath") String filePath);

    @Query(value = "SELECT id, hash, file_path, order_index, data FROM chunks WHERE file_path = :filePath ORDER BY order_index ASC", nativeQuery = true)
    List<Object[]> findByFilePathOrderByOrderIndexNative(@Param("filePath") String filePath);

    @Query(value = "SELECT IFNULL(SUM(LENGTH(data)), 0) FROM chunks WHERE file_path = :filePath", nativeQuery = true)
    Long getTotalSizeByFilePath(@Param("filePath") String filePath);

    @Query("SELECT COUNT(c) FROM Chunk c WHERE c.filePath = :filePath")
    long countByFilePath(@Param("filePath") String filePath);
}