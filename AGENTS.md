# CloudOps Manager — Agent Bootstrap Instructions

Welcome to the CloudOps Manager project. All AI coding agents working on this repository must strictly adhere to the project governance rules and workflows.

## Mandatory Agent Workflow

Before proposing or making any changes to the codebase, every agent MUST perform the following steps:

1. **Read Master Rules**: Read `.rules/00-MASTER-RULES.md`.
2. **Read Applicable Rules**: Read all applicable rule files in `.rules/*.md` (`01` through `10`).
3. **Read Current Task**: Read `docs/project-progress/CURRENT-TASK.md` to understand active work and status.
4. **Read Roadmap**: Read `docs/project-progress/ROADMAP.md` for project context and phase requirements.
5. **Inspect Repository**: Inspect repository structure and existing conventions before writing or modifying code.
6. **Respect Change Budgets**: Respect file and line-of-code (LOC) change limits (see `.rules/01-SCOPE-AND-CHANGE-RULES.md`).
7. **Update Project Progress**: Update progress tracking files in `docs/project-progress/` after completing meaningful work.
8. **Provide Validation Evidence**: Never claim completion without executing validation and verifying evidence (see `.rules/04-TESTING-AND-VALIDATION-RULES.md` and `.rules/10-DEFINITION-OF-DONE.md`).
9. **Enforce AWS Operational Safety**: STOP and ask for approval before performing dangerous AWS operations (see `.rules/05-AWS-SAFETY-RULES.md`).
10. **Protect Secrets**: Never expose, log, or commit secrets, credentials, or private keys.
