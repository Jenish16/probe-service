## ADDED Requirements

### Requirement: Pending request listing
An authorized Forge operator SHALL be able to list high-power requests
awaiting review, ordered oldest submission first. Each listed request
SHALL show the requester, artifact type, material, power level, submission
time, and approval expiry time.

#### Scenario: Operator lists pending requests oldest first
- **WHEN** an operator requests the list of pending high-power requests
- **THEN** the requests are returned ordered oldest submission time first
- **AND** each entry shows requester, artifact type, material, power
  level, submission time, and approval expiry time

### Requirement: Approval decision
An operator SHALL be able to approve a pending request, making it eligible
to begin processing.

#### Scenario: Operator approves a pending request
- **WHEN** an operator approves a request that is `PENDING_APPROVAL`
- **THEN** the request becomes `APPROVED`
- **AND** the request is eligible to begin processing

### Requirement: Rejection decision requires a reason
An operator SHALL be able to reject a pending request only after supplying
a reason for the rejection.

#### Scenario: Operator rejects a pending request with a reason
- **WHEN** an operator rejects a request that is `PENDING_APPROVAL` and
  supplies a reason
- **THEN** the request becomes `REJECTED`
- **AND** no forging work begins for that request

#### Scenario: Rejection without a reason is not accepted
- **WHEN** an operator attempts to reject a pending request without
  supplying a reason
- **THEN** the rejection is not accepted
- **AND** the request remains `PENDING_APPROVAL`

### Requirement: Single effective terminal decision
A request SHALL receive only one effective terminal approval decision.
Repeating the same approval or rejection decision on a request that
already carries that terminal decision SHALL be safe and SHALL NOT change
the outcome. A competing decision submitted after a request already has a
terminal decision SHALL be rejected.

#### Scenario: Repeating the same decision is safe
- **WHEN** an operator submits the same approval (or the same rejection
  with the same reason) that already resulted in a request's current
  terminal state
- **THEN** the request's state is unchanged
- **AND** no error results from the repeated decision

#### Scenario: Competing decision after a terminal decision is rejected
- **WHEN** an operator submits a decision that conflicts with a request's
  existing terminal decision (for example, approving an already-rejected
  request)
- **THEN** the competing decision is rejected
- **AND** the request's existing terminal state is unchanged
