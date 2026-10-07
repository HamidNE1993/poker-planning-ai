export type StoryPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type StoryStatus = 'PENDING' | 'VOTING' | 'ESTIMATED' | 'SKIPPED';

export interface UserStory {
  id: string;
  sessionId: string;
  storyKey: string;
  title: string;
  description?: string;
  acceptanceCriteria: string[];
  priority: StoryPriority;
  status: StoryStatus;
  finalEstimate?: string;
  orderIndex: number;
  votesRevealed: boolean;
  createdAt: string;
}

export interface CreateStoryDto {
  storyKey?: string;
  title: string;
  description?: string;
  acceptanceCriteria?: string[];
  priority?: StoryPriority;
}

export interface UpdateStoryDto {
  storyKey?: string;
  title: string;
  description?: string;
  acceptanceCriteria?: string[];
  priority?: StoryPriority;
  status?: StoryStatus;
  finalEstimate?: string;
}
