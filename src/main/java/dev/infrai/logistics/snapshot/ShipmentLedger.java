package dev.infrai.logistics.snapshot;

import java.time.Instant;

public final class ShipmentLedger {
    private ShipmentLedger() {}

    public record ShipmentEvent(String shipmentId, Instant occurredAt, Status status, String facility) {}
    public record ProofOfDelivery(String shipmentId, String objectKey, String sha256) {}
    public record DeliveryException(String shipmentId, String category, String owner, boolean resolved) {}

    public enum Status {
        IN_TRANSIT,
        DELIVERED,
        DELIVERY_EXCEPTION
    }
}
