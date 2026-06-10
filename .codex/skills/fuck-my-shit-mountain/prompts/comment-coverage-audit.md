# Comment Coverage Audit Prompt

Use the fuck-my-shit-mountain skill in **comment-coverage mode**.

Focus on documentation quality, comment coverage of public APIs, stale/misleading comments, and the balance between useful documentation and unnecessary noise.

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for comment-coverage but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

## Audit Areas

### Public API Documentation
- Public functions/types/constants without doc comments (where the language/ecosystem expects them).
- Missing module-level documentation (no module overview, no purpose statement).
- API surfaces that lack examples or usage guidance.
- Parameters or return values not documented (especially non-obvious ones like boolean flags, magic numbers, or error conditions).

### Stale / Misleading Comments
- Comments that describe behavior no longer true (refactored code, changed assumptions).
- "TODO" / "FIXME" / "HACK" / "XXX" comments that are years old with no associated issue or plan.
- Comments that lie (describe one thing, code does another).
- Commented-out code blocks without explanation of why they're kept.
- Outdated file headers, copyright dates, or author lines.

### Over-commenting / Noise
- Comments that simply restate the code in English (`i += 1  // increment i`).
- Comment headers for trivial sections (`// Getters`, `// Constructor`).
- Change log comments in file headers (`2024-01-01: fixed bug` — that's what git is for).
- Comments that explain "what" instead of "why".

### Module / Package Documentation
- Missing README or module-level docs for key packages.
- Missing architecture decision records (ADR) for non-obvious design choices.
- Missing setup/configuration documentation for development environment.
- No documented error handling strategy or conventions.

### Inline Comment Quality
- Comments that don't add context (explaining trivial code, missing explanation of non-trivial code).
- Magic numbers/strings without explanation.
- Complex algorithms or business rules without rationale comments.
- Safety invariants (lock ordering, memory ownership, thread safety assumptions) not documented.

### Self-Documenting Code
- Code so convoluted that even a well-written comment can't save it — consider this a finding.
- Functions with 7+ parameters that need a novel to explain what each does.
- Names so misleading that they contradict what the code actually does.

## Rules

1. A missing doc comment on every trivial getter/setter is not a finding. A missing doc on a public API that has unclear preconditions or side effects is.
2. Prioritize stale/misleading comments over missing ones — wrong docs are worse than no docs.
3. Consider the project's language ecosystem norms (Rust expects doc comments on pub items, Python less so).
4. Flag commented-out code only if it's extensive (5+ lines) or has been there for multiple commits.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Maintainability
- Status: Confirmed / Suspected
- Subtype: MissingDoc / StaleComment / NoiseComment / MissingModuleDoc / PoorInlineComment / NotSelfDocumenting
- Evidence:
  - File(s):
  - Function / Module:
  - Current comment (or absence):
- Why this matters:
- Impact on maintainability or onboarding:
- Minimal fix:
- Better long-term fix:
- Regression test suggestion:
- Estimated effort:
