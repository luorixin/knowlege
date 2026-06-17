package com.sunxin.knowledge.document.desensitization;

import java.util.List;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

public interface Desensitizer {

    DesensitizationResult desensitize(List<ParsedPageRequest> pages);
}
