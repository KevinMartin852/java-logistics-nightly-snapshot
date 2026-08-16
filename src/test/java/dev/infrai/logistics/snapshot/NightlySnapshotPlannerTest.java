package dev.infrai.logistics.snapshot;

import dev.infrai.logistics.snapshot.NightlySnapshotPlanner.SnapshotPlan;
import dev.infrai.logistics.snapshot.ShipmentLedger.DeliveryException;
import dev.infrai.logistics.snapshot.ShipmentLedger.ProofOfDelivery;
import dev.infrai.logistics.snapshot.ShipmentLedger.ShipmentEvent;
import dev.infrai.logistics.snapshot.ShipmentLedger.Status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class NightlySnapshotPlannerTest {
    public static void main(String[] args) {
        NightlySnapshotPlanner planner = new NightlySnapshotPlanner();
        SnapshotPlan plan = planner.plan(
                LocalDate.parse("2026-08-15"),
                List.of(
                        new ShipmentEvent("SHP-2", Instant.parse("2026-08-15T02:00:00Z"), Status.DELIVERY_EXCEPTION, "SHA-07"),
                        new ShipmentEvent("SHP-1", Instant.parse("2026-08-15T01:00:00Z"), Status.DELIVERED, "PVG-02")),
                List.of(new ProofOfDelivery("SHP-1", "pod/SHP-1.pdf", "abc123")),
                List.of(
                        new DeliveryException("SHP-2", "ADDRESS_REVIEW", "ops", false),
                        new DeliveryException("SHP-9", "CLOSED", "ops", true)));

        check(plan.eventCount() == 2, "all shipment events must be retained");
        check(plan.openExceptionCount() == 1, "only unresolved exceptions must be reported");
        List<?> rows = (List<?>) plan.document().get("shipment_events");
        Map<?, ?> delivered = (Map<?, ?>) rows.get(0);
        check("SHP-1".equals(delivered.get("shipment_id")), "events must be ordered by occurrence time");
        check(delivered.containsKey("proof_of_delivery"), "delivered shipment must carry its proof reference");
        Map<?, ?> exception = (Map<?, ?>) rows.get(1);
        check(!exception.containsKey("proof_of_delivery"), "exception event must not claim delivery proof");
        check("logistics/2026-08-15/shipment-snapshot.json".equals(plan.objectKey()), "object key must be date stable");
        System.out.println("NightlySnapshotPlannerTest passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
