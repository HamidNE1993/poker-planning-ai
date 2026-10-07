package com.pokerplanning.story.domain;

import com.pokerplanning.session.domain.PlanningSession;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "user_stories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class UserStory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private PlanningSession session;

    @Column(nullable = false, length = 30)
    private String storyKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "story_acceptance_criteria", joinColumns = @JoinColumn(name = "story_id"))
    @Column(name = "criterion", length = 500)
    @Builder.Default
    private List<String> acceptanceCriteria = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StoryPriority priority = StoryPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StoryStatus status = StoryStatus.PENDING;

    @Column(length = 20)
    private String finalEstimate;

    @Column(nullable = false)
    @Builder.Default
    private Integer orderIndex = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean votesRevealed = false;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    @Builder.Default
    private Instant updatedAt = Instant.now();

    public UserStory(PlanningSession session, String storyKey, String title, String description,
                     List<String> acceptanceCriteria, StoryPriority priority, Integer orderIndex) {
        this.session = session;
        this.storyKey = storyKey;
        this.title = title;
        this.description = description;
        if (acceptanceCriteria != null) {
            this.acceptanceCriteria = new ArrayList<>(acceptanceCriteria);
        }
        this.priority = priority != null ? priority : StoryPriority.MEDIUM;
        this.status = StoryStatus.PENDING;
        this.orderIndex = orderIndex != null ? orderIndex : 0;
        this.votesRevealed = false;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
