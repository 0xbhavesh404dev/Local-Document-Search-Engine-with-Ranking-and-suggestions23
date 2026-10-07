package algorithms;

import model.Document;
import java.util.*;

public class SearchAlgorithms {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "a", "an", "the", "is", "are", "was", "were", "of", "to", "in", 
            "on", "for", "and", "or", "with", "as", "by", "from", "this", "that", "it", "at"
    ));

    public static String normalizeText(String text) {
        if (text == null) return "";
        // Convert to lowercase and remove punctuation
        return text.toLowerCase().replaceAll("[^a-z0-9\\s]", " ");
    }

    public static List<String> tokenizeAndRemoveStopWords(String normalizedText) {
        List<String> tokens = new ArrayList<>();
        if (normalizedText == null || normalizedText.trim().isEmpty()) {
            return tokens;
        }
        
        String[] words = normalizedText.split("\\s+");
        for (String word : words) {
            if (!word.isEmpty() && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    // KMP Algorithm implementation
    public static int[] buildLPS(String pattern) {
        int m = pattern.length();
        int[] lps = new int[m];
        int len = 0;
        int i = 1;

        while (i < m) {
            if (pattern.charAt(i) == pattern.charAt(len)) {
                len++;
                lps[i] = len;
                i++;
            } else {
                if (len != 0) {
                    len = lps[len - 1];
                } else {
                    lps[i] = 0;
                    i++;
                }
            }
        }
        return lps;
    }

    public static int kmpSearch(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) return -1;
        if (text == null || text.isEmpty()) return -1;

        int n = text.length();
        int m = pattern.length();
        int[] lps = buildLPS(pattern);

        int i = 0; // index for text
        int j = 0; // index for pattern

        while (i < n) {
            if (pattern.charAt(j) == text.charAt(i)) {
                j++;
                i++;
            }
            if (j == m) {
                return i - j; // Match found, return starting index
            } else if (i < n && pattern.charAt(j) != text.charAt(i)) {
                if (j != 0) {
                    j = lps[j - 1];
                } else {
                    i++;
                }
            }
        }
        return -1; // No match
    }

    public static String extractSnippet(String originalText, int matchPosition, String matchedPhrase) {
        if (matchPosition < 0 || matchPosition >= originalText.length()) {
            return "";
        }
        
        // Extract 20 words before and after
        int wordsBefore = 20;
        int wordsAfter = 20;

        // Find the start index by counting spaces backwards
        int startIndex = matchPosition;
        int spaceCount = 0;
        while (startIndex > 0 && spaceCount <= wordsBefore) {
            startIndex--;
            if (Character.isWhitespace(originalText.charAt(startIndex))) {
                spaceCount++;
            }
        }
        if (startIndex > 0) startIndex++; // Skip the last space found

        // Find the end index by counting spaces forwards
        int endIndex = matchPosition + matchedPhrase.length();
        spaceCount = 0;
        while (endIndex < originalText.length() && spaceCount <= wordsAfter) {
            if (Character.isWhitespace(originalText.charAt(endIndex))) {
                spaceCount++;
            }
            endIndex++;
        }
        if (endIndex < originalText.length()) endIndex--;

        String prefix = startIndex > 0 ? "... " : "";
        String suffix = endIndex < originalText.length() - 1 ? " ..." : "";
        
        String beforeStr = originalText.substring(startIndex, matchPosition);
        String matchStr = originalText.substring(matchPosition, matchPosition + matchedPhrase.length());
        String afterStr = originalText.substring(matchPosition + matchedPhrase.length(), endIndex);

        return prefix + beforeStr + "[" + matchStr + "]" + afterStr + suffix;
    }

    public static double computeCosineSimilarity(Document doc1, Document doc2) {
        Map<String, Integer> tf1 = getTermFrequencies(doc1);
        Map<String, Integer> tf2 = getTermFrequencies(doc2);
        
        Set<String> uniqueTerms = new HashSet<>();
        uniqueTerms.addAll(tf1.keySet());
        uniqueTerms.addAll(tf2.keySet());

        double dotProduct = 0.0;
        double norm1 = 0.0;
        double norm2 = 0.0;

        for (String term : uniqueTerms) {
            int count1 = tf1.getOrDefault(term, 0);
            int count2 = tf2.getOrDefault(term, 0);
            
            dotProduct += (count1 * count2);
            norm1 += (count1 * count1);
            norm2 += (count2 * count2);
        }

        if (norm1 == 0 || norm2 == 0) {
            return 0.0; // Avoid division by zero
        }
        
        return dotProduct / (Math.sqrt(norm1) * Math.sqrt(norm2));
    }

    private static Map<String, Integer> getTermFrequencies(Document doc) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : doc.getTokens()) {
            tf.put(token, tf.getOrDefault(token, 0) + 1);
        }
        return tf;
    }
}
