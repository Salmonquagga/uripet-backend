package com.dbp.uripet.billing.repository;

import com.dbp.uripet.billing.domain.PaymentMethod;
import com.dbp.uripet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {
    Optional<PaymentMethod> findByUid(String uid);
    List<PaymentMethod> findByUserAndActiveTrue(User user);
}