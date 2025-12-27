package com.music.server.repository;

import com.music.server.model.User;

import java.util.List;
import java.util.Optional;

public interface UserManageRepository {
    List<User> findAll();
    Optional<User> findById(String id);
    User save(User user);
    void deleteById(String id);
    User findByUsername(String username);
}
