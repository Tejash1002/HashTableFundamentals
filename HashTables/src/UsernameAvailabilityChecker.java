import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class UsernameAvailabilityChecker {

    // username -> userId
    private ConcurrentHashMap<String, Integer> users;

    // username -> attempt count
    private ConcurrentHashMap<String, Integer> attempts;

    private AtomicInteger userIdGenerator;

    public UsernameAvailabilityChecker() {
        users = new ConcurrentHashMap<>();
        attempts = new ConcurrentHashMap<>();
        userIdGenerator = new AtomicInteger(1);
    }

    // Register a username
    public boolean register(String username) {
        if (users.containsKey(username)) {
            return false;
        }
        users.put(username, userIdGenerator.getAndIncrement());
        return true;
    }

    // Check availability in O(1)
    public boolean checkAvailability(String username) {
        attempts.merge(username, 1, Integer::sum);
        return !users.containsKey(username);
    }

    // Suggest alternatives
    public List<String> suggestAlternatives(String username) {
        List<String> suggestions = new ArrayList<>();

        // Append numbers
        for (int i = 1; i <= 5; i++) {
            String candidate = username + i;
            if (!users.containsKey(candidate)) {
                suggestions.add(candidate);
            }
        }

        // Replace "_" with "."
        if (username.contains("_")) {
            String modified = username.replace("_", ".");
            if (!users.containsKey(modified)) {
                suggestions.add(modified);
            }
        }

        return suggestions;
    }

    // Get most attempted username
    public String getMostAttempted() {
        String maxUser = null;
        int maxCount = 0;

        for (Map.Entry<String, Integer> entry : attempts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                maxUser = entry.getKey();
            }
        }

        return maxUser + " (" + maxCount + " attempts)";
    }

    // Main method for testing
    public static void main(String[] args) {
        UsernameAvailabilityChecker checker = new UsernameAvailabilityChecker();

        checker.register("john_doe");
        checker.register("admin");

        System.out.println("checkAvailability(\"john_doe\") → "
                + checker.checkAvailability("john_doe"));

        System.out.println("checkAvailability(\"jane_smith\") → "
                + checker.checkAvailability("jane_smith"));

        System.out.println("suggestAlternatives(\"john_doe\") → "
                + checker.suggestAlternatives("john_doe"));

        for (int i = 0; i < 10543; i++) {
            checker.checkAvailability("admin");
        }

        System.out.println("getMostAttempted() → "
                + checker.getMostAttempted());
    }
}