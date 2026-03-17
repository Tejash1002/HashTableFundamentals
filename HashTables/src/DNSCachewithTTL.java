import java.util.*;
import java.util.concurrent.*;

public class DNSCache {

    // DNS Entry class
    static class DNSEntry {
        String domain;
        String ipAddress;
        long expiryTime;

        DNSEntry(String domain, String ipAddress, long ttlSeconds) {
            this.domain = domain;
            this.ipAddress = ipAddress;
            this.expiryTime = System.currentTimeMillis() + (ttlSeconds * 1000);
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }

    // LRU Cache using LinkedHashMap
    private final int capacity;

    private final Map<String, DNSEntry> cache;

    // Metrics
    private long hits = 0;
    private long misses = 0;
    private long totalLookupTime = 0;

    // Constructor
    public DNSCache(int capacity) {
        this.capacity = capacity;

        this.cache = new LinkedHashMap<String, DNSEntry>(capacity, 0.75f, true) {
            protected boolean removeEldestEntry(Map.Entry<String, DNSEntry> eldest) {
                return size() > DNSCache.this.capacity;
            }
        };

        startCleanupThread();
    }

    // Resolve domain
    public synchronized String resolve(String domain) {
        long start = System.nanoTime();

        DNSEntry entry = cache.get(domain);

        if (entry != null) {
            if (!entry.isExpired()) {
                hits++;
                totalLookupTime += (System.nanoTime() - start);

                return "Cache HIT → " + entry.ipAddress;
            } else {
                cache.remove(domain);
                System.out.println("Cache EXPIRED → " + domain);
            }
        }

        // Cache miss
        misses++;

        // Simulate upstream DNS call
        String ip = queryUpstreamDNS(domain);

        // Store with TTL (example: 5 sec)
        cache.put(domain, new DNSEntry(domain, ip, 5));

        totalLookupTime += (System.nanoTime() - start);

        return "Cache MISS → " + ip;
    }

    // Simulated upstream DNS resolver
    private String queryUpstreamDNS(String domain) {
        try {
            Thread.sleep(100); // simulate latency (100ms)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Fake IP generator
        return "172.217." + new Random().nextInt(255) + "." + new Random().nextInt(255);
    }

    // Cleanup expired entries periodically
    private void startCleanupThread() {
        Thread cleaner = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(2000);

                    synchronized (this) {
                        Iterator<Map.Entry<String, DNSEntry>> it = cache.entrySet().iterator();

                        while (it.hasNext()) {
                            Map.Entry<String, DNSEntry> entry = it.next();
                            if (entry.getValue().isExpired()) {
                                it.remove();
                            }
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        cleaner.setDaemon(true);
        cleaner.start();
    }

    // Cache statistics
    public synchronized String getCacheStats() {
        long totalRequests = hits + misses;
        double hitRate = (totalRequests == 0) ? 0 : (hits * 100.0 / totalRequests);

        double avgLookupMs = (totalRequests == 0) ? 0 :
                (totalLookupTime / 1_000_000.0) / totalRequests;

        return String.format("Hit Rate: %.2f%%, Avg Lookup Time: %.2f ms",
                hitRate, avgLookupMs);
    }

    // Main test
    public static void main(String[] args) throws InterruptedException {
        DNSCache dnsCache = new DNSCache(3);

        System.out.println(dnsCache.resolve("google.com"));
        System.out.println(dnsCache.resolve("google.com"));

        Thread.sleep(6000); // wait for TTL expiry

        System.out.println(dnsCache.resolve("google.com"));

        System.out.println(dnsCache.getCacheStats());
    }
}