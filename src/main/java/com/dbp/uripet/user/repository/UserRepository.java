package com.dbp.uripet.user.repository;

import com.dbp.uripet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUid(String uid);
    Optional<User> findByEmailAndVerificationCode(String email, String verificationCode);
    boolean existsByEmail(String email);
    Optional<User> findByName(String name);
}
