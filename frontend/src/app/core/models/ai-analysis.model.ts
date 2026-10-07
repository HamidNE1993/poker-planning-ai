export type ComplexityLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'VERY_HIGH';
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';

export interface ClarificationQuestion {
  category: string;
  question: string;
}

export interface StoryAnalysis {
  storyId: string | null;
  storyKey: string | null;
  complexity: ComplexityLevel;
  riskLevel: RiskLevel;
  clarityScore: number;
  suggestedEstimateRange: string;
  estimateRationale: string;
  missingAcceptanceCriteriaAlerts: string[];
  clarificationQuestions: ClarificationQuestion[];
  detectedRiskFactors: string[];
}

export interface AnalyzeTextRequest {
  title: string;
  description?: string;
  acceptanceCriteria?: string[];
}
