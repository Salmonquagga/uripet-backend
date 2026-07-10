package com.dbp.uripet.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BusinessVerificationRequestedEvent {
    private final String requestUid;
    private final String requesterEmail;
    private final String requesterName;
    private final String businessName;
    private final String businessType;
}
