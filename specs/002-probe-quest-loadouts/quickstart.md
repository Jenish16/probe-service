# Quickstart: Quest Loadout Fulfilment (probe-service)

Manual validation flow once implementation lands. Assumes `forge-service` is
running locally on `localhost:8081` (REST) / `localhost:9091` (gRPC) with its
own `/api/v1/loadouts/*` and `LoadoutService` gRPC support already
implemented, and `probe-service` on `localhost:8082` (`application.yml`).

## 1. Create a loadout (REST downstream)

```bash
curl -X POST http://localhost:8082/api/v1/probe-requests/loadouts/rest \
  -H "Content-Type: application/json" \
  -d '{
    "requestReference": "quickstart-loadout-1",
    "loadoutName": "Starter Kit",
    "requestedBy": "quest-leader-1",
    "items": [
      {"artifactName": "Iron Shield", "artifactType": "SHIELD", "material": "DWARVEN_IRON", "powerLevel": 3},
      {"artifactName": "Mithril Blade", "artifactType": "BLADE", "material": "MITHRIL", "powerLevel": 7}
    ]
  }'
```

Expect `201 Created`, `status: "ACCEPTED"`, two items each with a
`loadoutItemId`, `rejectedItems` omitted/empty, `transport: "REST"`.

## 2. Repeat the same request (idempotency, FR-008)

Resubmit the identical body. Expect `200 OK` with the same `loadoutId` and
`loadoutItemId`s as step 1 — no duplicate loadout is created.

## 3. Resubmit with different content, same reference (FR-009)

Resubmit with the same `requestReference` but a different `loadoutName` or
item list. Expect `409 Conflict`.

## 4. Retrieve the loadout (gRPC downstream, User Story 2)

```bash
curl http://localhost:8082/api/v1/probe-requests/loadouts/{loadoutId}/grpc
```

Expect `200 OK`, `transport: "GRPC"`, and `status` reflecting current
progress (`IN_PROGRESS`, `READY`, `PARTIALLY_READY`, or `FAILED` —
FR-011).

## 5. Inspect a failed item's attempt history (User Story 3)

Once an item shows `status: "FAILED"` in step 4's response:

```bash
curl http://localhost:8082/api/v1/probe-requests/loadouts/{loadoutId}/items/{loadoutItemId}/attempts/rest
```

Expect one attempt with a `failureReason`.

## 6. Retry the failed item (User Story 5)

```bash
curl -X POST http://localhost:8082/api/v1/probe-requests/loadouts/{loadoutId}/retry/rest \
  -H "Content-Type: application/json" \
  -d '{"items": [{"loadoutItemId": "{loadoutItemId}", "powerLevel": 4}]}'
```

Expect `200 OK`. Re-run step 5 and confirm both the original failed attempt
and the new attempt are visible, in order (FR-017).

## 7. Cancel the loadout (User Story 4)

```bash
curl -X POST http://localhost:8082/api/v1/probe-requests/loadouts/{loadoutId}/cancel/rest
```

Expect `200 OK`. Re-run step 4: any item that was still outstanding now
shows `status: "CANCELLED"`; items that already completed or failed retain
their prior results unchanged (FR-013). Repeat the cancel call and confirm
it is a no-op (FR-014).

## 8. Confirm existing single-artifact flow is unaffected (FR-019)

```bash
curl -X POST http://localhost:8082/api/v1/probe-requests/forge-jobs/rest \
  -H "Content-Type: application/json" \
  -d '{"artifactName": "Test Ring", "artifactType": "RING", "material": "SILVERWOOD", "requestedBy": "quest-leader-1", "powerLevel": 2}'
```

Expect the existing `ProbeForgeJobResponse` shape, unchanged by this
feature.
