## Why

High-power artifacts (power level 8–10) carry greater material cost and
operational risk but currently flow through the same unreviewed path as
ordinary artifacts. The product needs an explicit Forge-operator approval
step before a high-power request begins forging, while ordinary requests
(power level 1–7) keep their existing experience unchanged.

## What Changes

- Introduce an approval lifecycle (`PENDING_APPROVAL`, `APPROVED`,
  `REJECTED`, `EXPIRED`, `CANCELLED`) that gates forging for power levels
  8, 9, and 10 only; power levels 1–7 are unaffected.
- Add support for the required user operations: submit a request, retrieve
  its processing/approval state, list pending high-power requests oldest
  first, approve a pending request, reject a pending request with a reason,
  cancel a pending request, and retrieve decision history.
- Enforce idempotent submission: repeating the same requester reference
  with identical content returns the existing request; reusing it with
  different content is rejected.
- Enforce a single effective terminal decision per request: repeating the
  same approval/rejection is safe, a competing decision after a terminal
  decision is rejected, and decisions received after a 30-minute approval
  window (expiry) are rejected.
- Support requester cancellation while approval is pending; cancellation
  after forging has begun follows the existing processing cancellation
  policy instead.
- Ensure retrying or resubmitting a rejected/expired/failed high-power
  artifact creates a new approval attempt rather than reusing a prior
  decision, while linking decision history across attempts that share a
  requester reference or original request.
- Preserve visibility of both approval status and processing status to
  requesters, including rejection reasons and expiry times, and make
  decision history retrievable by request reference for support operators.
- Preserve consistent approval behavior across both REST and gRPC
  integrations, and ensure existing clients can distinguish a pending
  approval from a job that has begun processing.

This proposal defines end-to-end product behavior only. It intentionally
does not assign which participating system (Probe, Forge, or another
component) owns which operation, data, or state transition — that
ownership is deferred to the Technical Spec.

## Capabilities

### New Capabilities

- `high-power-approval-submission`: Submission-time applicability (power
  level 8–10 requires approval, 1–7 does not), idempotent request
  references, and the rule that no forging work begins while approval is
  pending.
- `high-power-approval-operator-review`: Listing pending requests oldest
  first with the required fields, approving/rejecting a pending request,
  reason-required rejection, and single-terminal-decision safety
  (idempotent repeats, rejected competing decisions).
- `high-power-approval-expiry`: The 30-minute pending-approval window,
  transition to `EXPIRED`, and rejection of decisions received after
  expiry.
- `high-power-approval-cancellation`: Requester cancellation while
  pending, the resulting `CANCELLED` terminal state, and the handoff to
  the normal processing cancellation policy once forging has begun.
- `high-power-approval-retry`: New approval attempts on retry/resubmission
  of a rejected, expired, or failed high-power artifact, non-inheritance
  of a prior attempt's approval, and decision-history linkage across
  attempts sharing a requester reference or original request.
- `high-power-approval-visibility-audit`: Requester-facing approval and
  processing status (including rejection reason and expiry time) and
  support-operator retrieval of full decision history (operator, decision,
  reason, timestamp) by request reference.

### Modified Capabilities

- None. This is a net-new safety/governance capability; no existing
  `openspec/specs/` capability requirements change.

## Impact

- **Affected product behavior**: Every artifact submission with power
  level 8–10, across both REST and gRPC integrations; ordinary (1–7)
  submissions are unaffected.
- **Affected users**: Artifact requesters, Forge operators, and support
  operators, per the required user operations above.
- **Participating systems**: Forge, Probe, and Forge operators, as named
  in the Functional Spec. Which system implements which operation, and any
  new APIs, data stores, or contracts required, are Technical Spec
  decisions and are explicitly out of scope here.
- **Out of scope (per Functional Spec)**: operator role provisioning,
  notifications/reminders, material inventory checks, pricing/quota
  enforcement, persistent storage requirements, and choice of approval
  owner or technical implementation.
