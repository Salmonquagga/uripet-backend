package com.dbp.uripet.events;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserVerifiedEvent {
    private final String userEmail;
    private final String userName;
}