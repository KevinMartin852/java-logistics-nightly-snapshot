package dev.infrai.logistics.snapshot;

import dev.infrai.logistics.snapshot.ShipmentLedger.DeliveryException;
import dev.infrai.logistics.snapshot.ShipmentLedger.ProofOfDelivery;
import dev.infrai.logistics.snapshot.ShipmentLedger.ShipmentEvent;
import dev.infrai.logistics.snapshot.ShipmentLedger.Status;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class NightlySnapshotPlanner {
    public SnapshotPlan plan(
            LocalDate businessDate,
            List<ShipmentEvent> events,
            List<ProofOfDelivery> proofs,
            List<DeliveryException> exceptions) {
        Map<String, ProofOfDelivery> proofByShipment = proofs.stream()
                .collect(Collectors.toMap(ProofOfDelivery::shipmentId, Function.identity()));

        List<Map<String, Object>> eventRows = events.stream()
                .sorted(Comparator.comparing(ShipmentEvent::occurredAt).thenComparing(ShipmentEvent::shipmentId))
                .map(event -> eventRow(event, proofByShipment.get(event.shipmentId())))
                .toList();

        List<Map<String, Object>> openExceptions = exceptions.stream()
                .filter(item -> !item.resolved())
                .sorted(Comparator.comparing(DeliveryException::shipmentId))
                .map(this::exceptionRow)
                .toList();

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("business_date", businessDate.toString());
        document.put("shipment_events", eventRows);
        document.put("open_exceptions", openExceptions);
        return new SnapshotPlan("logistics/" + businessDate + "/shipment-snapshot.json", document,
                eventRows.size(), openExceptions.size());
    }

    private Map<String, Object> eventRow(ShipmentEvent event, ProofOfDelivery proof) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("shipment_id", event.shipmentId());
        row.put("occurred_at", event.occurredAt().toString());
        row.put("status", event.status().name());
        row.put("facility", event.facility());
        if (event.status() == Status.DELIVERED && proof != null) {
            row.put("proof_of_delivery", Map.of("object_key", proof.objectKey(), "sha256", proof.sha256()));
        }
        return row;
    }

    private Map<String, Object> exceptionRow(DeliveryException item) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("shipment_id", item.shipmentId());
        row.put("category", item.category());
        row.put("owner", item.owner());
        return row;
    }

    public record SnapshotPlan(String objectKey, Map<String, Object> document, int eventCount, int openExceptionCount) {}
}
