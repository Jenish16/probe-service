## ADDED Requirements

### Requirement: Approval window expiry
A pending approval SHALL expire 30 minutes after the request's submission
time if no operator decision has been made by then, transitioning the
request to `EXPIRED`.

#### Scenario: Pending request expires after 30 minutes
- **GIVEN** a request is `PENDING_APPROVAL` with no operator decision
- **WHEN** 30 minutes elapse from its submission time
- **THEN** the request becomes `EXPIRED`

### Requirement: Expired requests do not process
An `EXPIRED` request SHALL NOT begin processing.

#### Scenario: Expired request does not begin forging
- **GIVEN** a request has become `EXPIRED`
- **WHEN** the request's processing state is checked
- **THEN** no forging work has begun and none begins afterward

### Requirement: Decisions after expiry are rejected
A decision received after a request has expired SHALL be rejected.

#### Scenario: Late approval after expiry is rejected
- **GIVEN** a request has become `EXPIRED`
- **WHEN** an operator attempts to approve or reject it
- **THEN** the decision is rejected
- **AND** the request remains `EXPIRED`

### Requirement: New attempt required after expiry
Because an `EXPIRED` request cannot proceed, the requester SHALL submit a
new attempt if processing is still required.

#### Scenario: Requester resubmits after expiry
- **GIVEN** a request has become `EXPIRED`
- **WHEN** the requester still needs the artifact processed
- **THEN** the requester submits a new request rather than reusing the
  expired one
