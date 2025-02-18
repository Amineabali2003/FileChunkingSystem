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
}