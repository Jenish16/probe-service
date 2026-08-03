## ADDED Requirements

### Requirement: New approval attempt on retry or resubmission
Retrying or resubmitting a rejected, expired, cancelled, or failed
high-power artifact request SHALL create a new approval attempt,
evaluated independently under the same approval applicability rules as
any newly submitted processing attempt. This applies regardless of
whether the new submission reuses the same requester reference as the
prior attempt and regardless of whether its content is identical to the
prior attempt's content — once a request has reached a terminal outcome,
idempotent duplicate-return no longer applies, and any further submission
tied to that requester reference or original request is treated as a new
attempt. There is no limit on the number of retry attempts for the same
requester reference or original request.

#### Scenario: Resubmission of a rejected high-power request
- **GIVEN** a high-power request that was previously `REJECTED`
- **WHEN** the requester tries again, whether or not the new submission
  reuses the same requester reference or repeats the same content
- **THEN** a new approval attempt is created for the new submission
- **AND** the new attempt enters `PENDING_APPROVAL` on its own

#### Scenario: Resubmission of an expired, cancelled, or failed high-power request
- **GIVEN** a high-power request that previously became `EXPIRED`,
  `CANCELLED`, or failed during processing
- **WHEN** the requester submits a new attempt
- **THEN** a new approval attempt is created independent of the outcome
  of the previous attempt

#### Scenario: Repeated retries remain unbounded
- **GIVEN** a requester reference or original request has already been
  retried one or more times after terminal outcomes
- **WHEN** the requester submits yet another attempt
- **THEN** the new attempt is evaluated independently with no maximum
  retry count enforced

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
