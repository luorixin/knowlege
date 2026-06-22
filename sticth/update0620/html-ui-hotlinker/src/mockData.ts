import { KnowledgeBase, Conversation, EvidenceSource, TelemetryTask, TeamMember } from './types';

export const INITIAL_KNOWLEDGE_BASES: KnowledgeBase[] = [
  {
    id: 'kb-1',
    name: 'Product Documentation',
    description: 'Comprehensive guides and manuals for all current enterprise and cloud products.',
    isPrivate: true,
    activeStatus: true,
    documentCount: 145,
  },
  {
    id: 'kb-2',
    name: 'Sales Playbook',
    description: 'Key strategies, competitive analysis, negotiation guides, and enterprise sales process info.',
    isPrivate: false,
    activeStatus: true,
    documentCount: 82,
  },
  {
    id: 'kb-3',
    name: 'Technical Support Guide',
    description: 'Troubleshooting steps, common error FAQs, and technical architecture specifications.',
    isPrivate: false,
    activeStatus: true,
    documentCount: 204,
  },
  {
    id: 'kb-4',
    name: 'Global Enterprise Docs',
    description: 'Aggregated regulatory compliance, financial reports, and strategic executive briefings.',
    isPrivate: false,
    activeStatus: true,
    documentCount: 312,
  },
  {
    id: 'kb-5',
    name: 'HR & Policy Manual',
    description: 'Employee onboarding, internal operational procedures, benefit descriptions, and standard rules.',
    isPrivate: true,
    activeStatus: false,
    documentCount: 45,
  }
];

export const INITIAL_EVIDENCE_SOURCES: EvidenceSource[] = [
  {
    id: '1',
    title: 'Project X Q3 Report',
    type: 'PDF',
    pages: 54,
    excerpt: 'Efficiency gains of 15% were realized within the first month of phase 2 implementation. Synergistic operation curves indicate accelerated ingestion vectors across cloud partitions.'
  },
  {
    id: '2',
    title: 'Resource Allocation Memo 2023',
    type: 'Word',
    pages: 3,
    excerpt: 'Team bandwidth remains a primary concern in Q4 due to competing priorities in security engineering. Re-assignment of embedding loads has alleviated buffer pools by 22%.'
  },
  {
    id: '3',
    title: 'Technical Support Guide - Volume II',
    type: 'PDF',
    pages: 120,
    excerpt: 'Database connection retry counts should scale linearly. When auth states collapse, verify active CORS policies and validate the client secret payload locally.'
  },
  {
    id: '4',
    title: 'Compliance Handbook 2026',
    type: 'Webpage',
    pages: 1,
    excerpt: 'All multi-tenant telemetry queues must deploy encrypted TLS 1.3 streams. Document extraction nodes require strict sandboxing constraints during PDF parsing cycles.'
  }
];

export const INITIAL_CONVERSATIONS: Conversation[] = [
  {
    id: 'chat-1',
    title: 'Project X Status Update',
    time: '1:58 am',
    selectedKb: 'kb-4',
    messages: [
      {
        id: 'm1',
        sender: 'user',
        content: 'What are the key findings from the latest project X report?'
      },
      {
        id: 'm2',
        sender: 'assistant',
        content: 'Based on the recent analysis [1], the key findings indicate a 15% increase in efficiency due to the new implementation. There were also challenges noted in resource allocation [2] which are being addressed.',
        citations: ['1', '2']
      },
      {
        id: 'm3',
        sender: 'user',
        content: 'Can you elaborate on the resource allocation challenges?'
      },
      {
        id: 'm4',
        sender: 'assistant',
        content: 'I noticed some elevated bandwidth constraints. I can pull up a waveform of the automated audio synthesis run for this brief to walk through details.',
        isAudio: true
      }
    ]
  },
  {
    id: 'chat-2',
    title: 'Quarterly Report Analysis',
    time: '1:59 pm',
    selectedKb: 'kb-4',
    messages: [
      {
        id: 'ca-1',
        sender: 'user',
        content: 'Summarize our compliance benchmarks for TLS 1.3.'
      },
      {
        id: 'ca-2',
        sender: 'assistant',
        content: 'Compliance updates state that all telemetry and document-sharing queues must run TLS 1.3 [4]. This prevents transit decryption during client extraction phases.',
        citations: ['4']
      }
    ]
  },
  {
    id: 'chat-3',
    title: 'Onboarding Guide Queries',
    time: '1:50 pm',
    selectedKb: 'kb-1',
    messages: [
      {
        id: 'ob-1',
        sender: 'user',
        content: 'Where is the employee benefits summary stored?'
      },
      {
        id: 'ob-2',
        sender: 'assistant',
        content: 'You can check the HR & Policy manual (Private archive) for full details on internal medical coverages and stock vestments.'
      }
    ]
  },
  {
    id: 'chat-4',
    title: 'Policy Document Review',
    time: '1:00 pm',
    selectedKb: 'kb-1',
    messages: [
      {
        id: 'pdr-1',
        sender: 'user',
        content: 'What is our doc ingestion strategy?'
      },
      {
        id: 'pdr-2',
        sender: 'assistant',
        content: 'Documents undergo deep parsing (OCR and text extraction) followed by dense vector embedding generations before loading.'
      }
    ]
  }
];

export const INITIAL_TELEMETRY_TASKS: TelemetryTask[] = [
  {
    id: 'task-1',
    stage: 'Parsing',
    documentName: 'Q4_Financial_Report.pdf',
    status: 'Completed',
    progress: 100,
    modelName: 'Llama-3-8B-Instruct',
    retryCount: 0,
    logs: [
      '[INFO] 07:01:05 - Initiating document parser socket.',
      '[INFO] 07:01:07 - PDF text streams located. Running high-precision OCR on scanned charts.',
      '[INFO] 07:01:10 - Structure recognition complete. Extracted 4,192 text nodes.',
      '[SUCCESS] 07:01:12 - Parsing phase completed successfully.'
    ]
  },
  {
    id: 'task-2',
    stage: 'Parsing',
    documentName: 'Sales_Strategy_Guide_2026.pdf',
    status: 'Completed',
    progress: 100,
    modelName: 'Llama-3-8B-Instruct',
    retryCount: 0,
    logs: [
      '[INFO] 07:01:40 - Found doc: Sales_Strategy_Guide_2026.pdf',
      '[INFO] 07:01:42 - Scanning layout nodes.',
      '[SUCCESS] 07:01:45 - Extraction done. Forwarding to Embedding service queue.'
    ]
  },
  {
    id: 'task-3',
    stage: 'Embedding',
    documentName: 'Engineering_Specs_v2.docx',
    status: 'Running',
    progress: 45,
    modelName: 'text-embedding-3-small',
    retryCount: 0,
    logs: [
      '[INFO] 07:02:10 - Embedding task received for v2 docx spec.',
      '[INFO] 07:02:15 - Chunking document into overlapping segments of 512 tokens.',
      '[INFO] 07:02:22 - Feeding chunks into text-embedding-3-small API client.',
      '[PROGRESS] 07:02:30 - Embedded 45% of total tokens...'
    ]
  },
  {
    id: 'task-4',
    stage: 'Parsing',
    documentName: 'Vendor_Contracts_A.pdf',
    status: 'Failed',
    progress: 10,
    modelName: 'Mistral-7B-v0.1',
    retryCount: 2,
    logs: [
      '[INFO] 06:12:00 - Parsing attempt #1: Corrupt PDF header detected.',
      '[WARN] 06:12:12 - Parsing failed. Re-initiating job queue in retry mode.',
      '[INFO] 06:14:00 - Parsing attempt #2: Corrupt PDF header block at chunk offset [0xFA3D].',
      '[ERROR] 06:14:05 - Command failed with non-zero exit status: DecryptionKeyMismatch. Execution aborted.'
    ]
  },
  {
    id: 'task-5',
    stage: 'Embedding',
    documentName: 'Product_Roadmap_2025.pptx',
    status: 'Pending',
    progress: 0,
    modelName: 'text-embedding-ada-002',
    retryCount: 0,
    logs: [
      '[WAITING] 07:44:00 - System queue full. Waiting for free GPU slot...'
    ]
  },
  {
    id: 'task-6',
    stage: 'Parsing',
    documentName: 'Product_Roadmap_2025.pptx',
    status: 'Pending',
    progress: 0,
    modelName: 'Llama-3-8B-Instruct',
    retryCount: 0,
    logs: [
      '[WAITING] 07:44:00 - Job queued. Waiting for parsing context.'
    ]
  }
];

export const INITIAL_TEAM_MEMBERS: TeamMember[] = [
  {
    id: 'm-1',
    username: 'john.doe',
    displayName: 'John Doe',
    roles: ['Admin', 'Content Editor'],
    status: 'Active'
  },
  {
    id: 'm-2',
    username: 'jane.smith',
    displayName: 'Jane Smith',
    roles: ['Viewer', 'Content Editor'],
    status: 'Active'
  },
  {
    id: 'm-3',
    username: 'robert.jones',
    displayName: 'Robert Jones',
    roles: ['Viewer'],
    status: 'Active'
  },
  {
    id: 'm-4',
    username: 'amy.nguyen',
    displayName: 'Amy Nguyen',
    roles: ['Admin'],
    status: 'Active'
  },
  {
    id: 'm-5',
    username: 'mike.brown',
    displayName: 'Mike Brown',
    roles: ['Content Editor'],
    status: 'Inactive'
  }
];
