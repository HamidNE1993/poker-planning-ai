export type InsightType = 'AMBIGUITY' | 'ESTIMATE_GAP' | 'CONSENSUS_READY';

export interface AiInsight {
  storyTitle: string;
  sessionName: string;
  type: InsightType;
  title: string;
  message: string;
  timeAgo: string;
  confidence: number;
}
