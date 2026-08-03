## ADDED Requirements

### Requirement: Combined approval and processing status visibility
Requesters SHALL be able to see both the approval status and the
processing status of their request, and existing clients SHALL be able to
distinguish a pending approval from a job that has begun processing.

#### Scenario: Requester retrieves combined status
- **WHEN** a requester retrieves the state of their request
- **THEN** both the approval status and the processing status are visible
- **AND** a `PENDING_APPROVAL` state is distinguishable from a state where
  processing has begun

### Requirement: Rejection reason visibility
A `REJECTED` request SHALL show the rejection reason to the requester.

#### Scenario: Requester sees rejection reason
- **GIVEN** a request is `REJECTED`
- **WHEN** the requester retrieves its state
- **THEN** the rejection reason supplied by the operator is visible

### Requirement: Expiry time visibility
An `EXPIRED` request SHALL show its expiry time to the requester.

#### Scenario: Requester sees expiry time
- **GIVEN** a request is `EXPIRED`
- **WHEN** the requester retrieves its state
- **THEN** the time at which it expired is visible

### Requirement: Decision history recording
Every approval decision SHALL be recorded in decision history with the
deciding operator, the decision, the reason when applicable, and the
decision timestamp.

#### Scenario: Decision is recorded with full context
- **WHEN** an operator approves or rejects a pending request
- **THEN** the decision history records the operator, the decision, the
  reason (for rejections), and the timestamp of the decision

### Requirement: Support retrieval of decision history
A support operator SHALL be able to retrieve the complete decision history
for a request by its request reference.

#### Scenario: Support operator retrieves history by reference
- **WHEN** a support operator looks up a request reference
- **THEN** the complete decision history for that request reference is
  returned, including all linked attempts

### Requirement: Consistent approval behavior across integrations
Approval behavior SHALL be consistent for requests originating through
REST and gRPC integrations.

#### Scenario: REST and gRPC submissions behave consistently
- **GIVEN** two otherwise identical high-power requests, one submitted via
  REST and one via gRPC
- **WHEN** each is submitted and later reviewed
- **THEN** both follow the same approval lifecycle, applicability rules,
  and visibility guarantees
