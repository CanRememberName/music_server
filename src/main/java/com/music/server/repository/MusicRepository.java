package com.music.server.repository;

import com.music.server.model.Music;
import java.util.List;
import java.util.Optional;

public interface MusicRepository {
    List<Music> findAll();
    Optional<Music> findById(String id);
    Music save(Music music);
    void deleteById(String id);
    List<Music> search(String keyword);
}
