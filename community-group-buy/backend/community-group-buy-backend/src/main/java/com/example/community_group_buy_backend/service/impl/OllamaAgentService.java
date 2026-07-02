package com.example.community_group_buy_backend.service.impl;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OllamaAgentService {

    private static final Logger log = LoggerFactory.getLogger(OllamaAgentService.class);
    private static final String OLLAMA_URL = "http://localhost:11434/api/generate";
    private static final String MODEL = "qwen2.5:0.5b";
    private static final int TIMEOUT_MS = 15000;

    public List<String> merchantSuggestions(List<Map<String, Object>> salesStats,
                                            List<Map<String, Object>> inventoryStats) {
        StringBuilder data = new StringBuilder();
        data.append("商品销售数据：");
        for (Map<String, Object> s : salesStats) {
            data.append(String.format("[%s: 销量%s件, 销售额%.2f元] ",
                    s.get("product_name"),
                    s.get("total_quantity"),
                    ((Number) s.getOrDefault("total_amount", 0)).doubleValue()));
        }
        data.append("。库存数据：");
        for (Map<String, Object> inv : inventoryStats) {
            data.append(String.format("[%s: 库存%s件, 已售%s件] ",
                    inv.get("product_name"),
                    inv.get("stock"),
                    inv.get("sales_count")));
        }

        String prompt = String.format(
                "你是一个社区团购经营顾问。根据以下数据给出3条简洁的经营建议，每条不超过30字，用数字序号列出：\n%s",
                data.toString());

        return callOllama(prompt);
    }

    public List<String> adminSuggestions(Map<String, Object> overview,
                                          List<Map<String, Object>> salesStats,
                                          List<Map<String, Object>> userStats) {
        StringBuilder data = new StringBuilder();
        data.append(String.format("平台总览：总交易额%.2f元, 总订单%s笔, 付费用户%s人。",
                ((Number) overview.getOrDefault("total_revenue", 0)).doubleValue(),
                overview.get("order_count"),
                overview.get("user_count")));

        data.append("商品销售排行：");
        int count = 0;
        for (Map<String, Object> s : salesStats) {
            if (count++ >= 5) break;
            data.append(String.format("[%s: 销量%s件, 销售额%.2f元] ",
                    s.get("product_name"),
                    s.get("total_quantity"),
                    ((Number) s.getOrDefault("total_amount", 0)).doubleValue()));
        }

        data.append("。用户购买排行：");
        count = 0;
        for (Map<String, Object> u : userStats) {
            if (count++ >= 5) break;
            data.append(String.format("[%s: 消费%.2f元, %s笔] ",
                    u.get("username"),
                    ((Number) u.getOrDefault("total_amount", 0)).doubleValue(),
                    u.get("order_count")));
        }

        String prompt = String.format(
                "你是一个社区团购平台运营顾问。根据以下数据给出3条简洁的平台运营建议，每条不超过30字，用数字序号列出：\n%s",
                data.toString());

        return callOllama(prompt);
    }

    private List<String> callOllama(String prompt) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URI(OLLAMA_URL).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(TIMEOUT_MS);
            conn.setReadTimeout(TIMEOUT_MS);

            String body = String.format("{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
                    MODEL, escapeJson(prompt));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            if (conn.getResponseCode() != 200) {
                log.warn("Ollama returned status {}", conn.getResponseCode());
                return null;
            }

            String response = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            // Parse JSON response: {"response": "..."}
            String responseField = extractJsonField(response, "response");
            if (responseField == null || responseField.isBlank()) {
                return null;
            }

            // Split by numbered lines
            return parseNumberedLines(responseField);
        } catch (Exception e) {
            log.warn("Ollama call failed: {}", e.getMessage());
            return null;
        }
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String extractJsonField(String json, String field) {
        String key = "\"" + field + "\":\"";
        int start = json.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '\\' && i + 1 < json.length()) {
                char next = json.charAt(i + 1);
                if (next == 'n') { sb.append('\n'); i++; }
                else if (next == 'r') { sb.append('\r'); i++; }
                else if (next == 't') { sb.append('\t'); i++; }
                else if (next == '"') { sb.append('"'); i++; }
                else if (next == '\\') { sb.append('\\'); i++; }
                else { sb.append(c); }
                continue;
            }
            if (c == '"') break;
            sb.append(c);
        }
        return sb.toString();
    }

    private List<String> parseNumberedLines(String text) {
        List<String> suggestions = new ArrayList<>();
        for (String line : text.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            // Strip leading number like "1." or "1、"
            String cleaned = trimmed.replaceFirst("^\\d+[\\.、]\\s*", "");
            if (!cleaned.isEmpty() && cleaned.length() >= 4) {
                suggestions.add(cleaned);
            }
        }
        return suggestions;
    }
}
