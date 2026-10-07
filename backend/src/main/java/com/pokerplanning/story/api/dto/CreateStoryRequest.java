package com.pokerplanning.story.api.dto;

import com.pokerplanning.story.domain.StoryPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateStoryRequest(
    String storyKey,

    @NotBlank(message = "Le titre de la user story est obligatoire")
    @Size(max = 200, message = "Le titre ne peut pas dépasser 200 caractères")
    String title,

    String description,

    List<String> acceptanceCriteria,

    StoryPriority priority
) {
}
