# Dependency Weight Audit Prompt

Use the fuck-my-shit-mountain skill in **dependency-weight mode**.

Focus on whether dependencies are pulling their weight — not just known vulnerabilities, but whether each dependency is justified relative to the project's scale.

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for dependency-weight but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

---

## Audit Areas

### Overweight Dependencies
- Large frameworks used for a tiny fraction of their capability.
- Library included for one utility function that could be inlined in 5 lines.
- Full-featured dependency where a lighter alternative exists.
- "Kitchen sink" dependencies that pull in transitive deps for unused features.
- Dependency that duplicates functionality of another included library.

### Dependency Count
- Total number of direct dependencies — is each one justified?
- Development dependencies that are never used in any script or build step.
- Dependencies that are only used in one file or one function.
- Dependencies added for a feature that was later removed (dead dependency).

### Transitive Dependency Risk
- Deep dependency trees (library → library → library → used for one call).
- Dependencies with no maintenance activity (stale, no recent releases).
- Dependencies with known CVEs that are not fixable by bumping version.
- Native bindings that complicate cross-platform builds.
- Dependencies with conflicting licenses.

### Build Toolchain Complexity
- Multiple build systems for the same project.
- Build scripts longer than the code they compile.
- Custom build tooling that could be replaced by standard toolchain features.
- Pre-build / post-install scripts that execute arbitrary code.
- Docker build stages that download unnecessary dependencies.

### Version Strategy
- No lockfile committed (cargo.lock, package-lock.json, yarn.lock).
- Overly permissive version ranges (`*`, `>=`, `^0`).
- Pinned versions with no upgrade cadence or policy.
- Duplicate versions of the same dependency (multiple versions in the tree).
- Git/submodule dependencies that are not pinned to a specific commit.

## Rules

1. Do not recommend removing a dependency unless the removal is realistic and reduces risk.
2. A "heavy" dependency is acceptable if it is used deeply and consistently.
3. Developer experience matters — a well-known library may be worth the weight for DX alone.
4. For each flagged dependency, check: can it be removed, replaced with a lighter alternative, or inlined?

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Release / Performance / Maintainability
- Status: Confirmed / Suspected
- Subtype: Overweight / Unused / DeadDependency / TransitiveRisk / ToolchainComplexity / VersionRisk
- Affected area:
- Dependency:
- Evidence:
  - File (where it is used):
  - Usage pattern (how much of its API is used):
  - Bundle / binary size contribution (if measurable):
- Problem:
- Why it is a risk:
- What it provides vs what the project actually needs:
- Recommended action: Keep / Inline / ReplaceWithLighter / Remove / AuditTransitives
- Minimal fix:
- Build / test verification after removal:
- Estimated effort:
