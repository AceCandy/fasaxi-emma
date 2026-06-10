# Full Audit Prompt

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for full (all 19 dimensions) but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

Use the fuck-my-shit-mountain skill in **full mode**.

Audit this repository as if it is preparing for a stable public release.

Your job is not to insult the codebase. Your job is to identify real engineering risks with evidence.

## Setup

Before writing findings, build a project map:

- Main components and their responsibilities
- Runtime entry points and initialization order
- Data flow — request/event lifecycle
- State ownership — what owns what state, how it is mutated
- Persistence layer — storage format, migration strategy, backup
- External interfaces — APIs, WebSocket, CLI, file system, network
- Security boundaries — authentication, authorization, input validation, secret management
- Testing structure — test organization, coverage patterns, CI integration
- Release process — versioning, build, packaging, deployment, rollback

## Audit Dimensions

1. **Architecture and module boundaries** — cohesion, coupling, dependency direction, layered architecture
2. **Security** — authentication, authorization, injection, secret handling, dependency risks
3. **Stability and error handling** — panic paths, error propagation, retry, timeout, shutdown
4. **Performance and scalability** — hot paths, growth limits, resource leaks, contention
5. **Testing quality** — coverage, test types, flakiness, confidence
6. **Maintainability** — complexity, duplication, naming, documentation accuracy
7. **Design principles compliance** — check `rubrics/principles.md` for SRP, file size, function length, coupling, cohesion, DRY, YAGNI, KISS, fail-fast, command-query separation, law of demeter, and all other principles
8. **Release and deployment process** — CI/CD, versioning, upgrade, rollback
9. **Documentation accuracy** — does the docs match the code?
10. **Configuration safety** — defaults, environment separation, validation
11. **Observability** — logging, metrics, tracing, debugging support
12. **Fallback / defensive code audit** — check `prompts/fallback-audit.md` for silent fallbacks, empty catches, compatibility branches, and defensive guessing that hides real errors
13. **Testing authenticity audit** — check `prompts/testing-authenticity-audit.md` for over-mocking, implementation detail tests, production code modified for tests, and false confidence
14. **Type safety audit** — check `prompts/type-safety-audit.md` for unsafe blocks, type assertions, boundary weakness, and error type quality
15. **Frontend state audit** (if applicable) — check `prompts/frontend-state-audit.md` for component size, state duplication, effect proliferation, and UI-business logic coupling
16. **Backend API audit** (if applicable) — check `prompts/backend-api-audit.md` for API consistency, request validation, data access patterns, and error response structure
17. **Dependency weight audit** — check `prompts/dependency-weight-audit.md` for overweight deps, unused deps, build toolchain complexity, and version strategy
18. **Code consistency audit** — check `prompts/code-consistency-audit.md` for naming conventions, import organization, error handling patterns, pattern uniformity, file structure, and boilerplate duplication
19. **Comment coverage audit** — check `prompts/comment-coverage-audit.md` for missing public API docs, stale/misleading comments, over-commenting, module documentation gaps, and inline comment quality

## Rules

1. Every finding must include concrete evidence (file, function, behavior).
2. Separate confirmed issues from suspected issues.
3. Do not exaggerate severity. Use the severity rubric.
4. Do not recommend rewrites unless local fixes are clearly insufficient.
5. Prefer the smallest practical fix that reduces real risk.
6. Do not produce generic advice.
7. Do not complain about style unless it creates maintainability risk.
8. If evidence is insufficient, say so.
9. For each issue, include a regression test suggestion.
10. Prioritize the top risks first.
11. Cross-reference findings against `rubrics/principles.md`. For each principle violation, cite the specific principle (e.g., "SRP violation — principle 1.1").

## Attitude

1. **Be exhaustive.** Search the entire codebase, not just obvious hotspots. Every file, every function. Leave no stone unturned.
2. **Do not be a yes-man.** Do not suppress findings to be agreeable. Report issues objectively regardless of who wrote the code. If the code has problems, say so.

## Scoring

After collecting all findings, assign dimension scores using `rubrics/scoring.md`:

1. Review all findings per dimension.
2. Judge the score (0.0–10.0, **10 = best / clean, 0 = worst / shit mountain**) based on **engineering quality and maintainability**, not on mechanical deduction.
3. Each score must have a **one-sentence justification** summarizing the strongest evidence.
4. Render the score dashboard with ASCII bars and letter grades.
5. Include the dashboard with justifications in the Executive Summary.

## Output Format

**IMPORTANT: Use the skill's templates, NOT the project's markdown style.**
1. Each finding MUST follow `templates/issue-card.md` exactly.

2. The report MUST follow `templates/audit-report.md` (or `templates/audit-report.html` for HTML).

3. For HTML output: read `templates/audit-report.html` and generate a COMPLETE HTML file that copies the exact structure:
   - Score dashboard: one .score-item per scoring dimension the user selected (full mode = Security, Stability, Performance, Testing, Maintainability, Design, Release). Do NOT show dimensions the user didn't pick.
   - Executive summary: 2-4 paragraph overview covering project health, biggest risks, bright spots, priorities, and overall grade in context
   - Stats row with total + severity breakdown
   - Top risks table with all findings
   - Detailed findings with full evidence + fix boxes
   - Per-dimension sections: one `<h3>` per audit dimension the user selected (full mode = ALL 19: Architecture, Security, Stability, Performance, Testing, Maintainability, Design, Release, Documentation, Configuration, Observability, Fallback, Testing-Authenticity, Type-Safety, Frontend-State, Backend-API, Dependency-Weight, Code-Consistency, Comment-Coverage). Each section has findings table + verified checklist. Do NOT skip any.
   - Design principles violations table + followed checklist
   - Fix order with tables grouped by priority
   - Quick wins grid
   - Sidebar nav links: one per section, matching selected dimensions only
   - Footer text
   Do NOT skip any section. Do NOT use placeholder variables. Generate complete, self-contained HTML.
3. Do NOT copy formatting, headings, or style from any `.md` file in the audited project.
4. The project's own README, docs, or comments are not the report format.
