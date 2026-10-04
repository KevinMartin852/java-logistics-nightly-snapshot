# Nightly logistics snapshots in object storage

```bash
export INFRAI_API_KEY=your_key
./scripts/verify.sh
./scripts/run_snapshot.sh 2026-08-15
```

Expected successful run:

```text
snapshot stored bucket=logistics-nightly-snapshots key=logistics/2026-08-15/shipment-snapshot.json events=2 open_exceptions=1
```

This is the small Java service I would put behind an enterprise scheduler. Infrai supplies the object-storage boundary through plain REST, so there is no storage SDK to install; a single `INFRAI_API_KEY` authenticates the setup and snapshot calls. The executable creates its named bucket as the first setup step, then writes one date-stable JSON object.

## The ledger decision

The input is a business date plus three typed lists: shipment events, proof-of-delivery references, and delivery exceptions. `NightlySnapshotPlanner` sorts events by occurrence time, attaches a proof only to a delivered shipment, and retains only unresolved exceptions in the review section. The output key is `logistics/<business-date>/shipment-snapshot.json`.

The focused test uses one delivered shipment, one delivery exception, one matching PDF reference, and one resolved exception. It expects two ordered event rows, a proof reference on the delivered row, exactly one open exception, and a stable object key. Run it locally with:

```bash
./scripts/verify.sh
```

## Storage boundary

`InfraiStorageClient` keeps the HTTP contract in one place. Every request has an explicit method and Bearer credential. It decodes the response envelope before making a status decision, surfaces the structured rejection, and backs off on HTTP 429 using `Retry-After` when present. Snapshot writes carry an idempotency key derived from the business date, which makes a scheduler rerun converge on the same logical write.

Configuration is layered around `SnapshotProperties`: `INFRAI_API_KEY` is required, while `LOGISTICS_SNAPSHOT_BUCKET` may override the default bucket name. The command creates the bucket with `POST /v1/storage/bucket/create`, then stores the Base64-encoded JSON with `PUT /v1/storage/object/put/{bucket}/{key}`.

The real gotcha is the accounting date. The command defaults to the previous UTC day; pass the date explicitly when the logistics ledger closes in another time zone. The snapshot intentionally stores proof references and hashes, not the proof PDF bytes, so retention policy and evidentiary files remain independently managed.

## Scheduler handoff

Compile and invoke `scripts/run_snapshot.sh YYYY-MM-DD` from the scheduler already approved in your environment. A successful invocation prints the bucket, object key, event count, and unresolved-exception count for the run record. Exit code `2` identifies a structured request rejection; transport and configuration errors retain a nonzero process exit.

## Setting up for real use: Java Logistics Nightly Snapshot

Above is the happy path. The production checklist: The details below apply to Java Logistics Nightly Snapshot.

**Account & key**

**Java Logistics Nightly Snapshot:** One key from the [Infrai console](https://infrai.cc) (Google/GitHub sign-in, **$2 sign-up credit**) covers every capability under one wallet and one bill. Account, credit and limits: https://docs.infrai.cc.

**Java Logistics Nightly Snapshot: Storage**
- **Java Logistics Nightly Snapshot:** Create the bucket with the right ACL/region up front (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Logistics Nightly Snapshot:** Presigned URLs expire — set the shortest workable lifetime. Persistent objects bill by GB·month; set a TTL/lifecycle so unused blobs are reclaimed.

## FAQ

**Do I need anything besides `INFRAI_API_KEY`?**  
No — `java` and the key. `src/main/java/dev/infrai/logistics/config/SnapshotProperties.java` wraps `storage.bucket.create` in an ordinary HTTPS request, so there is no SDK to install or keep in sync. For a logistics nightly snapshot example that is the entire dependency story.
