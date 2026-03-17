import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

class ParkingSpot {
    enum Status { EMPTY, OCCUPIED, DELETED }

    Status status;
    String licensePlate;
    LocalDateTime entryTime;

    public ParkingSpot() {
        this.status = Status.EMPTY;
        this.licensePlate = null;
        this.entryTime = null;
    }
}

public class ParkingLotManagement {

    private final int capacity;
    private final ParkingSpot[] spots;
    private int totalProbes = 0;
    private int totalParkings = 0;
    private final Map<Integer, Integer> hourlyOccupancy = new HashMap<>();

    public ParkingLotManagement(int capacity) {
        this.capacity = capacity;
        this.spots = new ParkingSpot[capacity];
        for (int i = 0; i < capacity; i++) spots[i] = new ParkingSpot();
    }

    // Simple hash function based on license plate
    private int hash(String licensePlate) {
        return Math.abs(licensePlate.hashCode()) % capacity;
    }

    // Park vehicle using linear probing
    public void parkVehicle(String licensePlate) {
        int preferred = hash(licensePlate);
        int probe = 0;
        int spotIndex = preferred;

        while (spots[spotIndex].status == ParkingSpot.Status.OCCUPIED) {
            probe++;
            spotIndex = (preferred + probe) % capacity;
            if (probe >= capacity) {
                System.out.println("Parking lot full!");
                return;
            }
        }

        spots[spotIndex].status = ParkingSpot.Status.OCCUPIED;
        spots[spotIndex].licensePlate = licensePlate;
        spots[spotIndex].entryTime = LocalDateTime.now();

        totalProbes += probe;
        totalParkings++;

        int hour = spots[spotIndex].entryTime.getHour();
        hourlyOccupancy.put(hour, hourlyOccupancy.getOrDefault(hour, 0) + 1);

        System.out.printf("parkVehicle(\"%s\") → Assigned spot #%d (%d probe%s)%n",
                licensePlate, spotIndex, probe, probe != 1 ? "s" : "");
    }

    // Exit vehicle and calculate duration & fee
    public void exitVehicle(String licensePlate) {
        for (int i = 0; i < capacity; i++) {
            if (spots[i].status == ParkingSpot.Status.OCCUPIED && licensePlate.equals(spots[i].licensePlate)) {
                LocalDateTime exitTime = LocalDateTime.now();
                Duration duration = Duration.between(spots[i].entryTime, exitTime);
                double hours = duration.toMinutes() / 60.0;
                double fee = hours * 5; // $5 per hour

                System.out.printf("exitVehicle(\"%s\") → Spot #%d freed, Duration: %dh %dm, Fee: $%.2f%n",
                        licensePlate, i, duration.toHours(), duration.toMinutesPart(), fee);

                spots[i].status = ParkingSpot.Status.EMPTY;
                spots[i].licensePlate = null;
                spots[i].entryTime = null;
                return;
            }
        }
        System.out.println("Vehicle not found in parking lot!");
    }

    // Generate statistics
    public void getStatistics() {
        long occupied = Arrays.stream(spots).filter(s -> s.status == ParkingSpot.Status.OCCUPIED).count();
        double avgProbes = totalParkings == 0 ? 0 : (double) totalProbes / totalParkings;

        int peakHour = hourlyOccupancy.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        System.out.printf("Occupancy: %.1f%%, Avg Probes: %.2f, Peak Hour: %s%n",
                occupied * 100.0 / capacity, avgProbes,
                peakHour >= 0 ? peakHour + ":00-" + (peakHour + 1) + ":00" : "N/A");
    }

    // Test simulation
    public static void main(String[] args) throws InterruptedException {
        ParkingLotManagement lot = new ParkingLotManagement(500);

        lot.parkVehicle("ABC-1234");
        Thread.sleep(1000);
        lot.parkVehicle("ABC-1235");
        Thread.sleep(1000);
        lot.parkVehicle("XYZ-9999");
        Thread.sleep(1000);

        lot.exitVehicle("ABC-1234");
        lot.getStatistics();
    }
}