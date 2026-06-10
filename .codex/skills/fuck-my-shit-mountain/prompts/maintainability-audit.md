# Maintainability Audit Prompt

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for maintainability but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

Use the fuck-my-shit-mountain skill in **maintainability mode**.

Focus on maintainability, complexity, coupling, and design risk.

## Audit Areas

### Size & Structure (see principles 1.1–1.6)
- Files over 500 lines
- Functions over 50 lines
- Functions with deep nesting (4+ levels)
- Files with too many responsibilities — SRP violation (principle 1.1)
- Modules with unclear boundaries

### Complexity (see principles 1.5–1.6)
- Cyclomatic complexity hotspots (>10 paths)
- Functions with too many parameters (4+) — principle 1.4
- Functions with too many branches / conditions
- Complex conditional logic
- Deeply nested callbacks or promises
- Overuse of dynamic features (eval, reflection, metaprogramming)

### Coupling & Cohesion (see principles 2.1–2.6)
- Hidden dependencies between modules — principle 2.1
- Circular dependencies — principle 7.1
- Tight coupling to external libraries or frameworks — principle 2.4
- God objects / classes with too many responsibilities — SRP violation
- Shotgun surgery — one change requires touching many files
- Feature envy — a function that mostly uses data from another module — principle 2.3

### Duplication (see principle 4.1)
- Repeated logic that should be abstracted — DRY violation
- Copy-pasted code with minor variations
- Duplicated configuration or constants
- Parallel hierarchies that mirror each other

### Naming & Abstraction (see principles 3.1–3.5)
- Misleading names that do not reflect behavior — principle 3.1
- Names that hide side effects (e.g., `getX()` that mutates state) — principle 3.2
- Missing documentation on non-obvious behavior
- Documentation that contradicts the code
- Comments that explain "what" instead of "why"
- Boolean trap parameters — principle 3.5

### Design Patterns (see principles 4.2–4.3, 7.4–7.5)
- Business logic mixed with transport (HTTP, CLI, message queue) — principle 7.2
- Business logic mixed with persistence (SQL, file I/O) — principle 7.2
- Business logic mixed with UI rendering — principle 7.2
- Leaky abstractions that expose implementation details
- Premature abstraction — YAGNI violation (principle 4.2)
- Over-engineering — KISS violation (principle 4.3)
- Inheritance over composition — principle 7.4

### Changeability (see principles 4.1, 7.5)
- Code that is difficult to delete (too many callers, unclear ownership)
- Special-case logic scattered across the codebase
- Configuration scattered instead of centralized
- Default values duplicated across code paths — DRY violation
- Feature flags that are never cleaned up — YAGNI violation

## Attitude

1. **Be exhaustive.** Check every file, every function, every module. Maintainability rot hides in the corners nobody looks at.
2. **Do not be a yes-man.** Report design problems even if "it works." Bad design accumulates interest.

## Rules

1. Do not recommend abstraction unless it removes real duplication, coupling, or confusion.
2. For each issue, include the risk of changing the code and the test needed before refactoring.
3. Prefer local improvements over architectural rewrites.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Maintainability
- Status: Confirmed / Suspected
- Affected area:
- Evidence:
  - File:
  - Function / Module:
  - Relevant behavior:
- Why this affects maintainability:
- Risk of changing it:
- Local fix:
- Better long-term fix:
- Test needed before refactor:
- Estimated effort:
