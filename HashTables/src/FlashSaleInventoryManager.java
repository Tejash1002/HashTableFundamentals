import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class FlashSaleInventoryManager {

    // productId -> stock count
    private ConcurrentHashMap<String, AtomicInteger> inventory;

    // productId -> waiting list (FIFO)
    private ConcurrentHashMap<String, Queue<Integer>> waitingList;

    public FlashSaleInventoryManager() {
        inventory = new ConcurrentHashMap<>();
        waitingList = new ConcurrentHashMap<>();
    }

    // Initialize product stock
    public void addProduct(String productId, int stock) {
        inventory.put(productId, new AtomicInteger(stock));
        waitingList.put(productId, new ConcurrentLinkedQueue<>());
    }

    // Check stock in O(1)
    public String checkStock(String productId) {
        AtomicInteger stock = inventory.get(productId);
        if (stock == null) return "Product not found";

        return stock.get() + " units available";
    }

    // Purchase item (thread-safe, O(1))
    public String purchaseItem(String productId, int userId) {
        AtomicInteger stock = inventory.get(productId);

        if (stock == null) {
            return "Product not found";
        }

        // Atomic decrement
        while (true) {
            int currentStock = stock.get();

            if (currentStock <= 0) {
                // Add to waiting list (FIFO)
                Queue<Integer> queue = waitingList.get(productId);
                queue.add(userId);
                return "Added to waiting list, position #" + queue.size();
            }

            // Try CAS (Compare-And-Set)
            if (stock.compareAndSet(currentStock, currentStock - 1)) {
                return "Success, " + (currentStock - 1) + " units remaining";
            }
        }
    }

    // Get waiting list for debugging
    public List<Integer> getWaitingList(String productId) {
        return new ArrayList<>(waitingList.get(productId));
    }

    // Test simulation
    public static void main(String[] args) {
        FlashSaleInventoryManager manager = new FlashSaleInventoryManager();

        manager.addProduct("IPHONE15_256GB", 100);

        // Check stock
        System.out.println("checkStock → " + manager.checkStock("IPHONE15_256GB"));

        // Simulate purchases
        for (int i = 1; i <= 102; i++) {
            System.out.println("User " + i + ": " +
                    manager.purchaseItem("IPHONE15_256GB", i));
        }
    }
}