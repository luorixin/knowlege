package com.sunxin.knowledge.eval.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunxin.knowledge.auth.AccessControlService;
import com.sunxin.knowledge.auth.CurrentUser;
import com.sunxin.knowledge.common.error.BadRequestException;
import com.sunxin.knowledge.common.error.NotFoundException;
import com.sunxin.knowledge.common.id.IdGenerator;
import com.sunxin.knowledge.eval.dto.EvalCaseCreateRequest;
import com.sunxin.knowledge.eval.dto.EvalCaseReportResponse;
import com.sunxin.knowledge.eval.dto.EvalCaseResponse;
import com.sunxin.knowledge.eval.dto.EvalCaseSpec;
import com.sunxin.knowledge.eval.dto.EvalDatasetCreateRequest;
import com.sunxin.knowledge.eval.dto.EvalDatasetResponse;
import com.sunxin.knowledge.eval.dto.EvalMetricsResponse;
import com.sunxin.knowledge.eval.dto.EvalRunRequest;
import com.sunxin.knowledge.eval.dto.EvalRunResponse;
import com.sunxin.knowledge.persistence.entity.KbDocument;
import com.sunxin.knowledge.persistence.entity.KbDocumentChunk;
import com.sunxin.knowledge.persistence.entity.KbEvalCase;
import com.sunxin.knowledge.persistence.entity.KbEvalDataset;
import com.sunxin.knowledge.persistence.entity.KbEvalResult;
import com.sunxin.knowledge.persistence.entity.KbSpace;
import com.sunxin.knowledge.persistence.repository.KbDocumentChunkRepository;
import com.sunxin.knowledge.persistence.repository.KbDocumentRepository;
import com.sunxin.knowledge.persistence.repository.KbEvalCaseRepository;
import com.sunxin.knowledge.persistence.repository.KbEvalDatasetRepository;
import com.sunxin.knowledge.persistence.repository.KbEvalResultRepository;
import com.sunxin.knowledge.persistence.repository.KbSpaceRepository;
import com.sunxin.knowledge.qa.dto.AgentChatResponse;
import com.sunxin.knowledge.qa.dto.AgentCitationResponse;
import com.sunxin.knowledge.retrieval.application.RetrievalSearchService;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchRequest;
import com.sunxin.knowledge.retrieval.dto.RetrievalSearchResponse;

@Service
public class EvalService {

    private static final String ACTIVE = "ACTIVE";
    private static final String COMPLETED = "COMPLETED";
    private static final int DEFAULT_TOP_K = 10;

    private final KbEvalDatasetRepository datasetRepository;
    private final KbEvalCaseRepository caseRepository;
    private final KbEvalResultRepository resultRepository;
    private final KbSpaceRepository spaceRepository;
    private final KbDocumentRepository documentRepository;
    private final KbDocumentChunkRepository chunkRepository;
    private final RetrievalSearchService retrievalSearchService;
    private final EvalAnswerClient answerClient;
    private final RuleBasedRagEvaluator evaluator;
    private final RuleBasedParserEvaluator parserEvaluator;
    private final EvalReportAggregator aggregator;
    private final AccessControlService accessControlService;
    private final IdGenerator idGenerator;
    private final ObjectMapper objectMapper;

    public EvalService(
            KbEvalDatasetRepository datasetRepository,
            KbEvalCaseRepository caseRepository,
            KbEvalResultRepository resultRepository,
            KbSpaceRepository spaceRepository,
            KbDocumentRepository documentRepository,
            KbDocumentChunkRepository chunkRepository,
            RetrievalSearchService retrievalSearchService,
            EvalAnswerClient answerClient,
            RuleBasedRagEvaluator evaluator,
            RuleBasedParserEvaluator parserEvaluator,
            EvalReportAggregator aggregator,
            AccessControlService accessControlService,
            IdGenerator idGenerator,
            ObjectMapper objectMapper
    ) {
        this.datasetRepository = datasetRepository;
        this.caseRepository = caseRepository;
        this.resultRepository = resultRepository;
        this.spaceRepository = spaceRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.retrievalSearchService = retrievalSearchService;
        this.answerClient = answerClient;
        this.evaluator = evaluator;
        this.parserEvaluator = parserEvaluator;
        this.aggregator = aggregator;
        this.accessControlService = accessControlService;
        this.idGenerator = idGenerator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EvalDatasetResponse createDataset(EvalDatasetCreateRequest request, CurrentUser user) {
        KbSpace space = requireSpace(request.spaceId());
        validateTenant(user, request.tenantId(), space.getTenantId());

        KbEvalDataset dataset = new KbEvalDataset();
        dataset.setId(idGenerator.nextId());
        dataset.setTenantId(space.getTenantId());
        dataset.setSpaceId(space.getId());
        dataset.setName(request.name().trim());
        dataset.setDescription(blankToNull(request.description()));
        dataset.setStatus(ACTIVE);
        dataset.setCreatedBy(user.userId());
        dataset.setUpdatedBy(user.userId());
        return EvalDatasetResponse.fromEntity(datasetRepository.save(dataset));
    }

    @Transactional(readOnly = true)
    public List<EvalDatasetResponse> listDatasets(CurrentUser user) {
        return datasetRepository.findByTenantIdAndStatusOrderByCreatedAtDesc(user.tenantId(), ACTIVE)
                .stream()
                .map(EvalDatasetResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EvalCaseResponse> listCases(Long datasetId, CurrentUser user) {
        KbEvalDataset dataset = requireDataset(datasetId);
        validateTenant(user, dataset.getTenantId(), dataset.getTenantId());
        return caseRepository.findByDatasetIdAndStatusOrderByCreatedAtAsc(datasetId, ACTIVE)
                .stream()
                .map(c -> EvalCaseResponse.fromEntity(c, spec(c)))
                .toList();
    }

    @Transactional
    public EvalCaseResponse createCase(EvalCaseCreateRequest request, CurrentUser user) {
        KbEvalDataset dataset = requireDataset(request.datasetId());
        validateTenant(user, dataset.getTenantId(), dataset.getTenantId());
        EvalCaseSpec spec = new EvalCaseSpec(
                safe(request.expectedDocIds()),
                safe(request.expectedChunkIds()),
                Boolean.TRUE.equals(request.expectNoAnswer()),
                request.filters(),
                request.tags() == null ? List.of() : request.tags(),
                null,
                null,
                null
        );

        KbEvalCase evalCase = new KbEvalCase();
        evalCase.setId(idGenerator.nextId());
        evalCase.setTenantId(dataset.getTenantId());
        evalCase.setDatasetId(dataset.getId());
        evalCase.setCaseType(request.caseType() != null ? request.caseType() : "QA_RAG");
        evalCase.setQuestion(request.question().trim());
        evalCase.setExpectedAnswer(blankToNull(request.expectedAnswer()));
        evalCase.setExpectedDocIds(joinIds(spec.expectedDocIds()));
        evalCase.setTags(spec.tags().isEmpty() ? null : String.join(",", spec.tags()));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("expected_chunk_ids", spec.expectedChunkIds());
        metadata.put("expect_no_answer", spec.expectNoAnswer());
        metadata.put("filters", spec.filters());
        metadata.put("tags", spec.tags());
        evalCase.setMetadataJson(toJson(metadata));
        evalCase.setStatus(ACTIVE);
        evalCase.setCreatedBy(user.userId());
        evalCase.setUpdatedBy(user.userId());
        return EvalCaseResponse.fromEntity(caseRepository.save(evalCase), spec);
    }

    @Transactional
    public EvalRunResponse run(EvalRunRequest request, CurrentUser user) {
        KbEvalDataset dataset = requireDataset(request.datasetId());
        KbSpace space = requireSpace(dataset.getSpaceId());
        CurrentUser resolvedUser = resolveUser(user, dataset.getTenantId());
        int topK = request.topK() == null || request.topK() <= 0 ? DEFAULT_TOP_K : request.topK();
        String runId = "eval-" + idGenerator.nextId();

        List<KbEvalCase> cases = caseRepository.findByDatasetIdAndStatusOrderByCreatedAtAsc(dataset.getId(), ACTIVE);
        List<EvalCaseReportResponse> reports = new ArrayList<>();
        for (KbEvalCase evalCase : cases) {
            EvalCaseSpec spec = spec(evalCase);
            
            if ("PARSER_QUALITY".equals(evalCase.getCaseType())) {
                EvalCaseReportResponse report = parserEvaluator.evaluate(evalCase.getId(), evalCase.getQuestion(), spec, resolvedUser);
                reports.add(report);
                saveResult(dataset, evalCase, runId, null, report, "PARSER_QUALITY");
            } else {
                RetrievalSearchResponse retrieval = retrievalSearchService.search(
                        new RetrievalSearchRequest(evalCase.getQuestion(), space.getId(), spec.filters(), topK, java.util.List.of()),
                        resolvedUser
                );
                AgentChatResponse answer = answerClient.answer(space.getId(), evalCase.getQuestion(), spec, resolvedUser);
                int inaccessibleExpectedTargets = inaccessibleExpectedTargets(spec, resolvedUser);
                int unauthorizedCitations = unauthorizedCitations(answer, resolvedUser);
                int unauthorizedRetrieved = unauthorizedRetrieved(retrieval, resolvedUser);
                EvalCaseReportResponse report = evaluator.evaluate(
                        evalCase.getId(),
                        evalCase.getQuestion(),
                        spec,
                        retrieval,
                        answer,
                        inaccessibleExpectedTargets,
                        unauthorizedCitations,
                        unauthorizedRetrieved
                );
                reports.add(report);
                saveResult(dataset, evalCase, runId, answer, report, "RULE_BASED");
            }
        }

        EvalMetricsResponse metrics = aggregator.aggregate(reports);
        return new EvalRunResponse(runId, dataset.getId(), reports.size(), metrics, reports);
    }

    @Transactional(readOnly = true)
    public EvalRunResponse result(String runId, CurrentUser user) {
        List<KbEvalResult> rows = resultRepository.findByRunIdOrderByCreatedAtAsc(runId);
        if (rows.isEmpty()) {
            throw new NotFoundException("Evaluation result not found");
        }
        Long datasetId = rows.get(0).getDatasetId();
        KbEvalDataset dataset = requireDataset(datasetId);
        validateTenant(user, dataset.getTenantId(), dataset.getTenantId());
        List<EvalCaseReportResponse> reports = rows.stream()
                .map(row -> fromJson(row.getDetailJson(), EvalCaseReportResponse.class))
                .toList();
        return new EvalRunResponse(runId, datasetId, reports.size(), aggregator.aggregate(reports), reports);
    }

    private void saveResult(
            KbEvalDataset dataset,
            KbEvalCase evalCase,
            String runId,
            AgentChatResponse answer,
            EvalCaseReportResponse report,
            String evaluatorType
    ) {
        KbEvalResult result = new KbEvalResult();
        result.setId(idGenerator.nextId());
        result.setTenantId(dataset.getTenantId());
        result.setDatasetId(dataset.getId());
        result.setCaseId(evalCase.getId());
        result.setRunId(runId);
        result.setQuerySessionId(answer != null ? answer.sessionId() : null);
        result.setActualAnswer(answer != null ? answer.answer() : null);
        result.setScore(BigDecimal.valueOf(report.recallHit() ? 1.0 : 0.0));
        result.setHitCount(report.recallHit() ? 1 : 0);
        result.setCitationHitCount(report.citationAccuracy() > 0 ? 1 : 0);
        result.setEvaluatorType(evaluatorType);
        result.setEvaluatorModel("mvp-rule-evaluator");
        result.setStatus(COMPLETED);
        result.setDetailJson(toJson(report));
        resultRepository.save(result);
    }

    private int inaccessibleExpectedTargets(EvalCaseSpec spec, CurrentUser user) {
        Set<Long> expectedDocIds = new java.util.HashSet<>(safe(spec.expectedDocIds()));
        for (Long chunkId : safe(spec.expectedChunkIds())) {
            chunkRepository.findById(chunkId)
                    .map(KbDocumentChunk::getDocId)
                    .ifPresent(expectedDocIds::add);
        }
        int count = 0;
        List<KbDocument> documents = documentRepository.findAllById(expectedDocIds);
        for (KbDocument document : documents) {
            if (!accessControlService.canAccessDocument(document, user, "document_read")) {
                count++;
            }
        }
        return count;
    }

    private int unauthorizedCitations(AgentChatResponse answer, CurrentUser user) {
        if (answer == null || answer.citations() == null) {
            return 0;
        }
        int count = 0;
        Set<Long> docIds = answer.citations().stream().map(AgentCitationResponse::docId).collect(java.util.stream.Collectors.toSet());
        List<KbDocument> documents = documentRepository.findAllById(docIds);
        for (KbDocument document : documents) {
            if (!accessControlService.canAccessDocument(document, user, "document_read")) {
                count++;
            }
        }
        return count;
    }

    private int unauthorizedRetrieved(RetrievalSearchResponse retrieval, CurrentUser user) {
        if (retrieval == null || retrieval.results() == null) {
            return 0;
        }
        Set<Long> docIds = retrieval.results().stream()
                .map(com.sunxin.knowledge.retrieval.dto.RetrievalSearchResult::docId)
                .collect(java.util.stream.Collectors.toSet());
        int count = 0;
        List<KbDocument> documents = documentRepository.findAllById(docIds);
        for (KbDocument document : documents) {
            if (!accessControlService.canAccessDocument(document, user, "document_read")) {
                count++;
            }
        }
        return count;
    }

    private KbEvalDataset requireDataset(Long datasetId) {
        return datasetRepository.findByIdAndStatus(datasetId, ACTIVE)
                .orElseThrow(() -> new NotFoundException("Evaluation dataset not found"));
    }

    private KbSpace requireSpace(Long spaceId) {
        return spaceRepository.findByIdAndStatus(spaceId, ACTIVE)
                .orElseThrow(() -> new NotFoundException("Knowledge space not found"));
    }

    private CurrentUser resolveUser(CurrentUser user, Long tenantId) {
        CurrentUser currentUser = user == null ? new CurrentUser(0L, tenantId, Set.of()) : user;
        validateTenant(currentUser, tenantId, tenantId);
        return currentUser.withTenant(tenantId);
    }

    private void validateTenant(CurrentUser user, Long requestTenantId, Long resourceTenantId) {
        if (requestTenantId != null && resourceTenantId != null && !requestTenantId.equals(resourceTenantId)) {
            throw new BadRequestException("Tenant does not match resource");
        }
        if (user != null && user.tenantId() != null && resourceTenantId != null && !user.tenantId().equals(resourceTenantId)) {
            throw new BadRequestException("X-Tenant-Id does not match requested resource");
        }
    }

    private EvalCaseSpec spec(KbEvalCase evalCase) {
        Map<String, Object> metadata = metadata(evalCase.getMetadataJson());
        return new EvalCaseSpec(
                parseIds(evalCase.getExpectedDocIds()),
                objectMapper.convertValue(
                        metadata.getOrDefault("expected_chunk_ids", List.of()),
                        new TypeReference<List<Long>>() {
                        }
                ),
                Boolean.TRUE.equals(metadata.get("expect_no_answer")),
                objectMapper.convertValue(metadata.get("filters"), com.sunxin.knowledge.retrieval.dto.SearchFilters.class),
                objectMapper.convertValue(
                        metadata.getOrDefault("tags", List.of()),
                        new TypeReference<List<String>>() {
                        }
                ),
                metadata.get("expected_block_count") != null ? ((Number) metadata.get("expected_block_count")).intValue() : null,
                metadata.get("expected_table_count") != null ? ((Number) metadata.get("expected_table_count")).intValue() : null,
                metadata.get("expected_error_count") != null ? ((Number) metadata.get("expected_error_count")).intValue() : null
        );
    }

    private Map<String, Object> metadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        return fromJson(metadataJson, new TypeReference<Map<String, Object>>() {
        });
    }

    private List<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : value.split(",")) {
            if (!part.isBlank()) {
                ids.add(Long.valueOf(part.trim()));
            }
        }
        return ids;
    }

    private String joinIds(List<Long> ids) {
        return ids == null || ids.isEmpty()
                ? null
                : ids.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","));
    }

    private <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Evaluation report cannot be serialized");
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Evaluation report cannot be parsed");
        }
    }

    private <T> T fromJson(String value, TypeReference<T> type) {
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new BadRequestException("Evaluation metadata cannot be parsed");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
