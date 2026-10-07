export type SessionEventType =
  | 'SESSION_UPDATED'
  | 'PARTICIPANT_JOINED'
  | 'PARTICIPANT_UPDATED'
  | 'PARTICIPANT_LEFT'
  | 'STORY_CREATED'
  | 'STORY_UPDATED'
  | 'STORY_DELETED'
  | 'STORY_SELECTED'
  | 'VOTE_SUBMITTED'
  | 'VOTES_REVEALED'
  | 'VOTES_RESET'
  | 'ESTIMATE_FINALIZED'
  | 'TIMER_SYNC'
  | 'HEARTBEAT';

export interface SessionEvent<T = any> {
  sessionId: string;
  type: SessionEventType;
  payload: T;
  timestamp: string;
}

export interface TimerSyncPayload {
  action: 'START' | 'PAUSE' | 'RESET';
  remainingSeconds: number;
}
