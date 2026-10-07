package com.pokerplanning.ai.application;

import com.pokerplanning.ai.domain.StoryAnalysis;
import com.pokerplanning.story.domain.UserStory;

import java.util.List;

public interface StoryAnalysisService {
    StoryAnalysis analyze(UserStory story);
    StoryAnalysis analyzeText(String title, String description, List<String> acceptanceCriteria);
}
