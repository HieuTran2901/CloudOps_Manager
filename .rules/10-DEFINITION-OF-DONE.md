# Definition of Done

A feature is DONE only when all applicable criteria are satisfied.

## Code

- [ ] Implementation complete.
- [ ] Naming is clear.
- [ ] No unnecessary abstraction.
- [ ] No debug code.
- [ ] No secrets.
- [ ] No unrelated modifications.

## Architecture

- [ ] Correct module boundary.
- [ ] AWS calls isolated appropriately.
- [ ] No controller-to-AWS direct coupling.
- [ ] No accidental circular dependencies.

## Security

- [ ] Authorization enforced.
- [ ] Sensitive values protected.
- [ ] Dangerous actions protected.
- [ ] Audit logging implemented where required.
- [ ] Least privilege considered.

## Testing

- [ ] Unit tests added where appropriate.
- [ ] Integration tests added where appropriate.
- [ ] Relevant regression tests pass.
- [ ] Build passes.

## AWS

- [ ] Correct account/region handling.
- [ ] Pagination considered.
- [ ] Retry/throttling considered.
- [ ] AWS failure behavior handled.
- [ ] Cost implications considered.

## Documentation

- [ ] API documentation updated.
- [ ] Architecture docs updated if necessary.
- [ ] Progress files updated.
- [ ] Known limitations documented.

## Final Verification

- [ ] Git diff reviewed.
- [ ] Git status reviewed.
- [ ] No accidental files.
- [ ] Acceptance criteria verified with evidence.
