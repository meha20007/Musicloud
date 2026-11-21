package com.musicloud.repository;

import com.musicloud.model.PlaylistEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlaylistRepository extends JpaRepository<PlaylistEntity, Long> {

    // Only return playlists belonging to a specific user
    List<PlaylistEntity> findByUserId(String userId);

    // For security: find playlist only if it belongs to the user
    Optional<PlaylistEntity> findByIdAndUserId(Long id, String userId);
}
