package com.pokerplanning.participant.api.dto;

import com.pokerplanning.participant.domain.ParticipantRole;
import jakarta.validation.constraints.NotNull;

public record UpdateParticipantRoleRequest(
    @NotNull(message = "Le rôle est obligatoire")
    ParticipantRole role
) {}
