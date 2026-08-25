# CloudOps Manager — Master Engineering Rules

## Purpose

These rules define how the project must be planned, implemented, reviewed, tested, documented, and progressed.

The AI coding agent ("Anti") MUST read all rule files in this directory before changing project code.

## Core Principles

1. Architecture First.
2. Stability Before Features.
3. Security Before Convenience.
4. Small, reviewable changes.
5. Evidence Before Claims.
6. No speculative implementation.
7. No silent scope expansion.
8. Never hide failing tests or warnings.
9. Never mark incomplete work as complete.
10. Preserve existing behavior unless a change explicitly requires it.

## Mandatory Workflow

For every task:

```text
READ RULES
  ↓
INSPECT REPOSITORY
  ↓
DEFINE SCOPE
  ↓
CREATE/UPDATE PLAN
  ↓
IMPLEMENT SMALL CHANGE
  ↓
RUN VALIDATION
  ↓
UPDATE PROGRESS
  ↓
REVIEW DIFF
  ↓
REPORT RESULT
```

## Before Coding

Anti MUST:
- inspect the existing repository structure;
- identify relevant modules;
- read relevant source files;
- identify existing conventions;
- check current progress;
- check known risks/blockers;
- avoid modifying unrelated files.

## After Coding

Anti MUST:
- run the smallest relevant tests first;
- run broader validation when appropriate;
- inspect the final diff;
- update progress files;
- document blockers;
- report exactly what changed and what was not completed.

## Forbidden

- Rewriting the project without explicit approval.
- Large unrelated refactors.
- Deleting tests to make builds pass.
- Disabling security checks to make CI pass.
- Introducing a new framework without justification.
- Marking a task complete without evidence.
- Modifying generated/vendor files unless explicitly required.
