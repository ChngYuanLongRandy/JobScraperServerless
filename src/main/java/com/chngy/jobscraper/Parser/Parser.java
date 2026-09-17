package com.chngy.jobscraper.Parser;

import com.chngy.jobscraper.DTO.Response.GovSearchResponseJobPost;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class Parser {

    // Delimiters bounding each JS string-literal argument in a
    // self.__next_f.push([1,"..."]) call Next.js emits. Located and scanned
    // with plain indexOf()/charAt() rather than a regex — a real page's
    // single push() chunk can run over a megabyte with 100k+ backslash
    // escapes, and Java's regex engine recurses one stack frame per
    // alternation-group repetition, which throws StackOverflowError at that
    // scale even with an "unrolled" (?:[^"\\]*(?:\\.[^"\\]*)*) pattern.
    private static final String RSC_PUSH_PREFIX = "self.__next_f.push([1,\"";
    private static final String RSC_PUSH_SUFFIX = "\"])";

    ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Reconstructs the RSC buffer from every self.__next_f.push() call in the
     * raw HTML, then extracts and parses the "jobs":[...] array as JSON.
     */
    public List<GovSearchResponseJobPost> extractJobsFromRsc(String rawHtml) {
        String buffer = reconstructRscBuffer(rawHtml);
        String jobsJson = extractBalancedArray(buffer, "jobs");
        // Next.js's RSC ("Flight") wire format serializes JS `undefined` field
        // values as the literal string token "$undefined" rather than omitting
        // the field or using JSON null — e.g. postings with no experience
        // level come through as "experienceLevels":"$undefined". Since it's
        // a fixed Flight-protocol sigil (never legitimate job content),
        // normalize it to JSON null before parsing so it binds correctly
        // against typed fields like List<String> instead of throwing.
        jobsJson = jobsJson.replace("\"$undefined\"", "null");

        try {
            JsonNode jobsArray = objectMapper.readTree(jobsJson);
            List<GovSearchResponseJobPost> jobs = new ArrayList<>();
            jobsArray.forEach(job-> jobs.add(objectMapper.convertValue(job, GovSearchResponseJobPost.class)));
            return jobs;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse extracted jobs JSON from RSC payload", e);
        }
    }

    /** Pulls every push() chunk in document order, decodes JS escapes, concatenates. */
    private String reconstructRscBuffer(String rawHtml) {
        StringBuilder buffer = new StringBuilder();
        int searchFrom = 0;
        while (true) {
            int prefixStart = rawHtml.indexOf(RSC_PUSH_PREFIX, searchFrom);
            if (prefixStart == -1) {
                break;
            }
            int contentStart = prefixStart + RSC_PUSH_PREFIX.length();
            int contentEnd = findUnescapedQuote(rawHtml, contentStart);
            if (contentEnd == -1) {
                break; // unterminated string literal — malformed/truncated page
            }
            if (rawHtml.startsWith(RSC_PUSH_SUFFIX, contentEnd)) {
                buffer.append(decodeJsStringLiteral(rawHtml.substring(contentStart, contentEnd)));
                searchFrom = contentEnd + RSC_PUSH_SUFFIX.length();
            } else {
                searchFrom = contentEnd + 1;
            }
        }
        if (buffer.isEmpty()) {
            throw new IllegalStateException("No self.__next_f.push() chunks found — page structure may have changed");
        }
        return buffer.toString();
    }

    /** Scans forward from {@code from}, skipping \-escaped characters, for the closing unescaped '"'. */
    private int findUnescapedQuote(String s, int from) {
        int i = from;
        int len = s.length();
        while (i < len) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
            } else if (c == '"') {
                return i;
            } else {
                i++;
            }
        }
        return -1;
    }

    /** Decodes JS string-literal escape sequences (\n, \", \\, \/, \XXXX) into real characters. */
    private String decodeJsStringLiteral(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case 'n' -> { out.append('\n'); i++; }
                    case 't' -> { out.append('\t'); i++; }
                    case 'r' -> { out.append('\r'); i++; }
                    case '"' -> { out.append('"'); i++; }
                    case '\\' -> { out.append('\\'); i++; }
                    case '/' -> { out.append('/'); i++; }
                    case 'u' -> {
                        String hex = s.substring(i + 2, i + 6);
                        out.append((char) Integer.parseInt(hex, 16));
                        i += 5;
                    }
                    default -> { out.append(next); i++; }
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Finds "<key>":[ in the buffer and returns everything up to its matching
     * close bracket, tracking quoted-string state so [ / ] characters inside
     * job titles or descriptions don't throw off the depth count.
     */
    private String extractBalancedArray(String buf, String key) {
        String marker = "\"" + key + "\":[";
        int idx = buf.indexOf(marker);
        if (idx == -1) {
            throw new IllegalStateException("Key not found in reconstructed RSC buffer: " + key);
        }

        int start = idx + marker.length() - 1; // include the opening '['
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        int i = start;

        for (; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
            } else {
                if (c == '"') {
                    inString = true;
                } else if (c == '[') {
                    depth++;
                } else if (c == ']') {
                    depth--;
                    if (depth == 0) {
                        i++;
                        break;
                    }
                }
            }
        }
        return buf.substring(start, i);
    }

}
