# Backend API Audit Prompt

Use the fuck-my-shit-mountain skill in **backend-api mode**.

Focus on API design, request/response contracts, data access patterns, and backend data flow consistency.

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for backend-api but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

---

## Audit Areas

### API Design Consistency
- Inconsistent naming conventions across endpoints (`/api/v1/users` vs `/api/getUsers`).
- Mixed response formats (some return `{data: ...}`, others return array directly).
- Status code misuse (200 for errors, 400 for auth failures, 500 for validation).
- Endpoints that return different shapes for success and error.
- No versioning strategy (or versioning that is not actually enforced).

### Request Validation
- Missing input validation on endpoint boundaries.
- Validation logic scattered across handlers instead of centralized.
- Type coercion happening silently (string "123" accepted as number 123).
- Required fields not enforced at the API layer (fail later in business logic).

### Authentication and Authorization
- Auth check duplicated in every handler instead of middleware.
- Mixing auth concerns with business logic.
- Endpoints that should be protected but are not.
- Token/session verification happening too late (after expensive operations).
- Hardcoded roles/permissions instead of configurable policy.

### Data Access Patterns
- N+1 queries in request handlers.
- Database queries in loops.
- Missing pagination on list endpoints.
- Inefficient loading (eager loading everything vs lazy loading vs explicit select).
- Raw queries mixed with ORM queries in the same handler.

### Cache and Invalidation
- Stale cache serving old data because invalidation is missing.
- Cache-aside vs write-through inconsistency.
- Cache keys that do not include all relevant parameters.
- Cache TTL used as a substitute for proper invalidation.

### Error Responses
- Inconsistent error response structure across endpoints.
- Error messages that leak internal implementation details.
- Generic "Internal Server Error" with no correlation ID.
- Stack traces returned to the client in non-dev modes.

### Business Logic Location
- Business rules in API handlers instead of a service/domain layer.
- Validation split between frontend, API layer, and database constraints.
- Business logic in database triggers/stored procedures.
- Logic that depends on the order of operations in a handler.

### Data Flow and Transactions
- Partial writes — some data saved, some not, on failure.
- Missing transaction boundaries for multi-step operations.
- Long-held database connections waiting on external I/O.
- Optimistic locking without retry logic.

## Rules

1. Judge API design relative to the project's scale — a 5-endpoint service does not need GraphQL.
2. Consistency is more important than "correct" design. If all endpoints use snake_case, do not recommend camelCase.
3. Missing pagination is a problem only if the dataset can grow beyond ~1000 items.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Security / Stability / Performance / Maintainability
- Status: Confirmed / Suspected
- Subtype: ApiConsistency / Validation / Auth / NplusOne / Caching / ErrorResponse / BusinessLogic / DataFlow
- Affected area:
- Evidence:
  - File:
  - Endpoint / Handler:
  - Relevant behavior:
- Problem:
- Why it matters:
- Realistic failure scenario:
- Minimal fix:
- Better long-term fix:
- Regression test suggestion:
- Estimated effort:
