import { Participant } from './participant.model';

export type SessionStatus = 'CREATED' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
export type DeckType = 'FIBONACCI' | 'MODIFIED_FIBONACCI' | 'T_SHIRT' | 'POWERS_OF_TWO';
export type TagSeverity = 'success' | 'info' | 'warn' | 'secondary' | 'danger' | 'contrast';

export interface SessionSummary {
  id: string;
  name: string;
  sprint: string;
  status: SessionStatus;
  statusLabel?: string;
  severity?: TagSeverity;
  deckType: DeckType;
  inviteCode: string;
  autoReveal: boolean;
  timerDurationSeconds: number;
  participantCount: number;
  storyCount?: number;
  completedStories?: number;
  lastActivity?: string;
  aiAgreementRate?: number;
  participants?: Participant[];
  createdAt: string;
  updatedAt: string;
}

export interface PlanningSession {
  id: string;
  name: string;
  sprint: string;
  status: SessionStatus;
  deckType: DeckType;
  deckValues: string[];
  inviteCode: string;
  autoReveal: boolean;
  timerDurationSeconds: number;
  participants: Participant[];
  createdAt: string;
  updatedAt: string;
}

export interface CreateSessionDto {
  name: string;
  sprint: string;
  deckType?: DeckType;
  autoReveal?: boolean;
  timerDurationSeconds?: number;
  facilitatorName: string;
}

export interface UpdateSessionStatusDto {
  status: SessionStatus;
}

export interface UpdateSessionConfigDto {
  deckType?: DeckType;
  autoReveal?: boolean;
  timerDurationSeconds?: number;
}
