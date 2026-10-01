package com.pokerplanning.session.api.dto;

import com.pokerplanning.session.domain.DeckType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSessionRequest(
    @NotBlank(message = "Le nom de la session est obligatoire")
    @Size(max = 150, message = "Le nom ne peut pas dépasser 150 caractères")
    String name,

    @Size(max = 100, message = "Le sprint ne peut pas dépasser 100 caractères")
    String sprint,

    DeckType deckType,

    Boolean autoReveal,

    @Min(value = 10, message = "Le timer doit être d'au moins 10 secondes")
    @Max(value = 600, message = "Le timer ne peut pas dépasser 600 secondes")
    Integer timerDurationSeconds,

    @NotBlank(message = "Le nom du créateur / facilitateur est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String facilitatorName
) {}
