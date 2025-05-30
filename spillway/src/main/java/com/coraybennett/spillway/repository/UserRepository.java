package com.coraybennett.spillway.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.coraybennett.spillway.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByConfirmationToken(String confirmationToken);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}