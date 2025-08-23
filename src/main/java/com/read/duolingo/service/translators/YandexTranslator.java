package com.read.duolingo.service.translators;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.read.duolingo.enums.TranslatorType;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class YandexTranslator {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static List<String> translateSingleTextForService(String text, String lang, String service) throws Exception {
        URL url = new URL("https://translate.toil.cc/v2/translate/");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String requestBody = String.format("{\"lang\":\"%s\",\"service\":\"%s\",\"text\":\"%s\"}",
                escapeJson(lang), escapeJson(service), escapeJson(text));

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode != 200) {
            StringBuilder errorResponse = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorResponse.append(line);
                }
            }
            throw new Exception(String.format("%s failed with status %d\n%s\n%s\n%s",
                    service, responseCode, text.length(), requestBody, errorResponse.toString()));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        JsonNode data = objectMapper.readTree(response.toString());
        if (data != null && data.has("translations") && data.get("translations").isArray()) {
            List<String> translations = new ArrayList<>();
            for (JsonNode translationNode : data.get("translations")) {
                translations.add(translationNode.asText());
            }
            return translations;
        } else {
            // fallback: return original texts if translation failed
            return Arrays.asList(text);
        }
    }

    public static List<String> translate(List<String> texts, String sourceLang, String targetLang) throws Exception {
        if (texts.isEmpty()) {
            return new ArrayList<>();
        }

        String service = "yandexgpt";

        // Yandex does not accept "auto" language
        String source_lang = sourceLang.equals("AUTO") ? "en" : normalizeToShortLang(sourceLang).toLowerCase();
        String target_lang = normalizeToShortLang(targetLang).toLowerCase();
        String lang = source_lang + "-" + target_lang;

        List<String> translatedTexts = new ArrayList<>();

        for (String text : texts) {
            List<String> translations = translateSingleTextForService(text, lang, service);
            translatedTexts.addAll(translations);
        }

        return translatedTexts;
    }

    private static String normalizeToShortLang(String lang) {
        // Simplified implementation - in a real app, you would have a complete mapping
        if (lang.contains("-")) {
            return lang.split("-")[0];
        }
        return lang;
    }

    private static String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // For testing
    public static void main(String[] args) {
        try {
            List<String> texts = Arrays.asList("Hello, world!", "How are you?");
            List<String> translations = translate(texts, "en", "zh");
            System.out.println("Translations: " + translations);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}