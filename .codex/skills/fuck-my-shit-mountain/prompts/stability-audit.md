# Stability Audit Prompt

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for stability but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

Use the fuck-my-shit-mountain skill in **stability mode**.

Focus on reliability, runtime safety, state consistency, and failure recovery.

## Audit Areas (see principles 4.4, 6.1–6.4, 10.1–10.4)

### Crash & Panic Paths
- Direct calls to `panic!`, `unwrap()`, `expect()`, `assert!`
- Indexing without bounds checking
- Division by zero
- Null pointer / nil dereference risks
- Downcasting without type check
- Integer overflow / underflow

### Error Handling
- Errors that are silently swallowed
- Errors that are logged but not handled
- Catch-all error handlers that mask failures
- Error types that lose context (e.g., `Box<dyn Error>`, `String` errors)
- Missing error propagation in async contexts
- Error recovery that leaves state inconsistent

### Concurrency & State
- Race conditions in shared state access
- Lock ordering and deadlock risks
- Missing synchronization on shared mutable state
- Channel / queue overflow
- Task / goroutine / thread leaks
- Async task cancellation safety

### External Dependencies
- Network calls without timeout
- Retry without backoff or jitter
- Circuit breaker or bulkhead missing
- Database connection pool exhaustion
- File handle leaks
- Resource cleanup on error paths

### Lifecycle
- Graceful shutdown — is cleanup guaranteed?
- Signal handling (SIGTERM, SIGINT)
- State persistence and recovery
- Snapshot / checkpoint corruption handling
- Startup dependency ordering

### Resource Management
- Unbounded memory growth (collections, caches, buffers)
- Goroutine / task / thread leaks
- Connection pool sizing
- Backpressure implementation
- Streaming backpressure

## Rules

1. Focus on realistic failure scenarios, not theoretical ones.
2. For each issue, describe the trigger, the failure scenario, and the user-visible impact.
3. Prefer the minimal fix that removes the crash or inconsistency risk.

## Attitude

1. **Be exhaustive.** Check every error path, every unwrap, every timeout. Assume every failure will happen in production.
2. **Do not be a yes-man.** Do not skip issues because "it works in practice." Report every realistic crash path.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Stability
- Status: Confirmed / Suspected
- Affected area:
- Evidence:
  - File:
  - Function / Module:
  - Relevant behavior:
- Failure trigger:
- Failure scenario:
- User-visible impact:
- Minimal fix:
- Better long-term fix:
- Regression test suggestion:
- Estimated effort:
