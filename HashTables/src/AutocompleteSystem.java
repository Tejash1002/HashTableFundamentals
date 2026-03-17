import java.util.*;

class TrieNode {
    Map<Character, TrieNode> children = new HashMap<>();
    boolean isEndOfWord = false;
    String fullQuery = null;
}

public class AutocompleteSystem {

    private final TrieNode root;
    private final Map<String, Integer> frequencyMap; // query -> frequency
    private final int TOP_K = 10;

    public AutocompleteSystem() {
        this.root = new TrieNode();
        this.frequencyMap = new HashMap<>();
    }

    // Insert a query into the Trie
    public void insert(String query) {
        TrieNode node = root;
        for (char ch : query.toCharArray()) {
            node = node.children.computeIfAbsent(ch, c -> new TrieNode());
        }
        node.isEndOfWord = true;
        node.fullQuery = query;
    }

    // Update frequency of a query
    public void updateFrequency(String query) {
        int freq = frequencyMap.getOrDefault(query, 0) + 1;
        frequencyMap.put(query, freq);
        insert(query); // ensure query exists in Trie
        System.out.println("Query \"" + query + "\" frequency: " + freq);
    }

    // Search top-K suggestions for a prefix
    public List<String> search(String prefix) {
        TrieNode node = root;
        for (char ch : prefix.toCharArray()) {
            if (!node.children.containsKey(ch)) return Collections.emptyList();
            node = node.children.get(ch);
        }

        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue)
        );

        dfs(node, pq);

        List<String> result = new ArrayList<>();
        while (!pq.isEmpty()) {
            result.add(pq.poll().getKey());
        }
        Collections.reverse(result);
        return result;
    }

    // DFS to collect queries under a Trie node
    private void dfs(TrieNode node, PriorityQueue<Map.Entry<String, Integer>> pq) {
        if (node.isEndOfWord) {
            int freq = frequencyMap.getOrDefault(node.fullQuery, 0);
            pq.offer(new AbstractMap.SimpleEntry<>(node.fullQuery, freq));
            if (pq.size() > TOP_K) pq.poll();
        }

        for (TrieNode child : node.children.values()) {
            dfs(child, pq);
        }
    }

    // Main test
    public static void main(String[] args) {
        AutocompleteSystem auto = new AutocompleteSystem();

        // Add initial queries
        auto.updateFrequency("javascript");
        auto.updateFrequency("java tutorial");
        auto.updateFrequency("java download");
        auto.updateFrequency("java 21 features");
        auto.updateFrequency("java 21 features");
        auto.updateFrequency("java 21 features");

        System.out.println("\nAutocomplete for \"jav\":");
        List<String> suggestions = auto.search("jav");
        int rank = 1;
        for (String s : suggestions) {
            System.out.printf("%d. \"%s\" (%d searches)%n", rank++, s, auto.frequencyMap.get(s));
        }
    }
}