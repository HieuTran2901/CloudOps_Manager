# Git and Repository Rules

## Branching

Use small feature branches.

Examples:

```text
feature/aws-ec2-inventory
feature/security-group-audit
feature/cloudwatch-metrics
fix/sts-session-timeout
```

## Commit Scope

One logical change per commit where practical.

Commit messages should describe intent.

Examples:

```text
feat(ec2): add instance inventory discovery
feat(security): detect public ssh access
fix(sts): handle expired assumed-role credentials
test(ec2): add pagination coverage
```

## Never Commit

Never commit:

```text
.env
*.pem
*.key
credentials
AWS access keys
session tokens
production secrets
database passwords
```

## Diff Review

Before completion:
- inspect git diff;
- inspect git status;
- confirm no accidental files;
- confirm no generated secrets;
- confirm no unrelated formatting changes.

## Generated Files

Do not commit build artifacts unless explicitly required.

Examples:

```text
target/
node_modules/
dist/
build/
coverage/
```

## Formatting

Do not reformat entire files as part of a small feature.

Avoid huge noisy diffs.
