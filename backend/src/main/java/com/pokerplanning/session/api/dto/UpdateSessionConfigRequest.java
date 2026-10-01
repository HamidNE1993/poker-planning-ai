package com.pokerplanning.session.api.dto;

import com.pokerplanning.session.domain.DeckType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateSessionConfigRequest(
    DeckType deckType,
    Boolean autoReveal,
    @Min(value = 10, message = "Le timer doit être d'au moins 10 secondes")
    @Max(value = 600, message = "Le timer ne peut pas dépasser 600 secondes")
    Integer timerDurationSeconds
) {}
