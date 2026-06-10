# Fallback Audit Prompt

Use the fuck-my-shit-mountain skill in **fallback mode**.

Focus on "无异议兜底" — silent fallbacks, default values, empty catch blocks, compatibility branches, and defensive code that may be hiding real errors.

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for fallback but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

---

## Audit Areas

### Silent Fallbacks
- `||` / `or()` / `or_else()` default chains that silently activate.
- Fallback to hardcoded dev/empty values when config is missing.
- Optional/Maybe unwrapping with default that ignores the error.
- Default case in match/switch that returns a magic value without logging.

### Empty Catch / Ignored Errors
- Empty `catch {}` / `except: pass` blocks.
- `// TODO: handle error` with no follow-up.
- Return value of error-returning function discarded with `void` / `let _ =`.
- Errors mapped to `Ok(default)` instead of propagating.

### Compatibility Branches
- `if (isLegacy)` / `if (version < X)` branches in hot paths.
- Code paths that exist only to support old data formats indefinitely.
- Feature flags that are always enabled (or always disabled) with no cleanup plan.

### Silent Data Correction
- Input data silently mutated to fit schema (truncation, coercion, type casting).
- Validation that replaces invalid values with defaults instead of rejecting.
- Date/time parsing that silently assumes UTC/local when ambiguous.

### Defensive Guessing
- "Best effort" parsing that tries 3 formats and takes the first that doesn't crash.
- Network retry that silently succeeds on partial data.
- Auto-detection logic (language, encoding, timezone) with no user override.

## Rules

1. For each fallback, determine: **Should this fail fast, or is the fallback the correct behavior?**
2. If the fallback is correct, does it **log/warn/metric** so operators know it activated?
3. If the fallback is incorrect, recommend **fail-fast + clear error message**.
4. Distinguish between **defensive programming** (validates external input) and **defensive guessing** (hides bugs).
5. A fallback that activates silently and is never monitored is worse than a crash.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Stability
- Status: Confirmed / Suspected
- Subtype: SilentFallback / EmptyCatch / CompatibilityBranch / SilentCorrection / DefensiveGuess
- Affected area:
- Evidence:
  - File:
  - Function / Module:
  - Relevant behavior:
- What it falls back from:
- Why the original path can fail:
- Is the fallback necessary? (Yes / No / Partial):
- If yes, is it monitored?:
- Recommended action: KeepWithAlert / FailFast / Remove / Restructure
- Minimal fix:
- Regression test:
- Estimated effort:
