package com.example.mysoftpos_backend.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks PAN/Track2-like fragments before storing request/response hex payloads.
 */
@Service
public class SensitiveDataMaskingService {

    private static final Pattern TRACK2 = Pattern.compile("(\\d{13,19})=(\\d{4,})");
    private static final Pattern PAN = Pattern.compile("\\b(\\d{13,19})\\b");
    private static final Pattern HEX_PAN = Pattern.compile("([0-9A-Fa-f]{26,38})");

    public String maskIsoHex(String raw) {
        if (raw == null || raw.isBlank()) {
            return raw;
        }
        String masked = replace(TRACK2, raw, m -> maskPan(m.group(1)) + "=****");
        masked = replace(PAN, masked, m -> maskPan(m.group(1)));
        masked = replace(HEX_PAN, masked, m -> maskHexPan(m.group(1)));
        return masked;
    }

    private String maskHexPan(String value) {
        if (value == null || value.length() < 20) {
            return value;
        }
        int maskedLen = Math.max(2, value.length() - 20);
        return value.substring(0, 12) + "*".repeat(maskedLen) + value.substring(value.length() - 8);
    }

    private String maskPan(String value) {
        if (value == null || value.length() < 13) {
            return value;
        }
        int maskedLen = Math.max(1, value.length() - 10);
        return value.substring(0, 6) + "*".repeat(maskedLen) + value.substring(value.length() - 4);
    }

    private String replace(Pattern pattern, String input, Replacer replacer) {
        Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        int last = 0;
        while (matcher.find()) {
            sb.append(input, last, matcher.start());
            sb.append(replacer.replace(matcher));
            last = matcher.end();
        }
        sb.append(input, last, input.length());
        return sb.toString();
    }

    private interface Replacer {
        String replace(Matcher m);
    }
}

