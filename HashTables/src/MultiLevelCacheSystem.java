import java.util.*;

class VideoData {
    String videoId;
    String content; // Simulated video content
    public VideoData(String videoId, String content) {
        this.videoId = videoId;
        this.content = content;
    }
}

class MultiLevelCache {

    private final int L1_CAPACITY = 10000;
    private final int L2_CAPACITY = 100000;
    private final LinkedHashMap<String, VideoData> L1Cache;
    private final LinkedHashMap<String, VideoData> L2Cache; // Simulate SSD
    private final Map<String, VideoData> L3Database;

    private final Map<String, Integer> accessCountMap; // tracks promotions
    private int L1Hits = 0, L1Misses = 0;
    private int L2Hits = 0, L2Misses = 0;
    private int L3Hits = 0, L3Misses = 0;

    public MultiLevelCache(Map<String, VideoData> database) {
        // Access-order LinkedHashMap for LRU eviction
        L1Cache = new LinkedHashMap<>(L1_CAPACITY, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
                return size() > L1_CAPACITY;
            }
        };

        L2Cache = new LinkedHashMap<>(L2_CAPACITY, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, VideoData> eldest) {
                return size() > L2_CAPACITY;
            }
        };

        L3Database = database;
        accessCountMap = new HashMap<>();
    }

    // Get video from multi-level cache
    public VideoData getVideo(String videoId) {
        // L1 Cache
        if (L1Cache.containsKey(videoId)) {
            L1Hits++;
            System.out.println("L1 Cache HIT (0.5ms)");
            return L1Cache.get(videoId);
        }
        L1Misses++;
        System.out.println("L1 Cache MISS (0.5ms)");

        // L2 Cache
        if (L2Cache.containsKey(videoId)) {
            L2Hits++;
            System.out.println("L2 Cache HIT (5ms)");
            promoteToL1(videoId, L2Cache.get(videoId));
            return L2Cache.get(videoId);
        }
        L2Misses++;
        System.out.println("L2 Cache MISS (5ms)");

        // L3 Database
        if (L3Database.containsKey(videoId)) {
            L3Hits++;
            System.out.println("L3 Database HIT (150ms)");
            VideoData video = L3Database.get(videoId);
            addToL2(videoId, video);
            return video;
        }
        L3Misses++;
        System.out.println("Video not found in any cache/database!");
        return null;
    }

    // Promote video from L2→L1
    private void promoteToL1(String videoId, VideoData video) {
        accessCountMap.put(videoId, accessCountMap.getOrDefault(videoId, 0) + 1);
        if (!L1Cache.containsKey(videoId) && accessCountMap.get(videoId) > 1) {
            L1Cache.put(videoId, video);
            System.out.println("→ Promoted to L1");
        }
    }

    // Add video to L2 cache
    private void addToL2(String videoId, VideoData video) {
        L2Cache.put(videoId, video);
        accessCountMap.put(videoId, 1);
        System.out.println("→ Added to L2 (access count: 1)");
    }

    // Cache statistics
    public void getStatistics() {
        int L1Total = L1Hits + L1Misses;
        int L2Total = L2Hits + L2Misses;
        int L3Total = L3Hits + L3Misses;

        double overallHits = L1Hits + L2Hits + L3Hits;
        double overallTotal = overallHits + L1Misses + L2Misses + L3Misses;

        System.out.println("\nCache Statistics:");
        System.out.printf("L1: Hit Rate %.1f%%, Avg Time: 0.5ms%n", L1Total == 0 ? 0 : L1Hits*100.0/L1Total);
        System.out.printf("L2: Hit Rate %.1f%%, Avg Time: 5ms%n", L2Total == 0 ? 0 : L2Hits*100.0/L2Total);
        System.out.printf("L3: Hit Rate %.1f%%, Avg Time: 150ms%n", L3Total == 0 ? 0 : L3Hits*100.0/L3Total);
        System.out.printf("Overall: Hit Rate %.1f%%, Avg Time: %.1fms%n",
                overallTotal==0?0: overallHits*100/overallTotal,
                (L1Hits*0.5 + L2Hits*5 + L3Hits*150)/overallTotal);
    }

    // Test
    public static void main(String[] args) {
        Map<String, VideoData> db = new HashMap<>();
        db.put("video_123", new VideoData("video_123", "Movie A"));
        db.put("video_999", new VideoData("video_999", "Movie B"));

        MultiLevelCache cache = new MultiLevelCache(db);

        System.out.println("Request 1: video_123");
        cache.getVideo("video_123"); // L1 MISS → L2 MISS → L3 HIT → Add L2
        System.out.println("\nRequest 2: video_123");
        cache.getVideo("video_123"); // L1 MISS → L2 HIT → Promote L1
        System.out.println("\nRequest 3: video_123");
        cache.getVideo("video_123"); // L1 HIT

        System.out.println("\nRequest 4: video_999");
        cache.getVideo("video_999"); // L1 MISS → L2 MISS → L3 HIT → Add L2

        cache.getStatistics();
    }
}