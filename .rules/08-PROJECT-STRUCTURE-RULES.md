# Project Structure Rules

Recommended repository:

```text
cloudops-manager/
├── backend/
├── frontend/
├── infrastructure/
│   └── terraform/
├── docs/
│   ├── architecture/
│   ├── api/
│   ├── security/
│   └── project-progress/
├── scripts/
├── .github/
│   └── workflows/
├── .gitignore
├── README.md
└── AGENTS.md
```

## Documentation Ownership

Architecture documentation:

```text
/docs/architecture/
```

Security documentation:

```text
/docs/security/
```

Progress tracking:

```text
/docs/project-progress/
```

## Progress Folder Must Exist Early

Before substantial implementation, create:

```text
docs/project-progress/
  ROADMAP.md
  CURRENT-TASK.md
  COMPLETED.md
  BLOCKERS.md
  TECH-DEBT.md
  DECISIONS.md
  FILE-STATUS.md
```

## Documentation Is Part of the Product

If an architectural decision changes, update DECISIONS.md.

If a known limitation is introduced, update TECH-DEBT.md.

If a task is blocked, update BLOCKERS.md.

Do not rely on conversation history as the only project memory.
