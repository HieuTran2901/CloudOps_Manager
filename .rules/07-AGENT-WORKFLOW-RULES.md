# AI Agent / Anti Workflow Rules

## Mandatory Reading

Before coding, Anti MUST read:

```text
00-MASTER-RULES.md
01-SCOPE-AND-CHANGE-RULES.md
02-PROGRESS-TRACKING-RULES.md
03-CODE-QUALITY-RULES.md
04-TESTING-AND-VALIDATION-RULES.md
05-AWS-SAFETY-RULES.md
06-GIT-AND-REPOSITORY-RULES.md
```

Then inspect project-specific documentation.

## Planning Before Editing

Anti must first produce internally:
1. Current architecture understanding.
2. Relevant files.
3. Intended changes.
4. Risks.
5. Validation plan.

Do not start broad edits immediately.

## File-by-File Discipline

For each changed file:
- know why it must change;
- keep the change focused;
- validate after the logical unit is complete.

## Stop Conditions

Anti MUST STOP and ask for approval when:
- scope exceeds the change budget;
- a database migration becomes unexpectedly destructive;
- an AWS operation could delete production resources;
- credentials/secrets are required but not safely available;
- architecture must materially change;
- a new major dependency/framework is required;
- existing behavior must be intentionally broken;
- tests cannot validate a high-risk change.

## No Autonomous Scope Expansion

If Anti discovers a better idea, do not silently implement it.

Record it in:

```text
/docs/project-progress/TECH-DEBT.md
```

or

```text
/docs/project-progress/DECISIONS.md
```

and continue the approved scope.

## Progress Discipline

After each meaningful milestone:

1. Update CURRENT-TASK.md.
2. Update FILE-STATUS.md.
3. Update ROADMAP.md if status changed.
4. Append COMPLETED.md if completed.
5. Record blockers if blocked.

## Completion Report

Every task completion report must contain:

```text
Implemented:
Files changed:
Tests:
Validation:
Known limitations:
Next recommended task:
```

Never say "everything is complete" unless the acceptance criteria are actually satisfied.
