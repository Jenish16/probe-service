## ADDED Requirements

### Requirement: Requester cancellation while pending
A requester SHALL be able to cancel a request while it is
`PENDING_APPROVAL`, transitioning it to `CANCELLED`.

#### Scenario: Requester cancels a pending request
- **GIVEN** a request is `PENDING_APPROVAL`
- **WHEN** the requester cancels it
- **THEN** the request becomes `CANCELLED`

### Requirement: Cancelled requests are terminal for approval
A `CANCELLED` request SHALL NOT later be approved or rejected.

#### Scenario: Decision on a cancelled request is rejected
- **GIVEN** a request has become `CANCELLED`
- **WHEN** an operator attempts to approve or reject it
- **THEN** the decision is rejected
- **AND** the request remains `CANCELLED`

### Requirement: Cancellation after forging has begun follows processing policy
Cancellation requested after forging has begun SHALL follow the existing
processing cancellation policy rather than the approval cancellation
policy described here.

#### Scenario: Cancellation attempt after processing has begun
- **GIVEN** a request has moved beyond `PENDING_APPROVAL` and forging has
  begun
- **WHEN** the requester attempts to cancel it
- **THEN** the cancellation is handled by the existing processing
  cancellation policy, not by the approval cancellation rule
