package com.dbp.uripet.events;
import lombok.AllArgsConstructor;
import lombok.Getter;
@Getter @AllArgsConstructor
public class UserAddedToPetEvent {
    private final String userEmail;
    private final String petName;
}
