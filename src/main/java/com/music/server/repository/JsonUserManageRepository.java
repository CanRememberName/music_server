package com.music.server.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.music.server.model.User;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Repository
public class JsonUserManageRepository implements UserManageRepository {

    @Value("${app.music.user-file}")
    private String dataFilePath;

    private final ObjectMapper objectMapper;

    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    public JsonUserManageRepository(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void init() {
        loadData();
    }

    private void loadData() {
        File file = new File(dataFilePath);
        if (!file.exists()) {
            log.warn("User data file not found at {}, creating empty DB.", dataFilePath);
            // Ensure directory exists
            file.getParentFile().mkdirs();
            saveData(); // Create empty file
            return;
        }

        try {
            List<User> list = objectMapper.readValue(file, new TypeReference<List<User>>() {});
            list.forEach(m -> userCache.put(m.getId(), m));
            log.info("Loaded {} user items.", userCache.size());
        } catch (IOException e) {
            log.error("Failed to load user data", e);
        }
    }

    private synchronized void saveData() {
        try {
            objectMapper.writeValue(new File(dataFilePath), new ArrayList<>(userCache.values()));
        } catch (IOException e) {
            log.error("Failed to save user data", e);
        }
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(userCache.values());
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(userCache.get(id));
    }

    @Override
    public User save(User user) {
        userCache.put(user.getId(), user);
        saveData(); // Simple synchronous write for this requirement
        return user;
    }

    @Override
    public void deleteById(String id) {
        userCache.remove(id);
        saveData();
    }

    @Override
    public User findByUsername(String username) {
        return userCache.values().stream().filter(u -> u.getUsername().equals(username)).findFirst().orElse(new User());
    }
}
