export interface LeaderboardRecord {
  rank?: number;
  playerName: string;
  boardSize: number;
  difficulty: 'easy' | 'medium' | 'hard' | '简单模式' | '中等模式' | '困难模式';
  timeTaken: number;
  timestamp: string;
  moves?: number;
}
