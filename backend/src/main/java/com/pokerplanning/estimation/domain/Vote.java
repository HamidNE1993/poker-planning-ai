package com.pokerplanning.estimation.domain;

import com.pokerplanning.participant.domain.Participant;
import com.pokerplanning.story.domain.UserStory;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "story_votes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"story_id", "participant_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "story_id", nullable = false)
    private UserStory story;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id", nullable = false)
    private Participant participant;

    @Column(nullable = false, length = 20)
    private String voteValue;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant votedAt = Instant.now();

    public Vote(UserStory story, Participant participant, String voteValue) {
        this.story = story;
        this.participant = participant;
        this.voteValue = voteValue;
        this.votedAt = Instant.now();
    }
}
