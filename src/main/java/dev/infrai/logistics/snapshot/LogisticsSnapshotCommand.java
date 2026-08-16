package dev.infrai.logistics.snapshot;

import dev.infrai.logistics.config.SnapshotProperties;
import dev.infrai.logistics.snapshot.NightlySnapshotPlanner.SnapshotPlan;
import dev.infrai.logistics.snapshot.ShipmentLedger.DeliveryException;
import dev.infrai.logistics.snapshot.ShipmentLedger.ProofOfDelivery;
import dev.infrai.logistics.snapshot.ShipmentLedger.ShipmentEvent;
import dev.infrai.logistics.snapshot.ShipmentLedger.Status;
import dev.infrai.logistics.storage.InfraiException;
import dev.infrai.logistics.storage.InfraiStorageClient;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

public final class LogisticsSnapshotCommand {
    private LogisticsSnapshotCommand() {}

    public static void main(String[] args) throws Exception {
        LocalDate date = args.length == 1 ? LocalDate.parse(args[0]) : LocalDate.now(ZoneOffset.UTC).minusDays(1);
        SnapshotProperties properties = SnapshotProperties.fromEnvironment();
        InfraiStorageClient storage = new InfraiStorageClient(properties);
        SnapshotPlan plan = new NightlySnapshotPlanner().plan(date, sampleEvents(date), sampleProofs(), sampleExceptions());
        byte[] payload = SnapshotJson.write(plan.document()).getBytes(StandardCharsets.UTF_8);

        try {
            storage.createBucket(properties.bucket());
            storage.putSnapshot(properties.bucket(), plan.objectKey(), payload, "logistics-snapshot-" + date);
            System.out.printf("snapshot stored bucket=%s key=%s events=%d open_exceptions=%d%n",
                    properties.bucket(), plan.objectKey(), plan.eventCount(), plan.openExceptionCount());
        } catch (InfraiException rejected) {
            System.err.printf("snapshot rejected status=%d code=%s message=%s%n",
                    rejected.statusCode(), rejected.code(), rejected.getMessage());
            System.exit(2);
        }
    }

    private static List<ShipmentEvent> sampleEvents(LocalDate date) {
        Instant start = date.atStartOfDay().toInstant(ZoneOffset.UTC);
        return List.of(
                new ShipmentEvent("SHP-1042", start.plusSeconds(32_400), Status.DELIVERED, "PVG-02"),
                new ShipmentEvent("SHP-1043", start.plusSeconds(39_600), Status.DELIVERY_EXCEPTION, "SHA-07"));
    }

    private static List<ProofOfDelivery> sampleProofs() {
        return List.of(new ProofOfDelivery("SHP-1042", "pod/SHP-1042.pdf", "8a7e6d5c4b3a2910"));
    }

    private static List<DeliveryException> sampleExceptions() {
        return List.of(new DeliveryException("SHP-1043", "ADDRESS_REVIEW", "ops-shanghai", false));
    }
}
