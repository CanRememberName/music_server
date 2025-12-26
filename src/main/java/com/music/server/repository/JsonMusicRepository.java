package com.music.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.server.model.Music;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class JsonMusicRepository implements MusicRepository {

    @Value("${app.music.data-file}")
    private String dataFilePath;

    private final ObjectMapper objectMapper;
    private final Map<String, Music> musicCache = new ConcurrentHashMap<>();

    public JsonMusicRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadData();
    }

    private void loadData() {
        File file = new File(dataFilePath);
        if (!file.exists()) {
            log.warn("Music data file not found at {}, creating empty DB.", dataFilePath);
            // Ensure directory exists
            file.getParentFile().mkdirs();
            saveData(); // Create empty file
            return;
        }

        try {
            List<Music> list = objectMapper.readValue(file, new TypeReference<List<Music>>() {});
            list.forEach(m -> musicCache.put(m.getId(), m));
            log.info("Loaded {} music items.", musicCache.size());
        } catch (IOException e) {
            log.error("Failed to load music data", e);
        }
    }

    private synchronized void saveData() {
        try {
            objectMapper.writeValue(new File(dataFilePath), new ArrayList<>(musicCache.values()));
        } catch (IOException e) {
            log.error("Failed to save music data", e);
        }
    }

    @Override
    public List<Music> findAll() {
        return new ArrayList<>(musicCache.values());
    }

    @Override
    public Optional<Music> findById(String id) {
        return Optional.ofNullable(musicCache.get(id));
    }

    @Override
    public Music save(Music music) {
        musicCache.put(music.getId(), music);
        saveData(); // Simple synchronous write for this requirement
        return music;
    }

    @Override
    public void deleteById(String id) {
        musicCache.remove(id);
        saveData();
    }

    @Override
    public List<Music> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }
        String k = keyword.toLowerCase();
        return musicCache.values().stream()
                .filter(m -> m.getTitle().toLowerCase().contains(k) || 
                             m.getArtist().toLowerCase().contains(k) ||
                             m.getAlbum().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }
}
