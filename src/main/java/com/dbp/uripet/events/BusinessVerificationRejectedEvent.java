package com.dbp.uripet.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class BusinessVerificationRejectedEvent {
    private final String userEmail;
    private final String userName;
    private final String businessName;
    private final String businessType;
    private final String reviewComment;
}
