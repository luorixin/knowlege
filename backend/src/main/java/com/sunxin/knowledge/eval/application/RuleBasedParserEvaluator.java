package com.sunxin.knowledge.eval.application;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.eval.dto.EvalCaseReportResponse;
import com.sunxin.knowledge.eval.dto.EvalCaseSpec;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;

@Component
public class RuleBasedParserEvaluator {

    private final KbDocumentChunkRepository chunkRepository;

    public RuleBasedParserEvaluator(KbDocumentChunkRepository chunkRepository) {
        this.chunkRepository = chunkRepository;
    }

    public EvalCaseReportResponse evaluate(
            Long caseId,
            String question,
            EvalCaseSpec spec,
            CurrentUser user
    ) {
        List<Long> expectedDocIds = spec.expectedDocIds() == null ? List.of() : spec.expectedDocIds();
        if (expectedDocIds.isEmpty()) {
            return new EvalCaseReportResponse(
                    caseId,
                    question,
                    false,
                    "No expected document ID provided for parser evaluation",
                    List.of(),
                    List.of(),
                    List.of(),
                    false,
                    0.0,
                    false,
                    0,
                    0,
                    0,
                    false,
                    0.0,
                    0.0,
                    0.0,
                    ""
            );
        }

        Long docId = expectedDocIds.get(0);
        List<KbDocumentChunk> chunks = chunkRepository.findByDocId(docId);

        int totalBlocks = chunks.size();
        int tableBlocks = 0;
        int errorBlocks = 0; // If any chunk has error metadata or we define it

        for (KbDocumentChunk chunk : chunks) {
            String metadata = chunk.getMetadataJson();
            if (metadata != null && metadata.contains("\"block_type\":\"table\"")) {
                tableBlocks++;
            }
            if (metadata != null && (metadata.contains("\"error\"") || metadata.contains("\"error_code\""))) {
                errorBlocks++;
            }
        }

        boolean pass = true;
        if (spec.expectedBlockCount() != null && totalBlocks != spec.expectedBlockCount()) {
            pass = false;
        }
        if (spec.expectedTableCount() != null && tableBlocks < spec.expectedTableCount()) {
            pass = false;
        }
        if (spec.expectedErrorCount() != null && errorBlocks > spec.expectedErrorCount()) {
            pass = false;
        }

        String answer = String.format("Parser Evaluation: Blocks=%d (Expected %s), Tables=%d (Expected %s), Errors=%d (Expected %s). Result: %s",
                totalBlocks, spec.expectedBlockCount(),
                tableBlocks, spec.expectedTableCount(),
                errorBlocks, spec.expectedErrorCount(),
                pass ? "PASS" : "FAIL"
        );

        return new EvalCaseReportResponse(
                caseId,
                question,
                false,
                answer,
                chunks.stream().map(KbDocumentChunk::getId).toList(),
                expectedDocIds,
                expectedDocIds,
                pass, // Use recallHit as the pass/fail boolean indicator for the overall score
                pass ? 1.0 : 0.0, // Use citationAccuracy as a numeric score (0.0 or 1.0)
                false,
                0,
                0,
                0,
                false,
                pass ? 1.0 : 0.0,
                0.0,
                0.0,
                ""
        );
    }
}
