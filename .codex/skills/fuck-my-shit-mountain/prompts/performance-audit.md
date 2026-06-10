# Performance Audit Prompt

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for performance but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

Use the fuck-my-shit-mountain skill in **performance mode**.

Focus on realistic bottlenecks, not premature micro-optimization.

## Audit Areas (see principle 10.2: Unbounded Resources)

### Hot Paths
- Request handler latency
- Inner loop performance
- Allocation frequency on hot paths
- Serialization/deserialization on critical paths

### Data Access
- Database query patterns (N+1, missing indexes, full table scans)
- Cache miss ratio
- Connection pool utilization
- Query result size
- Data pagination

### Concurrency & Locking
- Lock contention on hot paths
- Serialization bottlenecks
- Channel / queue backpressure
- Mutex / RwLock granularity

### Memory
- Unbounded collection growth
- Memory allocation patterns
- Object pooling or lack thereof
- Large object retention
- Memory fragmentation (GC languages)
- Slice / array copying on hot paths

### I/O
- Synchronous I/O in async context
- File I/O pattern (read size, buffering)
- Network round-trip count
- Payload size optimization
- Compression usage

### Frontend (if applicable)
- Re-render frequency
- Virtual DOM diff cost
- Bundle size
- Image / asset optimization
- Lazy loading
- List virtualization

### Startup & Initialization
- Cold start time
- Dependency loading
- Configuration parsing
- Lazy initialization opportunities

## Rules

1. Do not suggest optimization unless there is a realistic scale or workload where it matters.
2. Identify the bottleneck mechanism, not just the symptom.
3. Include the workload or conditions under which the issue becomes relevant.
4. Prefer small, targeted optimizations over architectural changes when sufficient.

## Attitude

1. **Be exhaustive.** Scan every hot path, every query, every allocation pattern. One unoptimized query can kill production at scale.
2. **Do not be a yes-man.** Report bottlenecks even if the user says "it's fast enough for now." Your job is to identify where it will break under load.

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Performance
- Status: Confirmed / Suspected
- Affected area:
- Evidence:
  - File:
  - Function / Module:
  - Relevant behavior:
- Workload where this matters:
- Bottleneck mechanism:
- Expected impact:
- Minimal optimization:
- Better long-term optimization:
- Benchmark or test suggestion:
- Estimated effort:
