package graph;

import algorithms.SearchAlgorithms;
import model.Document;

import java.util.*;

public class DocumentGraph {
    private static final double SIMILARITY_THRESHOLD = 0.20;
    private static final double CITATION_WEIGHT = 0.1;

    // Adjacency list: DocID -> (Neighbor DocID -> Weight)
    private final Map<Integer, Map<Integer, Double>> adjacencyList;
    private final Map<Integer, Document> documentsMap;
    private int edgesCount = 0;
    private int citationEdgesCount = 0;
    private int similarityEdgesCount = 0;

    private final boolean citationOnlyMode;

    public DocumentGraph(List<Document> documents) {
        this(documents, false);
    }

    public DocumentGraph(List<Document> documents, boolean citationOnlyMode) {
        this.adjacencyList = new HashMap<>();
        this.documentsMap = new HashMap<>();
        this.citationOnlyMode = citationOnlyMode;

        for (Document doc : documents) {
            documentsMap.put(doc.getDocId(), doc);
            adjacencyList.put(doc.getDocId(), new HashMap<>());
        }

        buildGraph();
    }

    private void buildGraph() {
        List<Document> docs = new ArrayList<>(documentsMap.values());

        for (int i = 0; i < docs.size(); i++) {
            Document doc1 = docs.get(i);
            
            // 1. Direct Citations
            for (Document doc2 : docs) {
                if (doc1.getDocId() == doc2.getDocId()) continue;
                
                String rawName = doc2.getFileName().replace(".txt.txt", ".txt");
                String baseName = rawName.replace(".txt", "");
                
                // Check multiple variations of the filename
                if (doc1.getOriginalText().contains(doc2.getFileName()) ||
                    doc1.getOriginalText().contains(rawName) ||
                    doc1.getOriginalText().contains(baseName + ".txt")) {
                    if (addEdge(doc1.getDocId(), doc2.getDocId(), CITATION_WEIGHT)) {
                        citationEdgesCount++;
                    }
                }
            }

            // 2. Cosine Similarity
            if (!citationOnlyMode) {
                for (int j = i + 1; j < docs.size(); j++) {
                    Document doc2 = docs.get(j);
                    
                    double similarity = SearchAlgorithms.computeCosineSimilarity(doc1, doc2);
                    if (similarity >= SIMILARITY_THRESHOLD) {
                        double distance = 1.0 - similarity;
                        // Add undirected edges for similarity
                        if (addEdge(doc1.getDocId(), doc2.getDocId(), distance)) {
                            similarityEdgesCount++;
                        }
                        if (addEdge(doc2.getDocId(), doc1.getDocId(), distance)) {
                            similarityEdgesCount++;
                        }
                    }
                }
            }
        }
    }

    private boolean addEdge(int source, int dest, double weight) {
        Map<Integer, Double> edges = adjacencyList.get(source);
        boolean addedNew = false;
        // Only update if the new weight is smaller (e.g. citation is smaller than similarity distance)
        if (!edges.containsKey(dest) || weight < edges.get(dest)) {
            if (!edges.containsKey(dest)) {
                edgesCount++;
                addedNew = true;
            }
            edges.put(dest, weight);
        }
        return addedNew;
    }

    public int getNodesCount() {
        return adjacencyList.size();
    }

    public int getEdgesCount() {
        return edgesCount;
    }

    public int getCitationEdgesCount() {
        return citationEdgesCount;
    }

    public int getSimilarityEdgesCount() {
        return similarityEdgesCount;
    }

    /**
     * Dijkstra's algorithm to find the shortest conceptual path.
     * Complexity: O((V + E) log V)
     */
    public PathResult findShortestPath(int sourceId, int destId) {
        if (!documentsMap.containsKey(sourceId) || !documentsMap.containsKey(destId)) {
            return null;
        }

        Map<Integer, Double> distances = new HashMap<>();
        Map<Integer, Integer> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        for (Integer id : documentsMap.keySet()) {
            distances.put(id, Double.POSITIVE_INFINITY);
            previous.put(id, -1);
        }

        distances.put(sourceId, 0.0);
        pq.add(new NodeDistance(sourceId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance current = pq.poll();
            int u = current.nodeId;

            if (current.distance > distances.get(u)) continue;

            Map<Integer, Double> neighbors = adjacencyList.get(u);
            for (Map.Entry<Integer, Double> neighborEntry : neighbors.entrySet()) {
                int v = neighborEntry.getKey();
                double weight = neighborEntry.getValue();
                double newDist = distances.get(u) + weight;

                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    previous.put(v, u);
                    pq.add(new NodeDistance(v, newDist));
                }
            }
        }

        if (distances.get(destId) == Double.POSITIVE_INFINITY) {
            return null; // No path
        }

        // Reconstruct path
        List<Document> path = new ArrayList<>();
        int curr = destId;

        while (curr != -1) {
            path.add(documentsMap.get(curr));
            if (curr == sourceId) {
                break;
            }
            curr = previous.get(curr);
        }
        
        Collections.reverse(path);
        
        // Safety check if source was somehow not reached
        if (path.isEmpty() || path.get(0).getDocId() != sourceId) {
            return null;
        }

        return new PathResult(path, distances.get(destId), distances, previous);
    }

    private static class NodeDistance {
        int nodeId;
        double distance;

        NodeDistance(int nodeId, double distance) {
            this.nodeId = nodeId;
            this.distance = distance;
        }
    }

    public static class PathResult {
        public final List<Document> path;
        public final double totalDistance;
        public final Map<Integer, Double> distances;
        public final Map<Integer, Integer> previous;

        public PathResult(List<Document> path, double totalDistance, Map<Integer, Double> distances, Map<Integer, Integer> previous) {
            this.path = path;
            this.totalDistance = totalDistance;
            this.distances = distances;
            this.previous = previous;
        }
    }
}
