package com.musicloud.repository;

import com.musicloud.model.SongEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SongRepository extends JpaRepository<SongEntity, Long> {

    // Search across title, artist, genre (case-insensitive)
    @Query("SELECT s FROM SongEntity s WHERE " +
           "LOWER(s.title) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(s.artist) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(s.genre) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<SongEntity> searchSongs(@Param("term") String term);

    // Filter by exact genre (case-insensitive)
    List<SongEntity> findByGenreIgnoreCase(String genre);

    // Order newest first
    List<SongEntity> findAllByOrderByCreatedAtDesc();
}
