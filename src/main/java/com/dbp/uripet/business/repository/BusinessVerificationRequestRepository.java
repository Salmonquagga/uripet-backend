package com.dbp.uripet.business.repository;

import com.dbp.uripet.business.domain.BusinessVerificationRequest;
import com.dbp.uripet.business.domain.enums.BusinessVerificationStatus;
import com.dbp.uripet.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BusinessVerificationRequestRepository extends JpaRepository<BusinessVerificationRequest, Long> {
    Optional<BusinessVerificationRequest> findByUid(String uid);
    List<BusinessVerificationRequest> findByRequesterOrderByCreatedAtDesc(User requester);
    List<BusinessVerificationRequest> findByStatusOrderByCreatedAtDesc(BusinessVerificationStatus status);
    List<BusinessVerificationRequest> findAllByOrderByCreatedAtDesc();
    boolean existsByRequesterAndStatusIn(User requester, Collection<BusinessVerificationStatus> statuses);
}
