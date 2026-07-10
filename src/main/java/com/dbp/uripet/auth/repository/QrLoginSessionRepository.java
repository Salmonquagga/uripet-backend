package com.dbp.uripet.auth.repository;

import com.dbp.uripet.auth.domain.QrLoginSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrLoginSessionRepository extends JpaRepository<QrLoginSession, Long> {
    Optional<QrLoginSession> findByToken(String token);
}
