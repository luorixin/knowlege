export interface KnowledgeBase {
  id: string;
  name: string;
  description: string;
  isPrivate: boolean;
  activeStatus: boolean; // Pulsing cyan indicator
  documentCount: number;
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'assistant' | 'system';
  content: string;
  citations?: string[]; // Citation IDs e.g. ["1", "2"]
  isAudio?: boolean;
}

export interface Conversation {
  id: string;
  title: string;
  time: string;
  messages: ChatMessage[];
  selectedKb: string;
}

export interface EvidenceSource {
  id: string;
  title: string;
  type: 'PDF' | 'Word' | 'Excel' | 'Webpage';
  pages: number;
  excerpt: string;
}

export interface TelemetryTask {
  id: string;
  stage: 'Parsing' | 'Embedding' | 'Completed' | 'Failed';
  documentName: string;
  status: 'Completed' | 'Running' | 'Failed' | 'Pending';
  progress: number; // 0 to 100
  modelName: string;
  retryCount: number;
  logs: string[];
}

export interface TeamMember {
  id: string;
  username: string;
  displayName: string;
  roles: ('Admin' | 'Content Editor' | 'Viewer')[];
  status: 'Active' | 'Inactive';
}
