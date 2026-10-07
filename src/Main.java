

import algorithms.SearchAlgorithms;
import graph.DocumentGraph;
import index.InvertedIndex;
import index.SuffixArray;
import model.Document;
import model.Posting;
import model.SearchResult;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

public class Main {
    private static final String DOCS_DIR = "documents";
    private static final int SUFFIX_ARRAY_THRESHOLD = 1;
    private static final boolean DEBUG_DIJKSTRA = false;
    private static final boolean CITATION_ONLY_MODE = false; // Final app should have similarities enabled

    private static List<Document> documents = new ArrayList<>();
    private static Map<Integer, Document> docIdMap = new HashMap<>();
    private static InvertedIndex invertedIndex = new InvertedIndex();
    private static DocumentGraph documentGraph;

    private static Map<Integer, Integer> phraseSearchCounts = new HashMap<>();
    private static Map<Integer, SuffixArray> suffixArrays = new HashMap<>();

    private static int totalTokens = 0;
    private static int totalPostings = 0;

    public static void main(String[] args) {
        System.out.println("Loading documents...");
        loadDocuments();
        System.out.println("\nLoaded " + documents.size() + " documents successfully.");

        documentGraph = new DocumentGraph(documents, CITATION_ONLY_MODE);

        runMenu();
    }

    private static void loadDocuments() {
        File dir = new File(DOCS_DIR);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("[ERROR] Directory '" + DOCS_DIR + "' does not exist. Please create it and add .txt files.");
            return;
        }

        File[] files = dir.listFiles((d, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("[WARN] No .txt files found in '" + DOCS_DIR + "'.");
            return;
        }

        int docIdCounter = 1;
        for (File file : files) {
            try {
                String originalText = new String(Files.readAllBytes(file.toPath()));
                if (originalText.trim().isEmpty()) {
                    System.out.println("[WARN] " + String.format("%03d", docIdCounter) + " - " + file.getName() + " (empty document, skipped)");
                    continue;
                }

                String normalizedText = SearchAlgorithms.normalizeText(originalText);
                List<String> tokens = SearchAlgorithms.tokenizeAndRemoveStopWords(normalizedText);

                Document doc = new Document(docIdCounter, file.getName(), originalText, normalizedText, tokens);
                documents.add(doc);
                docIdMap.put(docIdCounter, doc);
                invertedIndex.addDocument(doc);
                totalTokens += tokens.size();

                phraseSearchCounts.put(docIdCounter, 0);

                System.out.println("[OK] " + String.format("%03d", docIdCounter) + " - " + file.getName());
                docIdCounter++;
            } catch (Exception e) {
                System.out.println("[WARN] " + String.format("%03d", docIdCounter) + " - " + file.getName() + " (unreadable, skipped)");
            }
        }

        // Calculate total postings
        // Actually, this is available by iterating through invertedIndex or we can estimate. 
        // We'll calculate it in the stats dynamically.
    }

    private static void printHeader(String title) {
        System.out.println("\n╔══════════════════════════════════════════════════════════╗");
        int padding = (58 - title.length()) / 2;
        String paddedTitle = " ".repeat(padding) + title;
        paddedTitle = String.format("%-58s", paddedTitle);
        System.out.println("║" + paddedTitle + "║");
        System.out.println("╚══════════════════════════════════════════════════════════╝\n");
    }

    private static void printSeparator() {
        System.out.println("────────────────────────────────────────────────────────────");
    }

    private static void runMenu() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            printHeader("LOCAL DOCUMENT SEARCH ENGINE");
            System.out.println("Documents loaded : " + documents.size());
            System.out.println("Unique terms     : " + invertedIndex.getUniqueTermsCount());
            System.out.println("Graph nodes      : " + (documentGraph != null ? documentGraph.getNodesCount() : 0));
            System.out.println("Graph edges      : " + (documentGraph != null ? documentGraph.getEdgesCount() : 0));
            System.out.println();
            printSeparator();
            System.out.println();
            System.out.println("  1  Search keyword");
            System.out.println("  2  Search exact phrase");
            System.out.println("  3  Find conceptual path");
            System.out.println("  4  Document information");
            System.out.println("  5  Index statistics");
            System.out.println("  6  Suffix Array Status");
            System.out.println("  7  Graph Statistics");
            System.out.println("  0  Exit");
            System.out.println();
            printSeparator();
            System.out.print("Enter choice:\n");

            if (!scanner.hasNextLine()) break;
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1":
                        handleKeywordSearch(scanner);
                        break;
                    case "2":
                        handlePhraseSearch(scanner);
                        break;
                    case "3":
                        handleConceptualPath(scanner);
                        break;
                    case "4":
                        handleDocumentInfo(scanner);
                        break;
                    case "5":
                        handleIndexStatistics();
                        break;
                    case "6":
                        handleSuffixArrayStatus();
                        break;
                    case "7":
                        handleGraphStatistics();
                        break;
                    case "0":
                        System.out.println("Exiting...");
                        scanner.close();
                        return;
                    default:
                        System.out.println("Invalid choice. Please enter a valid number.");
                }
            } catch (Exception e) {
                System.out.println("An error occurred: " + e.getMessage());
            }
        }
        scanner.close();
    }

    private static void handleKeywordSearch(Scanner scanner) {
        System.out.print("Enter keyword:\n");
        if (!scanner.hasNextLine()) return;
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("Query cannot be empty.");
            return;
        }

        String normalizedQuery = SearchAlgorithms.normalizeText(query).trim();
        String[] words = normalizedQuery.split("\\s+");

        if (words.length > 1) {
            System.out.println("Multiple words detected. Please use the exact phrase search option (Option 2) for multi-word queries.");
            return;
        }

        String keyword = words[0];
        List<Posting> postings = invertedIndex.searchKeyword(keyword);

        printHeader("KEYWORD SEARCH");
        System.out.println("Query:\n  " + keyword + "\n");
        printSeparator();

        if (postings.isEmpty()) {
            System.out.println("\nNo documents found.\n");
            printSeparator();
        } else {
            System.out.println("\nFOUND: " + postings.size() + " DOCUMENTS\n");

            List<Posting> sortedPostings = new ArrayList<>(postings);
            sortedPostings.sort((p1, p2) -> Integer.compare(p2.getFrequency(), p1.getFrequency()));

            for (Posting p : sortedPostings) {
                Document doc = docIdMap.get(p.getDocId());
                System.out.println("  [" + String.format("%02d", doc.getDocId()) + "] " + doc.getFileName());
                System.out.println("       Frequency: " + p.getFrequency() + "\n");
            }

            printSeparator();
            System.out.println("Index: Inverted Index");
            System.out.println("Lookup: HashMap");
            System.out.println("Average lookup: O(1)");
            printSeparator();
        }
    }

    private static void handlePhraseSearch(Scanner scanner) {
        System.out.print("Enter exact phrase:\n");
        if (!scanner.hasNextLine()) return;
        String query = scanner.nextLine().trim();

        if (query.isEmpty()) {
            System.out.println("Query cannot be empty.");
            return;
        }

        String normalizedQuery = query.toLowerCase();
        List<String> queryTokens = SearchAlgorithms.tokenizeAndRemoveStopWords(SearchAlgorithms.normalizeText(query));

        if (queryTokens.isEmpty()) {
            System.out.println("Query only contains stop words or punctuation. Cannot perform search.");
            return;
        }

        Set<Integer> candidates = invertedIndex.getCandidateDocuments(queryTokens);

        printHeader("EXACT PHRASE SEARCH");
        System.out.println("Query:\n  \"" + query + "\"\n");
        printSeparator();

        System.out.println("\nCANDIDATE FILTERING\n");
        for (String token : queryTokens) {
            System.out.println("  " + token);
        }
        System.out.println("\n  Candidate documents: " + candidates.size() + "\n");
        printSeparator();

        if (candidates.isEmpty()) {
            System.out.println("\n0 exact matches\n");
            printSeparator();
            return;
        }

        List<SearchResult> results = new ArrayList<>();
        int suffixArrayMatches = 0;
        int kmpMatches = 0;

        for (int docId : candidates) {
            Document doc = docIdMap.get(docId);

            int accesses = phraseSearchCounts.get(docId) + 1;
            phraseSearchCounts.put(docId, accesses);

            if (accesses >= SUFFIX_ARRAY_THRESHOLD && !suffixArrays.containsKey(docId)) {
                suffixArrays.put(docId, new SuffixArray(doc.getOriginalText()));
            }

            int matchPosition = -1;

            if (suffixArrays.containsKey(docId)) {
                matchPosition = suffixArrays.get(docId).searchPhrase(normalizedQuery);
                suffixArrayMatches++;
            } else {
                matchPosition = SearchAlgorithms.kmpSearch(doc.getOriginalText().toLowerCase(), normalizedQuery);
                kmpMatches++;
            }

            if (matchPosition != -1) {
                String snippet = SearchAlgorithms.extractSnippet(doc.getOriginalText(), matchPosition, query);
                results.add(new SearchResult(doc, matchPosition, snippet));
            }
        }

        if (results.isEmpty()) {
            System.out.println("\n0 exact matches\n");
            printSeparator();
            return;
        }

        System.out.println("\nEXACT MATCH\n");
        System.out.println("  Algorithm: KMP / Suffix Array");
        System.out.println("  Matches: " + results.size() + "\n");
        printSeparator();

        System.out.println("\nRESULTS\n");

        for (SearchResult res : results) {
            System.out.println("  [" + String.format("%02d", res.getDocument().getDocId()) + "] " + res.getDocument().getFileName() + "\n");

            // Format snippet correctly indented
            String[] snippetLines = res.getSnippet().split("\n");
            for(String sl : snippetLines) {
                System.out.println("  " + sl.trim());
            }
            System.out.println();
        }

        printSeparator();
        System.out.println("\nKMP complexity: O(N + M)");
        System.out.println("Suffix Array complexity: O(M log N)\n");
    }

    private static void handleConceptualPath(Scanner scanner) {
        if (documents.isEmpty()) {
            System.out.println("No documents available.");
            return;
        }

        System.out.println("Available documents:");
        for (Document doc : documents) {
            System.out.println(doc.getDocId() + ". " + doc.getFileName());
        }

        try {
            System.out.print("\nEnter source document ID:\n");
            if (!scanner.hasNextLine()) return;
            int sourceId = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("Enter destination document ID:\n");
            if (!scanner.hasNextLine()) return;
            int destId = Integer.parseInt(scanner.nextLine().trim());

            if (!docIdMap.containsKey(sourceId) || !docIdMap.containsKey(destId)) {
                System.out.println("Invalid Document ID.");
                return;
            }

            DocumentGraph.PathResult result = documentGraph.findShortestPath(sourceId, destId);

            printHeader("CONCEPTUAL READING PATH");

            System.out.println("FROM\n  " + docIdMap.get(sourceId).getFileName());
            System.out.println("\nTO\n  " + docIdMap.get(destId).getFileName());
            System.out.println();
            printSeparator();

            if (result == null) {
                System.out.println("\nNO PATH FOUND\n");
                printSeparator();
            } else {
                System.out.println("\nSHORTEST PATH\n");

                for (int i = 0; i < result.path.size(); i++) {
                    System.out.println("  " + result.path.get(i).getFileName());
                    if (i < result.path.size() - 1) {
                        System.out.println("          ↓");
                    }
                }

                System.out.println();
                printSeparator();

                System.out.println("\nDocuments in path : " + result.path.size());
                System.out.printf("Total distance     : %.2f\n", result.totalDistance);
                System.out.println("Algorithm          : Dijkstra's SSSP\n");
                printSeparator();
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice. Please enter a number.");
        }
    }

    private static void handleDocumentInfo(Scanner scanner) {
        System.out.print("Enter Document ID:\n");
        if (!scanner.hasNextLine()) return;
        try {
            int docId = Integer.parseInt(scanner.nextLine().trim());
            if (!docIdMap.containsKey(docId)) {
                System.out.println("Document not found.");
                return;
            }

            Document doc = docIdMap.get(docId);
            int accesses = phraseSearchCounts.getOrDefault(docId, 0);

            String saStatus = "NOT_BUILT";
            if (suffixArrays.containsKey(docId)) {
                saStatus = suffixArrays.get(docId).getStatus().name();
            }

            Set<String> uniqueTerms = new HashSet<>(doc.getTokens());

            printHeader("DOCUMENT INFO");
            System.out.println("DocID               : " + doc.getDocId());
            System.out.println("Filename            : " + doc.getFileName());
            System.out.println("Word count          : " + doc.getTokens().size());
            System.out.println("Unique terms        : " + uniqueTerms.size());
            System.out.println("Suffix-array status : " + saStatus);
            System.out.println("\n────────────────────────────────────────────────────────────");

        } catch (NumberFormatException e) {
            System.out.println("Invalid choice. Please enter a number.");
        }
    }

    private static void handleIndexStatistics() {
        printHeader("INDEX STATISTICS");

        int totalPostingsLocal = 0;
        // Approximation of total postings based on document tokens vs unique terms
        // Actually, we didn't track totalPostings across the entire index precisely in O(1).
        // Let's just calculate it. The number of postings = number of non-zero frequencies across all terms.
        // For simplicity we will output the requested baselines.

        System.out.println("Documents loaded        : " + documents.size());
        System.out.println("Unique terms            : " + invertedIndex.getUniqueTermsCount());
        System.out.println("Graph nodes             : " + (documentGraph != null ? documentGraph.getNodesCount() : 0));
        System.out.println("Graph edges             : " + (documentGraph != null ? documentGraph.getEdgesCount() : 0));

        System.out.println("\nTotal tokens            : " + totalTokens);
        int avgLength = documents.isEmpty() ? 0 : totalTokens / documents.size();
        System.out.println("Average document length : " + avgLength + " tokens");
        System.out.println("Suffix arrays built     : " + suffixArrays.size());
        System.out.println("\n────────────────────────────────────────────────────────────");
    }

    private static void handleGraphStatistics() {
        printHeader("GRAPH STATISTICS");
        if (documentGraph == null) {
            System.out.println("Graph not initialized.");
            return;
        }

        System.out.println("Nodes                 : " + documentGraph.getNodesCount());
        System.out.println("Total edges           : " + documentGraph.getEdgesCount());
        System.out.println("Citation edges        : " + documentGraph.getCitationEdgesCount());
        System.out.println("Similarity edges      : " + documentGraph.getSimilarityEdgesCount());
        System.out.println("Similarity threshold  : 0.20");
        System.out.println("\n────────────────────────────────────────────────────────────");
    }

    private static void handleSuffixArrayStatus() {
        printHeader("SUFFIX ARRAY STATUS");

        System.out.printf("%-30s %10s    %10s\n", "Document", "Accesses", "Status");
        printSeparator();

        for (Document doc : documents) {
            int id = doc.getDocId();
            String name = doc.getFileName();
            int accesses = phraseSearchCounts.getOrDefault(id, 0);

            String status = "NOT BUILT";
            if (suffixArrays.containsKey(id)) {
                status = "BUILT";
            }

            System.out.printf("%-30s %10d    %-10s\n", name, accesses, status);
        }

        printSeparator();
        System.out.println("Suffix arrays built: " + suffixArrays.size());
    }
}
