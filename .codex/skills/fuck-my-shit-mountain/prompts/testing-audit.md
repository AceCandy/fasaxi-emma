# Testing Audit Prompt

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for testing but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

Use the fuck-my-shit-mountain skill in **testing mode**.

Focus on whether the tests provide real confidence in the codebase.

## Audit Areas (see principles 8.1–8.5)

### Coverage Quality (not quantity)
- Critical paths without test coverage
- Error handling paths without test coverage
- Edge cases in input validation
- Boundary conditions
- Failure mode testing

### Test Types
- Unit test coverage of core logic
- Integration test coverage of external interfaces
- End-to-end test coverage of critical user flows
- Snapshot / golden file test quality
- Property-based or fuzz test coverage where appropriate

### Test Patterns
- Tests that only cover happy paths
- Tests that are over-mocked (testing mock behavior, not real behavior)
- Tests that assert implementation details (brittle)
- Tests that exist only for coverage metrics
- Tests that are flaky (non-deterministic)

### Missing Tests
- Regression tests for past bugs
- Concurrency / race condition tests
- Performance / benchmark tests
- Upgrade / migration tests
- Configuration permutation tests
- Security tests (auth bypass, injection, permission)

### Test Infrastructure
- CI test execution (speed, parallelism, ordering)
- Test data management (fixtures, factories, cleanup)
- Test isolation (shared state between tests)
- Test environment consistency
- Test reporting (what breaks, where, why)

## Attitude

1. **Be exhaustive.** Check every critical path, every error path, every edge case. One missing test can ship a critical bug.
2. **Do not be a yes-man.** Report testing gaps even if the user says "we have good coverage." Coverage percentage does not equal confidence.

## Grouping

Group recommendations into:

- **Must add** — without these, the project cannot be confidently released
- **Should add** — significant confidence gap
- **Nice to have** — incremental improvement
- **Not worth testing** — trivial, stable, or generated code

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Testing
- Status: Confirmed / Suspected
- Affected area:
- Evidence:
  - File:
  - Function / Module:
  - Relevant behavior:
- Behavior to test:
- Why it matters:
- Suggested test type: Unit / Integration / E2E / Property / Fuzz
- Minimal test case:
- Failure it would catch:
- Estimated effort:
- Priority: Must add / Should add / Nice to have / Not worth testing
