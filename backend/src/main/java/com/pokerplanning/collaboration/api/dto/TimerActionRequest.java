package com.pokerplanning.collaboration.api.dto;

import jakarta.validation.constraints.NotNull;

public record TimerActionRequest(
    @NotNull TimerAction action,
    int remainingSeconds
) {
    public enum TimerAction {
        START,
        PAUSE,
        RESET
    }
}
