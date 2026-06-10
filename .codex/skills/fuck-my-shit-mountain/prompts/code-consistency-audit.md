# Code Consistency Audit Prompt

Use the fuck-my-shit-mountain skill in **code-consistency mode**.

Focus on code style consistency, naming conventions, pattern uniformity, and adherence to the project's own stated conventions.

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for code-consistency but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

## Audit Areas

### Naming Conventions
- Inconsistent casing (camelCase vs snake_case vs kebab-case) within the same language.
- Abbreviations used inconsistently (e.g., `idx` vs `index`, `cfg` vs `config`, `msg` vs `message`).
- Names that obscure meaning (single-letter variables beyond loop counters, overly generic names like `data`, `info`, `temp`).
- Function/method names that don't describe what they do (side effects not reflected in name).
- Boolean parameters that are meaningless at call site (e.g., `process(true, false)` without named params/struct).

### Import / Module Organization
- Import groups not following project convention (stdlib → third-party → internal).
- Wildcard imports that pollute namespace (`use module::*`, `from module import *`).
- Circular dependencies between modules.
- Files/modules that import from deep/generic paths instead of the public API.

### Error Handling Consistency
- Mix of error return patterns (e.g., exceptions in some paths, error codes in others).
- Inconsistent error type usage (string errors vs typed errors vs Result/Option).
- Some functions return `null`/`nil`/`None` for errors while others throw.
- Different parts of the codebase use different logging patterns for similar situations.

### Pattern Uniformity
- Similar operations done different ways across the codebase (e.g., HTTP clients constructed inline vs injected).
- Configuration access patterns vary (env vars read directly vs through a config object).
- Same data transformation duplicated with slightly different implementations.
- Inconsistent use of language features (e.g., async/await mixed with raw promises/callbacks).

### File / Directory Structure
- Files that don't follow the project's naming convention.
- Misplaced files (utility function in a domain-specific module, or vice versa).
- Files in the wrong directory layer (e.g., infrastructure code in domain layer).
- N+1 files doing essentially the same thing in slightly different ways.

### Boilerplate / Verbosity
- Repeated patterns that could be extracted (same validation appearing in N handlers).
- Manual serialization/deserialization where a library/framework handles it elsewhere.
- Copy-paste code that differs only in variable names.

## Rules

1. Focus on inconsistencies that create **real maintenance cost**, not aesthetic preferences.
2. A single inconsistent file is noise; a pattern of inconsistency across 5+ locations is a finding.
3. Do NOT suggest a full codebase reformat — suggest targeted extraction or lint rule additions.
4. If the project has an existing style guide or linter config, check compliance against it.
5. Consider whether a `clippy`/`eslint`/`ruff` rule could catch the inconsistency automatically.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Maintainability
- Status: Confirmed / Suspected
- Subtype: NamingConvention / ImportOrganization / ErrorHandlingConsistency / PatternUniformity / FileStructure / Boilerplate
- Evidence:
  - File(s):
  - Pattern observed:
  - Expected convention:
- Number of occurrences:
- Why this creates maintenance cost:
- Minimal fix (extract + unify):
- Better long-term fix:
- Regression test suggestion:
- Estimated effort:
