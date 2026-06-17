package com.sunxin.knowledge.document.desensitization;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import com.sunxin.knowledge.document.dto.ParsedPageRequest;

@Service
@EnableConfigurationProperties(DesensitizationProperties.class)
public class RegexDesensitizer implements Desensitizer {

    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1[3-9]\\d{9})(?!\\d)");
    private static final Pattern EMAIL = Pattern.compile("([A-Za-z0-9._%+-])([A-Za-z0-9._%+-]*)(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)([1-9]\\d{5}(?:18|19|20)\\d{2}(?:0[1-9]|1[0-2])(?:0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx])(?!\\w)");
    private static final Pattern AMOUNT = Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d+)?)\\s*(人民币万元|万元|人民币元|万|元|￥)(?!\\w)");
    private static final Pattern CONTACT = Pattern.compile("(客户联系人|项目联系人|联系人|对接人)\\s*[：:]\\s*([\\u4e00-\\u9fa5]{2,4})");

    private final DesensitizationProperties properties;

    public RegexDesensitizer(DesensitizationProperties properties) {
        this.properties = properties;
    }

    @Override
    public DesensitizationResult desensitize(List<ParsedPageRequest> pages) {
        if (!properties.isEnabled() || pages == null || pages.isEmpty()) {
            return new DesensitizationResult(pages == null ? List.of() : pages, List.of());
        }

        List<ParsedPageRequest> maskedPages = new ArrayList<>();
        List<SensitiveMapping> mappings = new ArrayList<>();
        int[] occurrenceIndex = {0};
        for (ParsedPageRequest page : pages) {
            PageText current = new PageText(page.content(), mappings, page, occurrenceIndex);
            if (properties.getCustomerContact().isEnabled()) {
                current.replaceContacts();
            }
            if (properties.getCustomerName().isEnabled()) {
                current.replaceCustomerNames(customerNames(), customerReplacement());
            }
            if (properties.getPhone().isEnabled()) {
                current.replacePattern(PHONE, "PHONE", "phone", match -> maskPhone(match.group(1)));
            }
            if (properties.getEmail().isEnabled()) {
                current.replacePattern(EMAIL, "EMAIL", "email", RegexDesensitizer::maskEmail);
            }
            if (properties.getIdCard().isEnabled()) {
                current.replacePattern(ID_CARD, "ID_CARD", "id_card", match -> "身份证号：已脱敏");
            }
            if (properties.getAmount().isEnabled()) {
                current.replacePattern(AMOUNT, "AMOUNT", "amount", RegexDesensitizer::amountBucket);
            }

            maskedPages.add(new ParsedPageRequest(
                    page.pageNo(),
                    page.sectionTitle(),
                    page.contentType(),
                    current.value,
                    page.metadata()
            ));
        }
        return new DesensitizationResult(maskedPages, mappings);
    }

    private List<String> customerNames() {
        return properties.getCustomerNames().stream()
                .filter(name -> name != null && !name.isBlank())
                .sorted(Comparator.comparingInt(String::length).reversed())
                .toList();
    }

    private String customerReplacement() {
        String replacement = properties.getCustomerNameReplacement();
        return replacement == null || replacement.isBlank() ? "客户A" : replacement.trim();
    }

    private static String maskPhone(String phone) {
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private static String maskEmail(Matcher match) {
        return match.group(1) + "***" + match.group(3);
    }

    private static String amountBucket(Matcher match) {
        BigDecimal amount = new BigDecimal(match.group(1));
        String unit = match.group(2);
        BigDecimal amountInWan = unit.contains("元") && !unit.contains("万")
                ? amount.divide(BigDecimal.valueOf(10_000), 4, java.math.RoundingMode.HALF_UP)
                : amount;
        if (amountInWan.compareTo(BigDecimal.valueOf(100)) < 0) {
            return "金额区间：0-100万";
        }
        if (amountInWan.compareTo(BigDecimal.valueOf(300)) <= 0) {
            return "金额区间：100万-300万";
        }
        if (amountInWan.compareTo(BigDecimal.valueOf(1_000)) <= 0) {
            return "金额区间：300万-1000万";
        }
        return "金额区间：1000万以上";
    }

    private interface Replacement {
        String replace(Matcher match);
    }

    private static final class PageText {
        private String value;
        private final List<SensitiveMapping> mappings;
        private final ParsedPageRequest page;
        private final int[] occurrenceIndex;

        private PageText(
                String value,
                List<SensitiveMapping> mappings,
                ParsedPageRequest page,
                int[] occurrenceIndex
        ) {
            this.value = value == null ? "" : value;
            this.mappings = mappings;
            this.page = page;
            this.occurrenceIndex = occurrenceIndex;
        }

        private void replaceContacts() {
            Matcher matcher = CONTACT.matcher(value);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String originalName = matcher.group(2);
                String masked = "客户联系人A";
                mappings.add(mapping("CUSTOMER_CONTACT", originalName, masked, "customer_contact"));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(matcher.group(1) + "：" + masked));
            }
            matcher.appendTail(buffer);
            value = buffer.toString();
        }

        private void replaceCustomerNames(List<String> customerNames, String replacement) {
            for (String name : customerNames) {
                if (!value.contains(name)) {
                    continue;
                }
                value = value.replace(name, replacement);
                mappings.add(mapping("CUSTOMER_NAME", name, replacement, "customer_dictionary"));
            }
        }

        private void replacePattern(Pattern pattern, String sensitiveType, String ruleName, Replacement replacement) {
            Matcher matcher = pattern.matcher(value);
            StringBuffer buffer = new StringBuffer();
            while (matcher.find()) {
                String original = matcher.group();
                String masked = replacement.replace(matcher);
                mappings.add(mapping(sensitiveType, original, masked, ruleName));
                matcher.appendReplacement(buffer, Matcher.quoteReplacement(masked));
            }
            matcher.appendTail(buffer);
            value = buffer.toString();
        }

        private SensitiveMapping mapping(String sensitiveType, String original, String masked, String ruleName) {
            return new SensitiveMapping(
                    page.pageNo(),
                    page.sectionTitle(),
                    sensitiveType,
                    original,
                    masked,
                    ruleName,
                    occurrenceIndex[0]++
            );
        }
    }
}
