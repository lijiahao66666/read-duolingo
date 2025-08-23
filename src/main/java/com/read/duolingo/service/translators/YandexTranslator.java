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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.read.duolingo.enums.TranslatorType;
import com.read.duolingo.utils.LanguageUtil;
import com.read.duolingo.utils.StringUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class YandexTranslator{
    private final RestTemplate restTemplate = new RestTemplate();

    private String queryYandex(String source, String targetLang) throws Exception {
        String url = "https://translate.toil.cc/v2/translate/";
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("service", "yandexgpt");
        formData.add("text", StringUtil.escapeJson(source));
        formData.add("lang", LanguageUtil.detectLanguage(source) + "-" + targetLang);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(formData, headers);
        Object o = restTemplate.postForObject(url, entity, Object.class);
        log.info("YandexTranslator queryYandex, source: {}, targetLang: {}, response: {}", source, targetLang, o);
        return o.toString();
    }

    // For testing
    public static void main(String[] args) {
        try {
            String translation = new YandexTranslator().queryYandex("Hello, world!", "zh");
            System.out.println("Translation: " + translation);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}