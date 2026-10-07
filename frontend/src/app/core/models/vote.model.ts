import { ParticipantRole } from './participant.model';

export interface VoteDetail {
  participantId: string;
  participantName: string;
  participantRole: ParticipantRole;
  voteValue?: string | null;
  hasVoted: boolean;
  votedAt?: string;
}

export interface ConsensusStatistics {
  average?: number | null;
  median?: string | null;
  consensusLabel: string;
  consensusAgreementPercent: number;
  minVote?: string | null;
  maxVote?: string | null;
  totalVotes: number;
  totalVoters: number;
  unanimous: boolean;
  distribution: { [key: string]: number };
}

export interface StoryVotesResponse {
  storyId: string;
  sessionId: string;
  revealed: boolean;
  totalVoters: number;
  totalVotesReceived: number;
  votes: VoteDetail[];
  consensus?: ConsensusStatistics | null;
}

export interface SubmitVoteDto {
  participantId: string;
  voteValue: string;
}

export interface FinalizeEstimateDto {
  finalEstimate: string;
}
