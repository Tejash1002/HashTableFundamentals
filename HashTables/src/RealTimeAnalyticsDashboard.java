import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class PageViewEvent {
    String url;
    String userId;
    String source;

    PageViewEvent(String url, String userId, String source) {
        this.url = url;
        this.userId = userId;
        this.source = source;
    }
}

public class RealTimeAnalyticsDashboard {

    // pageUrl -> total views
    private final ConcurrentHashMap<String, AtomicInteger> pageViews = new ConcurrentHashMap<>();

    // pageUrl -> unique visitors
    private final ConcurrentHashMap<String, Set<String>> uniqueVisitors = new ConcurrentHashMap<>();

    // source -> count
    private final ConcurrentHashMap<String, AtomicInteger> trafficSources = new ConcurrentHashMap<>();

    // Top N pages
    private final int TOP_N = 10;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public RealTimeAnalyticsDashboard() {
        // Schedule dashboard update every 5 seconds
        scheduler.scheduleAtFixedRate(this::displayDashboard, 5, 5, TimeUnit.SECONDS);
    }

    // Process an incoming event
    public void processEvent(PageViewEvent event) {
        // Increment page views
        pageViews.computeIfAbsent(event.url, k -> new AtomicInteger()).incrementAndGet();

        // Track unique visitors
        uniqueVisitors.computeIfAbsent(event.url, k -> ConcurrentHashMap.newKeySet()).add(event.userId);

        // Count traffic source
        trafficSources.computeIfAbsent(event.source, k -> new AtomicInteger()).incrementAndGet();
    }

    // Display dashboard
    private void displayDashboard() {
        System.out.println("\n=== DASHBOARD UPDATE ===");

        // Top pages by views
        PriorityQueue<Map.Entry<String, AtomicInteger>> topPages = new PriorityQueue<>(
                Comparator.comparingInt(e -> e.getValue().get())
        );

        for (Map.Entry<String, AtomicInteger> entry : pageViews.entrySet()) {
            topPages.offer(entry);
            if (topPages.size() > TOP_N) topPages.poll();
        }

        List<Map.Entry<String, AtomicInteger>> topList = new ArrayList<>();
        while (!topPages.isEmpty()) topList.add(topPages.poll());
        Collections.reverse(topList);

        System.out.println("Top Pages:");
        for (int i = 0; i < topList.size(); i++) {
            Map.Entry<String, AtomicInteger> e = topList.get(i);
            int uniqueCount = uniqueVisitors.getOrDefault(e.getKey(), Collections.emptySet()).size();
            System.out.printf("%d. %s - %d views (%d unique)%n", i + 1, e.getKey(), e.getValue().get(), uniqueCount);
        }

        // Traffic source percentages
        int total = trafficSources.values().stream().mapToInt(AtomicInteger::get).sum();
        System.out.println("\nTraffic Sources:");
        for (Map.Entry<String, AtomicInteger> entry : trafficSources.entrySet()) {
            double percent = (entry.getValue().get() * 100.0) / total;
            System.out.printf("%s: %.1f%%\n", entry.getKey(), percent);
        }

        System.out.println("========================\n");
    }

    // Main test
    public static void main(String[] args) throws InterruptedException {
        RealTimeAnalyticsDashboard dashboard = new RealTimeAnalyticsDashboard();

        // Simulate page view events
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_123", "google"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_456", "facebook"));
        dashboard.processEvent(new PageViewEvent("/sports/championship", "user_789", "direct"));
        dashboard.processEvent(new PageViewEvent("/article/breaking-news", "user_123", "google"));
        dashboard.processEvent(new PageViewEvent("/sports/championship", "user_456", "google"));

        // Keep main thread alive to see dashboard updates
        Thread.sleep(15000);
    }
}