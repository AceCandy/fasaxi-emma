> **INSTRUCTION TO AI: This is the ONLY valid report template. Do NOT use any formatting, heading style, or structure from files inside the audited project. Output MUST follow this template exactly.**

# Fuck My Shit Mountain Audit Report

**Project:** <project name>
**Audit mode:** <full / security / stability / performance / testing / maintainability / release>
**Date:** <date>
**Reviewer:** <AI model / version>

---

## 1. Executive Summary

<2-3 paragraph summary of codebase condition>

### Score Dashboard

```
Security        ████████░░  8.0  A   No auth on WS, hardcoded secret in config
Stability       ██████░░░░  6.0  B   3 unwrap on hot path, no retry on DB
Performance     ██████████  10.0 S   No issues found
Testing         ████░░░░░░  4.0  C   9 integration tests real, but unit is weak
Maintainability ███████░░░  7.0  A   3 files over 800 lines, SRP violated in 2 modules
Design          █████░░░░░  5.0  B   DRY violated 5x, fail-fast missing at API boundary
Release         ██████░░░░  6.0  B   No CI on Windows, no rollback plan
─────────────────────────────────────
Overall         ██████░░░░  6.6  B
```

Each dimension scored 0.0–10.0. **Higher = better (10 = clean, 0 = shit mountain).** Scores are judgment-based, not formula-based. See `rubrics/scoring.md` for anchor descriptions.

### Finding Statistics

| Severity | Count | Confirmed | Suspected |
|----------|-------|-----------|-----------|
| Critical | <N> | <N> | <N> |
| High | <N> | <N> | <N> |
| Medium | <N> | <N> | <N> |
| Low | <N> | <N> | <N> |
| Info | <N> | <N> | <N> |
| **Total** | **<N>** | **<N>** | **<N>** |

## 2. Project Map

<How the project is structured — key components, entry points, data flow, state ownership, persistence, external interfaces, security boundaries>
<Highlight areas most likely to contain risks>

## 3. Top Risks

<5-15 findings in priority order. Each entry includes finding title, severity, and one-sentence summary. Full details in section 4.>

## 4. Detailed Findings

<All findings using the issue-card template>

## 5. <Dimension-specific section>

Depending on audit mode:

- **Full / Security / Backend-API:** Security Concerns
- **Full / Stability / Fallback:** Stability Concerns
- **Full / Performance:** Performance Concerns
- **Full / Testing / Testing-Authenticity:** Testing Gaps
- **Full / Maintainability / Frontend-State / Backend-API:** Maintainability Concerns
- **Full / Type-Safety:** Type Safety Concerns
- **Full / Release / Dependency-Weight:** Release Concerns
- **Security:** Security Concerns
- **Stability:** Stability Concerns
- **Performance:** Performance Concerns
- **Testing:** Testing Gaps
- **Testing-Authenticity:** Testing Authenticity Analysis
- **Maintainability:** Maintainability Concerns
- **Design:** Design / Principles Concerns
- **Release:** Release Concerns
- **Fallback:** Fallback / Defensive Code Analysis
- **Type-Safety:** Type Safety Analysis
- **Frontend-State:** Frontend State Analysis
- **Backend-API:** Backend API Analysis
- **Dependency-Weight:** Dependency Weight Analysis

Each section lists relevant issues grouped by sub-category.

## 6. <Next dimension-specific section>

Repeat for all dimensions covered by the audit mode.

---

## <N+1>. Principles Compliance

<Summary of how well the codebase follows software engineering principles from rubrics/principles.md>

### Principles Violated

| Principle | Violations | Severity | Affected Areas |
|-----------|------------|----------|----------------|
| Single Responsibility (SRP) | <N> | Medium | <modules> |
| File Size Limit | <N> | Low | <modules> |
| Fail-Fast | <N> | High | <modules> |
| ... | ... | ... | ... |

### Principles Respected

<Principles that the codebase follows well — what is being done right>

---

## <N+1a>. Fallback / Defensive Code Analysis

<Only for full or fallback mode. See prompts/fallback-audit.md>

### Fallback Summary

| Subtype | Count | KeepWithAlert | FailFast | Remove |
|---------|-------|---------------|----------|--------|
| SilentFallback | <N> | <N> | <N> | <N> |
| EmptyCatch | <N> | <N> | <N> | <N> |
| CompatibilityBranch | <N> | <N> | <N> | <N> |
| SilentCorrection | <N> | <N> | <N> | <N> |
| DefensiveGuess | <N> | <N> | <N> | <N> |

<Detail each finding with the fallback audit finding format>

## <N+1b>. Testing Authenticity Analysis

<Only for full or testing-authenticity mode. See prompts/testing-authenticity-audit.md>

### Confidence Assessment

| Test Area | Real Confidence | Risk | Action |
|-----------|---------------|------|--------|
| <test file/area> | High / Medium / Low / None | <what bugs would escape> | Keep / Rewrite / Delete |

### Valuable Tests

<Tests that actually provide real regression protection>

### Suspicious Tests

<Over-mocked, implementation-detail, happy-path-only, brittle>

### Missing Tests

<Critical paths with no test coverage>

---

## <N+1c>. Type Safety Analysis

<Only for full or type-safety mode. See prompts/type-safety-audit.md>

### Summary

| Subtype | Count | Critical | High | Medium | Low |
|---------|-------|----------|------|--------|-----|
| UnsafeBlock | <N> | <N> | <N> | <N> | <N> |
| TypeAssertion | <N> | <N> | <N> | <N> | <N> |
| InputBoundary | <N> | <N> | <N> | <N> | <N> |
| OutputLeak | <N> | <N> | <N> | <N> | <N> |
| BooleanTrap | <N> | <N> | <N> | <N> | <N> |
| StringlyTyped | <N> | <N> | <N> | <N> | <N> |
| ErrorType | <N> | <N> | <N> | <N> | <N> |

<Detail each finding with the type-safety audit finding format>

## <N+1d>. Frontend State Analysis

<Only for full or frontend-state mode. See prompts/frontend-state-audit.md>

### Summary

| Subtype | Count | Affected Components |
|---------|-------|-------------------|
| ComponentSize | <N> | <list> |
| StateDuplication | <N> | <list> |
| PropDrilling | <N> | <list> |
| EffectChain | <N> | <list> |
| UIBusinessCoupling | <N> | <list> |
| DOMasState | <N> | <list> |
| RequestState | <N> | <list> |
| RenderPerf | <N> | <list> |

<Detail each finding with the frontend-state audit finding format>

## <N+1e>. Backend API Analysis

<Only for full or backend-api mode. See prompts/backend-api-audit.md>

### Summary

| Subtype | Count | Affected Endpoints |
|---------|-------|-------------------|
| ApiConsistency | <N> | <list> |
| Validation | <N> | <list> |
| Auth | <N> | <list> |
| NplusOne | <N> | <list> |
| Caching | <N> | <list> |
| ErrorResponse | <N> | <list> |
| BusinessLogic | <N> | <list> |
| DataFlow | <N> | <list> |

<Detail each finding with the backend-api audit finding format>

## <N+1f>. Dependency Weight Analysis

<Only for full or dependency-weight mode. See prompts/dependency-weight-audit.md>

### Dependency Scoreboard

| Dependency | Status | Weight | Transitives | Used For | Recommended Action |
|------------|--------|--------|-------------|----------|-------------------|
| <name@version> | Healthy / Overweight / Unused / Dead | <KB/MB> | <count> | <what it does> | Keep / Inline / Remove |

<Detail each finding with the dependency-weight audit finding format>

---

## <N+2>. Recommended Fix Order

### Fix Immediately

<Issues that can cause data loss, security breach, or service outage>

### Fix Before Stable Release

<Issues that degrade reliability, correctness, or security>

### Schedule Later

<Issues that increase maintenance cost or limit scale>

### Ignore for Now

<Low-severity issues, theoretical risks, style preferences>

## <N+3>. Quick Wins

<Low-cost, high-value fixes — typically 1-2 hour changes that remove real risk>

## <N+4>. Long-term Refactor Plan

<Only included if evidence supports structural improvements>
<Each item includes: motivation, approach, risk, and testing strategy>
