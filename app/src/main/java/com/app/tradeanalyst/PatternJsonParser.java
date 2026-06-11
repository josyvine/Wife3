package com.tradeanalyst.app;

import android.util.Log;
import com.google.gson.Gson;

/**
 * PARSER UTILITY: PatternJsonParser
 * Safely sanitizes and deserializes the structured JSON response into a ChartPatternResponse object.
 */
public class PatternJsonParser {

    private static final String TAG = "PatternJsonParser";

    /**
     * Sanitizes the incoming payload, strips any surrounding markdown markers,
     * isolates the root JSON brackets, and parses it into a ChartPatternResponse.
     *
     * @param rawJson The raw payload containing the JSON string.
     * @return Deserialized ChartPatternResponse, or null if deserialization fails.
     */
    public static ChartPatternResponse parse(String rawJson) {
        if (rawJson == null || rawJson.trim().isEmpty()) {
            Log.e(TAG, "Empty JSON payload passed to parser.");
            return null;
        }

        // Step 1: Clean markdown block wrappers and isolate root curly braces
        String cleaned = cleanJsonString(rawJson);

        // Step 2: Deserialize using Gson
        try {
            Gson gson = new Gson();
            ChartPatternResponse response = gson.fromJson(cleaned, ChartPatternResponse.class);
            if (response != null) {
                Log.d(TAG, "Successfully deserialized pattern response. Patterns found: " 
                    + (response.getPatterns() != null ? response.getPatterns().size() : 0));
            }
            return response;
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse cleaned JSON: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Identifies and strips common markdown formatting blocks (such as ```json and ```),
     * then isolates the string content between the first opening brace '{' and the last closing brace '}'.
     *
     * @param raw The raw string.
     * @return A sanitized, brace-enclosed JSON string.
     */
    private static String cleanJsonString(String raw) {
        String cleaned = raw.trim();

        // Step 1: Defensively strip "PATTERN:" prefix and any preamble before it
        if (cleaned.contains("PATTERN:")) {
            int patternIndex = cleaned.indexOf("PATTERN:");
            cleaned = cleaned.substring(patternIndex + 8).trim();
        }

        // Step 2: Strip ```json code block wrapper
        if (cleaned.contains("```json")) {
            int startIndex = cleaned.indexOf("```json") + 7;
            int endIndex = cleaned.lastIndexOf("```");
            if (endIndex > startIndex) {
                cleaned = cleaned.substring(startIndex, endIndex).trim();
            } else {
                cleaned = cleaned.substring(startIndex).trim();
            }
        } else if (cleaned.contains("```")) {
            // Strip general ``` block wrapper
            int startIndex = cleaned.indexOf("```") + 3;
            int endIndex = cleaned.lastIndexOf("```");
            if (endIndex > startIndex) {
                cleaned = cleaned.substring(startIndex, endIndex).trim();
            } else {
                cleaned = cleaned.substring(startIndex).trim();
            }
        }

        // Step 3: Isolate content between the first '{' and the last '}' to strip surrounding conversational text
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }

        return cleaned;
    }
}