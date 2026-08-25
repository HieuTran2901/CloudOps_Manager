# Scope and Change Rules

## Change Budget

Default maximum change per task:

- Maximum files modified: 15
- Maximum production source lines added/changed: 500
- Maximum lines changed in a single source file: 250
- Maximum test lines added/changed: 500
- Maximum documentation lines: 500

If a task genuinely requires more, STOP and update the progress/risk record before continuing.

## File Size Guidance

Target maximum size:

- Java class: 300 lines
- React/TypeScript component: 300 lines
- Service class: 350 lines
- Controller: 200 lines
- Repository: 150 lines
- Configuration file: 250 lines

These are governance limits, not reasons to split code artificially.

If a file exceeds the target:
1. explain why;
2. determine whether cohesion is still good;
3. create a refactoring task if necessary.

## One Logical Change Per Task

A task should have one primary objective.

Bad:

```text
Implement EC2 API
+ redesign authentication
+ migrate database
+ rewrite frontend
+ add Terraform
```

Good:

```text
Implement EC2 read-only inventory API.
```

## No Opportunistic Refactoring

If unrelated code looks bad:
- do not silently refactor it;
- record it in TECH-DEBT.md;
- continue only if it blocks the current task.

## Deletion Rule

Never delete code merely because it appears unused.

Before deletion:
- prove it is unused;
- inspect references;
- run tests;
- document the reason.

## Dependency Rule

Adding a dependency requires:
- justification;
- purpose;
- version;
- security consideration;
- whether an existing dependency can solve the problem.

Do not add libraries for trivial functionality.
