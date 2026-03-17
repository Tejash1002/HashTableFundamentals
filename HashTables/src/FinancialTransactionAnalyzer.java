import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.Duration;
import java.util.*;

class Transaction {
    int id;
    double amount;
    String merchant;
    String account;
    LocalDateTime time;

    public Transaction(int id, double amount, String merchant, String account, String timeStr) {
        this.id = id;
        this.amount = amount;
        this.merchant = merchant;
        this.account = account;
        this.time = LocalDateTime.parse(timeStr, DateTimeFormatter.ofPattern("HH:mm"));
    }

    @Override
    public String toString() {
        return String.format("id:%d", id);
    }
}

public class FinancialTransactionAnalyzer {

    private final List<Transaction> transactions;

    public FinancialTransactionAnalyzer(List<Transaction> transactions) {
        this.transactions = transactions;
    }

    // Classic Two-Sum
    public List<List<Transaction>> findTwoSum(double target) {
        Map<Double, Transaction> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                result.add(Arrays.asList(map.get(complement), t));
            }
            map.put(t.amount, t);
        }
        return result;
    }

    // Two-Sum within a time window (in minutes)
    public List<List<Transaction>> findTwoSumWithinWindow(double target, long minutesWindow) {
        Map<Double, List<Transaction>> map = new HashMap<>();
        List<List<Transaction>> result = new ArrayList<>();

        for (Transaction t : transactions) {
            double complement = target - t.amount;
            if (map.containsKey(complement)) {
                for (Transaction comp : map.get(complement)) {
                    if (Math.abs(Duration.between(t.time, comp.time).toMinutes()) <= minutesWindow) {
                        result.add(Arrays.asList(comp, t));
                    }
                }
            }
            map.computeIfAbsent(t.amount, k -> new ArrayList<>()).add(t);
        }
        return result;
    }

    // K-Sum
    public List<List<Transaction>> findKSum(int k, double target) {
        List<List<Transaction>> result = new ArrayList<>();
        findKSumHelper(transactions, 0, k, target, new ArrayList<>(), result);
        return result;
    }

    private void findKSumHelper(List<Transaction> txs, int index, int k, double target,
                                List<Transaction> current, List<List<Transaction>> result) {
        if (k == 0) {
            if (Math.abs(target) < 0.0001) {
                result.add(new ArrayList<>(current));
            }
            return;
        }
        for (int i = index; i < txs.size(); i++) {
            current.add(txs.get(i));
            findKSumHelper(txs, i + 1, k - 1, target - txs.get(i).amount, current, result);
            current.remove(current.size() - 1);
        }
    }

    // Detect duplicates (same amount, same merchant, different accounts)
    public List<Map<String, Object>> detectDuplicates() {
        Map<String, List<Transaction>> map = new HashMap<>();
        for (Transaction t : transactions) {
            String key = t.amount + "|" + t.merchant;
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Transaction>> entry : map.entrySet()) {
            Set<String> accounts = new HashSet<>();
            for (Transaction t : entry.getValue()) accounts.add(t.account);
            if (accounts.size() > 1) {
                Map<String, Object> dup = new HashMap<>();
                String[] parts = entry.getKey().split("\\|");
                dup.put("amount", Double.parseDouble(parts[0]));
                dup.put("merchant", parts[1]);
                dup.put("accounts", accounts);
                duplicates.add(dup);
            }
        }
        return duplicates;
    }

    // Test
    public static void main(String[] args) {
        List<Transaction> txs = Arrays.asList(
                new Transaction(1, 500, "Store A", "acc1", "10:00"),
                new Transaction(2, 300, "Store B", "acc2", "10:15"),
                new Transaction(3, 200, "Store C", "acc3", "10:30"),
                new Transaction(4, 500, "Store A", "acc2", "10:45")
        );

        FinancialTransactionAnalyzer analyzer = new FinancialTransactionAnalyzer(txs);

        System.out.println("findTwoSum(target=500) → " + analyzer.findTwoSum(500));
        System.out.println("findTwoSumWithinWindow(target=500, 60min) → " + analyzer.findTwoSumWithinWindow(500, 60));
        System.out.println("findKSum(k=3, target=1000) → " + analyzer.findKSum(3, 1000));
        System.out.println("detectDuplicates() → " + analyzer.detectDuplicates());
    }
}