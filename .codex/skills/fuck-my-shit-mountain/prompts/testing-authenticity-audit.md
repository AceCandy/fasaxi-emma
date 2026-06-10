# Testing Authenticity Audit Prompt

Use the fuck-my-shit-mountain skill in **testing-authenticity mode**.

Focus on whether tests provide **real confidence** or just green checkmarks. This is not about coverage percentage — it is about whether the tests would catch real bugs.

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for testing-authenticity but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

---

## Audit Areas

### Over-Mocking
- Tests that mock everything except the exact function being tested.
- Mocks that return canned data and never exercise real behavior.
- Mock assertions that verify the mock was called (testing the mock framework, not the code).
- Mock setups that are more complex than the code under test.

### Testing Implementation, Not Behavior
- Tests that assert on internal state, private methods, or intermediate values.
- Tests that break on refactoring that does not change external behavior.
- Tests that know too much about how a function works internally.
- Snapshot tests that capture irrelevant output.

### Production Code Modified for Tests
- `if (isTest)` / `if (env === 'test')` branches in production code.
- Special constructors, setters, or config paths only used by tests.
- `#[cfg(test)]` / `if __name__ == '__main__'` that changes production behavior.
- Mock/injector frameworks that require production code to accept injected dependencies it otherwise would not need.

### Happy-Path-Only Tests
- Tests that only verify the success case.
- No tests for: invalid input, network error, auth failure, empty data, concurrent access.
- Error paths that return `Ok` are never tested with actual `Err` conditions.

### Brittle Tests
- Tests that depend on exact string matching of error messages.
- Tests that depend on timing, ordering, or random data.
- Tests that depend on specific dates or environment variables.
- Tests that fail when run in a different order.

### False Confidence
- 100% line coverage with no assertion on behavior (only that "no crash").
- Integration tests that start a server but never send realistic requests.
- Tests that pass with a broken implementation because the mock is too permissive.
- Property-based tests with trivial generators that never produce edge cases.

## Rules

1. A test that never fails is not a good test — it is a test that never catches bugs.
2. If production code has test-only branches, that is a design smell. Tests should exercise production paths, not create special ones.
3. Over-mocked tests test the mock, not the code. Reduce mock scope or write integration tests.
4. Snapshot tests are not free — each one is a maintenance liability.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Testing
- Status: Confirmed / Suspected
- Subtype: OverMocked / ImplDetail / ProdCodeForTest / HappyPathOnly / Brittle / FalseConfidence
- Affected area:
- Evidence:
  - Test file:
  - Production file (if test-specific logic):
  - Test function:
  - What it actually tests vs what it should test:
- Problem:
- Why it produces false confidence:
- Recommended action: Rewrite / Delete / Keep but augment / Move to integration
- Minimal fix:
- Suggested replacement test:
- Estimated effort:
