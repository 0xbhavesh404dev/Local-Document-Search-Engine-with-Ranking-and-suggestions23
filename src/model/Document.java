package model;

import java.util.List;

public class Document {
    private final int docId;
    private final String fileName;
    private final String originalText;
    private final String normalizedText;
    private final List<String> tokens;

    public Document(int docId, String fileName, String originalText, String normalizedText, List<String> tokens) {
        this.docId = docId;
        this.fileName = fileName;
        this.originalText = originalText;
        this.normalizedText = normalizedText;
        this.tokens = tokens;
    }

    public int getDocId() {
        return docId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getNormalizedText() {
        return normalizedText;
    }

    public List<String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        return "DocID: " + docId + ", File: " + fileName;
    }
}
