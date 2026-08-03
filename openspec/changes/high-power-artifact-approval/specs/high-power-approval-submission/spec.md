## ADDED Requirements

### Requirement: Power level determines approval applicability
Every newly submitted artifact request with power level 8, 9, or 10 SHALL
enter the `PENDING_APPROVAL` approval state, and no forging work SHALL
begin while that request remains pending. Every newly submitted artifact
request with power level 1 through 7 SHALL NOT require approval and SHALL
continue through the existing processing flow unchanged.

#### Scenario: High-power request enters pending approval
- **WHEN** an artifact request with power level 9 is submitted
- **THEN** the request remains in `PENDING_APPROVAL`
- **AND** no forging work begins for that request

#### Scenario: Ordinary request bypasses approval
- **WHEN** an artifact request with power level 7 is submitted
- **THEN** the request follows the existing processing flow
- **AND** no approval state is required before processing begins

### Requirement: Stable request reference on submission
The system SHALL return a stable request reference to the requester
immediately upon submission, regardless of power level.

#### Scenario: Immediate reference on submission
- **WHEN** a requester submits any artifact request
- **THEN** the requester receives a stable request reference immediately

### Requirement: Idempotent submission by requester reference
Idempotent submission SHALL apply to every requester reference regardless
of the submitted power level. Repeating a submission with the same
requester reference and identical request content SHALL return the
existing request rather than creating a new one, but only while that
existing request has not yet reached a terminal outcome. Reusing a
requester reference with different request content while the existing
request has not yet reached a terminal outcome SHALL be rejected. Once an
existing request has reached a terminal outcome (`REJECTED`, `EXPIRED`,
`CANCELLED`, or a terminal processing failure), idempotent
duplicate-return no longer applies to further submissions using that
requester reference; see the retry capability for the behavior that
applies at that point.

#### Scenario: Duplicate submission returns existing request
- **GIVEN** a prior submission is not yet in a terminal outcome
- **WHEN** a requester resubmits using the same requester reference and
  identical request content
- **THEN** the existing request is returned
- **AND** no new request is created

#### Scenario: Conflicting reuse of requester reference is rejected
- **GIVEN** a prior submission is not yet in a terminal outcome
- **WHEN** a requester resubmits using the same requester reference but
  different request content
- **THEN** the resubmission is rejected

#### Scenario: Idempotency applies regardless of power level
- **WHEN** a requester resubmits an ordinary (power level 1–7) request
  using the same requester reference and identical content as a prior,
  not-yet-terminal submission
- **THEN** the existing request is returned, following the same
  idempotency rule that applies to power levels 8–10
