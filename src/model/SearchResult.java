package model;

import java.util.List;

public class SearchResult implements Comparable<SearchResult> {
    private final Document document;
    private int frequency;
    private List<Integer> positions;
    private int matchPosition; // Exact char index for phrase search
    private String snippet;
    
    // For keyword search
    public SearchResult(Document document, int frequency, List<Integer> positions) {
        this.document = document;
        this.frequency = frequency;
        this.positions = positions;
        this.matchPosition = -1;
    }

    // For phrase search
    public SearchResult(Document document, int matchPosition, String snippet) {
        this.document = document;
        this.matchPosition = matchPosition;
        this.snippet = snippet;
        this.frequency = 1; // Simplification for exact phrase search sort order if needed
    }

    public Document getDocument() {
        return document;
    }

    public int getFrequency() {
        return frequency;
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public int getMatchPosition() {
        return matchPosition;
    }

    public String getSnippet() {
        return snippet;
    }

    // Sort by frequency descending by default
    @Override
    public int compareTo(SearchResult other) {
        return Integer.compare(other.frequency, this.frequency);
    }
}
