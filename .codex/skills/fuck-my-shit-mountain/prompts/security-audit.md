# Security Audit Prompt

## Required: Ask Before Auditing

**STOP. Do not read any code yet. You must ask the user these questions first:**

1. **Audit modes** — Present the FULL list: `full`, `security`, `stability`, `performance`, `testing`, `maintainability`, `release`, `fallback`, `testing-authenticity`, `type-safety`, `frontend-state`, `backend-api`, `dependency-weight`, `code-consistency`, `comment-coverage`. Ask: "Which mode(s)? Pick one or comma-separated. This prompt is pre-configured for security but you can choose any."
2. **Report language** — Ask: "What language should the report be written in? (English / Chinese / etc.)"
3. **Output format** — Ask: "How do you want the report? `md` / `html` / `both` / `stdout`"

Wait for answers before proceeding.

---

Use the fuck-my-shit-mountain skill in **security mode**.

Focus only on security-relevant risks.

## Audit Areas (see principle 4.6: Least Privilege)

### Authentication & Authorization
- How does the application identify users?
- How does it determine what each user can do?
- Can a user access resources they should not have access to?
- Are there hardcoded credentials, tokens, or API keys?
- How are sessions managed? Token lifecycle?

### Input & Output
- Injection vectors (command, SQL, NoSQL, LDAP, template)
- Path traversal in file operations
- SSRF in URL fetching or proxy features
- CORS configuration
- CSRF protection
- Serialization of untrusted data

### Secrets & Configuration
- Hardcoded secrets in code, tests, or config files
- Secrets in environment variables with unsafe defaults
- Secrets in logs, error messages, or debug output
- Config files committed to version control
- Unsafe default configurations

### Network & Transport
- TLS configuration
- WebSocket authentication and origin checking
- API endpoint exposure (internal vs external)
- Rate limiting and brute-force protection

### Dependencies
- Known vulnerable dependencies
- Supply chain risks (install scripts, postinstall hooks, build-time code execution)
- Unnecessary dependencies expanding attack surface
- Dependency confusion (public package shadowing internal name)

### Operational Security
- File permissions on sensitive files
- Logging of sensitive data (passwords, tokens, PII)
- Debug endpoints left enabled
- Error handling that leaks internal state
- Information disclosure in response headers or error pages

## Rules

1. Every security finding must include an attack precondition and attack path.
2. Do not report theoretical vulnerabilities without a realistic exploitation path.
3. Separate confirmed issues from suspected issues.
4. For each issue, include a specific mitigation and a regression test.

## Attitude

1. **Be exhaustive.** Check every endpoint, every auth path, every dependency. Attackers only need one hole.
2. **Do not be a yes-man.** Report security issues objectively. Do not downplay because the project "is just internal" or "nobody will attack us."

## Format Constraint

**CRITICAL: The report MUST follow the skill's template format (`templates/issue-card.md` for findings, `templates/audit-report.md` or `.html` for the full report). Do NOT copy the formatting, heading style, or structure of any markdown file inside the audited project. The project's own docs are not the report template.**

For HTML output: read `templates/audit-report.html` and generate complete HTML that copies the exact structure. Score dashboard: one .score-item per dimension relevant to this mode (NOT all 7 — only the dimensions this mode audits). Per-dimension sections: one `<h3>` per relevant dimension, each with findings table + verified checklist. Design principles, fix order tables, quick wins grid, sidebar nav for every section. Do NOT use placeholder variables. Generate complete self-contained HTML.

## Finding Format

### Finding: <short title>

- Severity: Critical / High / Medium / Low / Info
- Confidence: High / Medium / Low
- Category: Security
- Status: Confirmed / Suspected
- Affected area:
- Evidence:
  - File:
  - Function / Module:
  - Relevant behavior:
- Attack precondition:
- Attack path:
- Impact:
- Mitigation:
- Regression test suggestion:
- Estimated effort:

## Focus

Do not waste output on:
- HTTPS enforcement when the app is designed for local-only use
- CSP headers when the app has no browser UI
- Theoretical supply chain attacks without evidence
- Missing auth on endpoints that are explicitly public
