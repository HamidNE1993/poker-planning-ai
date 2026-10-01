package com.pokerplanning.session.api.dto;

import com.pokerplanning.session.domain.SessionStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSessionStatusRequest(
    @NotNull(message = "Le statut est obligatoire")
    SessionStatus status
) {}
