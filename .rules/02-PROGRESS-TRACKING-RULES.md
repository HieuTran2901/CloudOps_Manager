# Progress Tracking Rules

## Source of Truth

Project progress MUST be maintained inside:

```text
/docs/project-progress/
```

Required files:

```text
/docs/project-progress/
  ROADMAP.md
  CURRENT-TASK.md
  COMPLETED.md
  BLOCKERS.md
  TECH-DEBT.md
  DECISIONS.md
  FILE-STATUS.md
```

## CURRENT-TASK.md

Before implementation, record:

```text
Task:
Goal:
Scope:
Files expected to change:
Acceptance criteria:
Validation commands:
Risks:
```

After implementation, update:

```text
Status: DONE | PARTIAL | BLOCKED
Actual files changed:
Tests executed:
Validation result:
Remaining work:
```

## ROADMAP.md

Use phases:

```text
[ ] Planned
[~] In Progress
[x] Completed
[!] Blocked
```

Every phase must contain measurable acceptance criteria.

## FILE-STATUS.md

Maintain a row for important project files/modules:

| File/Module | Status | Last Change | Tests | Notes |
|---|---|---|---|---|
| ... | DONE | ... | PASS | ... |

Allowed statuses:

```text
PLANNED
IN_PROGRESS
IMPLEMENTED
VERIFIED
BLOCKED
DEPRECATED
```

Do not use "DONE" when tests/validation have not passed.

## COMPLETED.md

Append completed work chronologically.

Do not rewrite history to hide previous failures.

## BLOCKERS.md

Every blocker must contain:

```text
ID:
Date:
Area:
Problem:
Evidence:
Impact:
Attempted solutions:
Next action:
```

## TECH-DEBT.md

Record intentional shortcuts and deferred improvements.

Never silently accumulate technical debt.

## Update Timing

Progress MUST be updated:
- before starting a substantial task;
- after implementation;
- after validation;
- when blocked;
- when scope changes.

## Evidence Rule

Statements such as:

```text
"EC2 module is complete"
```

must be supported by:
- tests;
- build output;
- integration evidence;
- or an explicit documented limitation.

Never claim success based only on source inspection.
