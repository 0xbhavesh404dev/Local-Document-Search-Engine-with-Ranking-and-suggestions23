package index;

import model.Document;
import model.Posting;
import java.util.*;

public class InvertedIndex {
    private final Map<String, List<Posting>> index;
    private int totalEntries = 0;

    public InvertedIndex() {
        // Using HashMap to provide O(1) average lookup for keywords.
        this.index = new HashMap<>();
    }

    public void addDocument(Document doc) {
        List<String> tokens = doc.getTokens();
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);
            
            index.putIfAbsent(token, new ArrayList<>());
            List<Posting> postings = index.get(token);
            
            // Check if last posting is for the same document
            if (!postings.isEmpty() && postings.get(postings.size() - 1).getDocId() == doc.getDocId()) {
                postings.get(postings.size() - 1).addPosition(i);
            } else {
                postings.add(new Posting(doc.getDocId(), i));
                totalEntries++; // Rough approximation of unique term-doc pairs
            }
        }
    }

    /**
     * O(1) average lookup for exact keyword search.
     * Returns the posting list for the given keyword.
     */
    public List<Posting> searchKeyword(String keyword) {
        return index.getOrDefault(keyword, Collections.emptyList());
    }

    /**
     * Intersects the posting lists for all words in the phrase.
     * Returns candidate Document IDs that contain all words.
     */
    public Set<Integer> getCandidateDocuments(List<String> phraseTokens) {
        if (phraseTokens == null || phraseTokens.isEmpty()) {
            return Collections.emptySet();
        }

        List<Posting> firstPostings = index.get(phraseTokens.get(0));
        if (firstPostings == null || firstPostings.isEmpty()) return Collections.emptySet();

        Set<Integer> candidates = new HashSet<>();
        for (Posting p : firstPostings) {
            candidates.add(p.getDocId());
        }

        for (int i = 1; i < phraseTokens.size(); i++) {
            List<Posting> postings = index.get(phraseTokens.get(i));
            if (postings == null || postings.isEmpty()) return Collections.emptySet();

            Set<Integer> currentDocIds = new HashSet<>();
            for (Posting p : postings) {
                currentDocIds.add(p.getDocId());
            }

            candidates.retainAll(currentDocIds);
            
            if (candidates.isEmpty()) break; // Early termination if intersection becomes empty
        }

        return candidates;
    }

    public int getUniqueTermsCount() {
        return index.size();
    }
}
