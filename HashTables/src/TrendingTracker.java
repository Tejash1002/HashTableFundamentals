import java.util.*;
import java.util.concurrent.*;

public class TrendingTracker {

    private final ConcurrentHashMap<String, AtomicInteger> counts = new ConcurrentHashMap<>();

    // Process a new mention
    public void mention(String topic) {
        counts.computeIfAbsent(topic, k -> new AtomicInteger()).incrementAndGet();
    }

    // Get trending topics sorted by frequency
    public List<Map.Entry<String, Integer>> getTrending(int topN) {
        PriorityQueue<Map.Entry<String, Integer>> pq = new PriorityQueue<>(
                Comparator.comparingInt(Map.Entry::getValue)
        );

        for (Map.Entry<String, AtomicInteger> e : counts.entrySet()) {
            pq.offer(new AbstractMap.SimpleEntry<>(e.getKey(), e.getValue().get()));
            if (pq.size() > topN) pq.poll();
        }

        List<Map.Entry<String, Integer>> result = new ArrayList<>();
        while (!pq.isEmpty()) result.add(pq.poll());
        Collections.reverse(result);
        return result;
    }

    public static void main(String[] args) throws InterruptedException {
        TrendingTracker tracker = new TrendingTracker();

        tracker.mention("features");
        System.out.println(tracker.getTrending(5)); // → features:1

        tracker.mention("features");
        System.out.println(tracker.getTrending(5)); // → features:2

        tracker.mention("features");
        System.out.println(tracker.getTrending(5)); // → features:3
    }
}