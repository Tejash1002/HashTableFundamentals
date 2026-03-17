import java.util.*;
import java.nio.file.*;
import java.io.*;

public class PlagiarismDetector {

    private int nGramSize;
    // n-gram -> set of document IDs containing this n-gram
    private Map<String, Set<String>> nGramIndex;
    // document ID -> number of n-grams
    private Map<String, Integer> documentNGramCount;

    public PlagiarismDetector(int nGramSize) {
        this.nGramSize = nGramSize;
        this.nGramIndex = new HashMap<>();
        this.documentNGramCount = new HashMap<>();
    }

    // Analyze a document, extract n-grams, and index them
    public void analyzeDocument(String docId, String content) {
        String[] words = content.split("\\s+");
        int totalNGrams = 0;

        for (int i = 0; i <= words.length - nGramSize; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < nGramSize; j++) {
                sb.append(words[i + j]).append(" ");
            }
            String nGram = sb.toString().trim();

            nGramIndex.computeIfAbsent(nGram, k -> new HashSet<>()).add(docId);
            totalNGrams++;
        }

        documentNGramCount.put(docId, totalNGrams);
        System.out.println("Document " + docId + ": Extracted " + totalNGrams + " n-grams");
    }

    // Compare a document against the existing database
    public void compareDocument(String docId, String content) {
        String[] words = content.split("\\s+");
        Map<String, Integer> matchCounts = new HashMap<>();

        for (int i = 0; i <= words.length - nGramSize; i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < nGramSize; j++) {
                sb.append(words[i + j]).append(" ");
            }
            String nGram = sb.toString().trim();

            if (nGramIndex.containsKey(nGram)) {
                for (String otherDoc : nGramIndex.get(nGram)) {
                    matchCounts.put(otherDoc, matchCounts.getOrDefault(otherDoc, 0) + 1);
                }
            }
        }

        // Calculate similarity percentages
        for (Map.Entry<String, Integer> entry : matchCounts.entrySet()) {
            String otherDoc = entry.getKey();
            int matches = entry.getValue();
            int totalOther = documentNGramCount.getOrDefault(otherDoc, 1);
            double similarity = (matches * 100.0) / totalOther;

            System.out.printf("→ Found %d matching n-grams with \"%s\"%n", matches, otherDoc);
            System.out.printf("→ Similarity: %.1f%% %s%n", similarity, similarity > 50 ? "(PLAGIARISM DETECTED)" : "(suspicious)");
        }
    }

    // Helper to load file content
    public static String readFile(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)));
        } catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    // Main test
    public static void main(String[] args) {
        PlagiarismDetector detector = new PlagiarismDetector(5); // 5-grams

        // Simulate existing database
        String doc1 = "This is a sample essay about computer science and programming.";
        String doc2 = "Programming in Java is fun and computer science is fascinating.";
        detector.analyzeDocument("essay_089.txt", doc1);
        detector.analyzeDocument("essay_092.txt", doc2);

        // New submission to compare
        String newEssay = "Programming in Java is fun and computer science is fascinating.";
        detector.compareDocument("essay_123.txt", newEssay);
    }
}