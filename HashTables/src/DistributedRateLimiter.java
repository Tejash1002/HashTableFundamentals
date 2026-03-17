import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class TokenBucket {
    private final int maxTokens;
    private final double refillRatePerSecond; // tokens per second
    private double tokens;
    private long lastRefillTime;

    public TokenBucket(int maxTokens, double refillRatePerSecond) {
        this.maxTokens = maxTokens;
        this.refillRatePerSecond = refillRatePerSecond;
        this.tokens = maxTokens;
        this.lastRefillTime = System.currentTimeMillis();
    }

    // synchronized for thread safety
    public synchronized boolean tryConsume() {
        refill();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.currentTimeMillis();
        double secondsPassed = (now - lastRefillTime) / 1000.0;
        tokens = Math.min(maxTokens, tokens + secondsPassed * refillRatePerSecond);
        lastRefillTime = now;
    }

    public synchronized int getRemainingTokens() {
        refill();
        return (int) tokens;
    }

    public long getResetTimeMillis() {
        // estimate time to refill all tokens
        return System.currentTimeMillis() + (long)((maxTokens - tokens) / refillRatePerSecond * 1000);
    }
}

public class DistributedRateLimiter {

    // clientId -> TokenBucket
    private final ConcurrentHashMap<String, TokenBucket> clients = new ConcurrentHashMap<>();
    private final int maxRequests;
    private final double refillRatePerSecond;

    public DistributedRateLimiter(int maxRequestsPerHour) {
        this.maxRequests = maxRequestsPerHour;
        this.refillRatePerSecond = maxRequestsPerHour / 3600.0;
    }

    // check if request is allowed
    public String checkRateLimit(String clientId) {
        TokenBucket bucket = clients.computeIfAbsent(clientId, id -> new TokenBucket(maxRequests, refillRatePerSecond));

        boolean allowed = bucket.tryConsume();

        if (allowed) {
            return String.format("Allowed (%d requests remaining)", bucket.getRemainingTokens());
        } else {
            long retryAfter = (bucket.getResetTimeMillis() - System.currentTimeMillis()) / 1000;
            return String.format("Denied (0 requests remaining, retry after %ds)", retryAfter);
        }
    }

    // get current status
    public String getRateLimitStatus(String clientId) {
        TokenBucket bucket = clients.get(clientId);
        if (bucket == null) {
            return String.format("{used: 0, limit: %d, reset: %d}", maxRequests, System.currentTimeMillis() / 1000 + 3600);
        }
        int used = maxRequests - bucket.getRemainingTokens();
        long reset = bucket.getResetTimeMillis() / 1000;
        return String.format("{used: %d, limit: %d, reset: %d}", used, maxRequests, reset);
    }

    // main test
    public static void main(String[] args) throws InterruptedException {
        DistributedRateLimiter limiter = new DistributedRateLimiter(10); // 10 requests/hour for testing

        String clientId = "abc123";

        for (int i = 0; i < 12; i++) {
            System.out.println(limiter.checkRateLimit(clientId));
        }

        System.out.println(limiter.getRateLimitStatus(clientId));
    }
}