package searchengine.services.implementation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.dto.indexing.LemmaDto;
import searchengine.dto.indexing.PageDto;
import searchengine.services.utilities.LemmaFinder;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@RequiredArgsConstructor
public class SnippetGenerator {
    private final PageDto pageDto;
    private final List<LemmaDto> lemmas;

    public String generateSnippet() {
        String text = pageDto.getContent().toLowerCase();
        try {
            LemmaFinder lemmaFinder = LemmaFinder.getInstance();
            Set<String> wordForms = getAllWordForms(lemmas, lemmaFinder);
            int index = findFirstMatchIndex(text, wordForms);
            if (index == -1) {
                return "";
            }
            int start = Math.max(0, index - 30);
            int end = Math.min(text.length(), index + 150);
            String snippet = extractText(text.substring(start, end));
            return highlightKeywords(snippet, wordForms);
        } catch (Exception e) {
            log.warn("Snippet was not generated: " + e.getMessage());
            return null;
        }
    }

    private Set<String> getAllWordForms(List<LemmaDto> lemmas, LemmaFinder lemmaFinder) {
        Set<String> wordForms = new HashSet<>();
        for (LemmaDto lemmaDto : lemmas) {
            wordForms.addAll(lemmaFinder.getWordForms(lemmaDto.getLemma()));
        }
        return wordForms;
    }

    public int findFirstMatchIndex(String text, Set<String> words) {
        String lowerText = text.toLowerCase();
        for (String word : words) {
            int index = lowerText.indexOf(word.toLowerCase());
            if (index != -1) {
                return index;
            }
        }
        return -1;
    }

    public static String highlightKeywords(String text, Set<String> wordForms) {
        if (wordForms.isEmpty() || text.isEmpty()) {
            return text;
        }
        String regex = "(?i)(?<=\\s|^)(" + String.join("|", wordForms) + ")(?=\\s|$|[.,!?])";
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(result, "<b>" + matcher.group() + "</b>");
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static String extractText(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        String cleanedText = input.replaceAll("</?(?!b\\b)[a-zA-Z][^>]*>", "");
        cleanedText = cleanedText.replaceAll("<[^>]*|[^<]*>", "");
        cleanedText = cleanedText.replaceAll("\\s+", " ").trim();

        return cleanedText;
    }
}
