## ADDED Requirements

### Requirement: New approval attempt on retry or resubmission
Retrying or resubmitting a rejected, expired, or failed high-power
artifact request SHALL create a new approval attempt, evaluated
independently under the same approval applicability rules as any newly
submitted processing attempt.

#### Scenario: Resubmission of a rejected high-power request
- **GIVEN** a high-power request that was previously `REJECTED`
- **WHEN** the requester tries again
- **THEN** a new approval attempt is created for the new submission
- **AND** the new attempt enters `PENDING_APPROVAL` on its own

#### Scenario: Resubmission of an expired or failed high-power request
- **GIVEN** a high-power request that previously became `EXPIRED` or
  failed during processing
- **WHEN** the requester submits a new attempt
- **THEN** a new approval attempt is created independent of the outcome
  of the previous attempt

### Requirement: Prior approval does not carry forward
Approval granted on a previous attempt SHALL NOT automatically authorize a
new attempt. Each new attempt requires its own approval decision when its
power level requires approval.

#### Scenario: Previously approved attempt does not authorize a new one
- **GIVEN** a prior attempt for the same requester reference or original
  request was `APPROVED`
- **WHEN** a new attempt is submitted (for example, after a later failure
  or a further retry)
- **THEN** the new attempt still requires its own approval decision
- **AND** the prior approval does not, by itself, allow processing to
  begin on the new attempt

### Requirement: Decision history links related attempts
Decision history SHALL link all attempts that share the same requester
reference or the same original request.

#### Scenario: History links multiple attempts
- **GIVEN** a requester reference or original request has multiple
  submission attempts over time
- **WHEN** decision history is retrieved for that requester reference or
  original request
- **THEN** the history includes the decisions from every linked attempt
