# Functional Spec: High-Power Artifact Approval

## Metadata

| Field | Value |
|---|---|
| Initiative ID | `POC-HIGH-POWER-APPROVAL` |
| Product area | Forge safety and governance |
| Known participating systems | Forge, Probe, and Forge operators |
| Status | Functional approved |

## Background

Artifacts with a high power level carry greater material cost and operational
risk. They are currently processed using the same flow as ordinary artifacts,
without an explicit review by a Forge operator.

The product needs a safety approval step for high-power artifacts while
preserving the existing experience for ordinary requests.

## Product outcome

Every artifact request with power level 8, 9, or 10 is reviewed by an
authorized Forge operator before processing begins. Requesters can track the
decision, cancel a pending request, and understand why a request was rejected
or expired.

## Users

- **Artifact requester:** submits an artifact and tracks its approval.
- **Forge operator:** reviews high-power requests.
- **Support operator:** audits decisions and resolves requester queries.

## Required user operations

The product must support operations to:

1. Submit an artifact request.
2. Retrieve its processing and approval state.
3. List high-power requests awaiting review.
4. Approve a pending request.
5. Reject a pending request with a reason.
6. Cancel a pending request.
7. Retrieve the approval decision history.

The Technical Spec will define the APIs, authorization integration, and
participating-service contracts needed to provide these operations.

## Approval applicability

- Power levels 1 through 7 continue through the existing processing flow.
- Power levels 8 through 10 require approval.
- Approval is evaluated for every newly submitted processing attempt.
- A high-power request shall not begin forging before approval.

## Approval lifecycle

| Status | Meaning |
|---|---|
| `PENDING_APPROVAL` | Waiting for an operator decision |
| `APPROVED` | Approved and eligible to begin processing |
| `REJECTED` | Rejected by an operator |
| `EXPIRED` | No decision was made within the approval window |
| `CANCELLED` | Cancelled by the requester before a decision |

After approval, the artifact continues through the existing forge-job
lifecycle. Approval state and forge-job state must both remain visible to
requesters and support operators.

## Functional requirements

### Submission

1. Requests with power level 8, 9, or 10 shall enter
   `PENDING_APPROVAL`.
2. No forging work shall begin while approval is pending.
3. Ordinary requests with power level 1 through 7 shall not require approval.
4. The requester shall receive a stable request reference immediately.
5. Repeating the same requester reference with identical content shall return
   the existing request.
6. Reusing a requester reference with different content shall be rejected.

### Operator review

7. An operator shall be able to list pending requests oldest first.
8. The pending list shall show requester, artifact type, material, power level,
   submission time, and approval expiry time.
9. An operator may approve a pending request.
10. An operator may reject a pending request only after supplying a reason.
11. A request may receive only one effective terminal approval decision.
12. Repeating the same approval or rejection shall be safe.
13. A competing decision after a terminal decision shall be rejected.

### Expiry

14. A pending approval expires 30 minutes after submission.
15. An expired request shall not begin processing.
16. A decision received after expiry shall be rejected.
17. The requester must submit a new attempt if processing is still required.

### Requester cancellation

18. A requester may cancel while approval is pending.
19. A cancelled request cannot later be approved or rejected.
20. Cancellation after forging has begun shall follow the normal processing
    cancellation policy rather than the approval policy.

### Retry and resubmission

21. Retrying or resubmitting a rejected, expired, or failed high-power artifact
    creates a new approval attempt.
22. Approval from a previous attempt shall not automatically authorize a new
    attempt.
23. Decision history shall link all attempts that share the same requester
    reference or original request.

### Visibility and audit

24. Requesters shall see both approval status and processing status.
25. Rejected requests shall show the rejection reason.
26. Expired requests shall show the expiry time.
27. Decision history shall record the operator, decision, reason when
    applicable, and timestamp.
28. Support operators shall be able to retrieve history by request reference.

## Acceptance scenarios

### Ordinary artifact

- **Given** an artifact with power level 7
- **When** it is submitted
- **Then** it follows the existing processing flow without approval

### High-power approval

- **Given** an artifact with power level 9
- **When** it is submitted
- **Then** it remains `PENDING_APPROVAL`
- **And** processing begins only after operator approval

### Rejection

- **Given** a pending request
- **When** an operator rejects it with a reason
- **Then** no forging work begins
- **And** the requester can see the reason

### Approval expiry

- **Given** a pending request with no decision
- **When** 30 minutes pass
- **Then** it becomes `EXPIRED`
- **And** a later approval is rejected

### Requester cancellation

- **Given** a request awaiting approval
- **When** the requester cancels it
- **Then** it becomes `CANCELLED`
- **And** it can no longer receive an operator decision

### New attempt

- **Given** a rejected or failed high-power request
- **When** the requester tries again
- **Then** a new approval attempt is created
- **And** the previous approval decision is retained only as history

## Compatibility expectations

- Existing low-power artifact behavior remains unchanged.
- Approval behavior must be consistent for requests originating through REST
  and gRPC integrations.
- Existing clients must be able to distinguish a pending approval from a job
  that has begun processing.

## Success measures

- No high-power artifact begins forging without an approval.
- Operators can review and decide pending requests through explicit product
  operations.
- Requesters and support teams can retrieve complete decision history.
- Duplicate decisions and submissions do not create duplicate processing.

## Out of scope

- Operator role provisioning
- Notifications and reminders
- Material inventory checks
- Pricing or quota enforcement
- Persistent storage requirements
- Choice of approval owner or technical implementation
