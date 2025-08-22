package com.read.duolingo.utils;

import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

public class LanguageUtil {
    private static  LanguageDetector detector = null;
    public static String detectLanguage(String text) throws Exception {
        if(detector == null){
            detector = LanguageDetector.getDefaultLanguageDetector().loadModels();
        }
        // 检测语言
        LanguageResult result = detector.detect(text);
        // 返回语言代码（如"en"、"zh-cn"）
        return result.getLanguage();
    }
}
