package com.pokerplanning.participant.api.dto;

import com.pokerplanning.participant.domain.ParticipantRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record JoinSessionRequest(
    @NotBlank(message = "Le nom du participant est obligatoire")
    @Size(max = 100, message = "Le nom ne peut pas dépasser 100 caractères")
    String name,

    @Size(max = 255, message = "L'avatar ne peut pas dépasser 255 caractères")
    String avatar,

    ParticipantRole role
) {}
