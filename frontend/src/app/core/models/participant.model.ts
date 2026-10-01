export type ParticipantRole = 'FACILITATOR' | 'VOTER' | 'OBSERVER';

export interface Participant {
  id?: string;
  sessionId?: string;
  name: string;
  avatar?: string;
  role: ParticipantRole;
  online: boolean;
  joinedAt?: string;
  lastHeartbeatAt?: string;
  voted?: boolean;
  currentVote?: string | null;
}

export interface JoinSessionDto {
  name: string;
  avatar?: string;
  role?: ParticipantRole;
}

export interface UpdateParticipantRoleDto {
  role: ParticipantRole;
}
