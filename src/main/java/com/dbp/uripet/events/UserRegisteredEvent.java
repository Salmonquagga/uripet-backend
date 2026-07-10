package com.dbp.uripet.events;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter @AllArgsConstructor
public class UserRegisteredEvent {
    private final String userEmail;
    private final String userName;
    private final String verificationCode;
}
