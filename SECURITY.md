# Security Policy

## Supported Versions

We provide security fixes for actively supported branches. Older releases may
not receive backports.

| Version               | Supported |
| --------------------- | --------- |
| main (development)    | yes       |
| latest stable release | yes       |
| older releases        | no        |

## Reporting a vulnerability

Please report vulnerabilities privately using GitHub Private Vulnerability Reporting (PVR):

- Open the repository's Security tab and select "Report a vulnerability" to submit a private report.
- If you are unfamiliar with this flow, see GitHub's guide: [Privately reporting a security vulnerability](https://docs.github.com/en/code-security/security-advisories/guidance-on-reporting-and-writing/privately-reporting-a-security-vulnerability).

Avoid filing public issues for potential vulnerabilities.

### What to include
- Affected versions, modules, and environment details
- Steps to reproduce or a minimal proof of concept
- Impact assessment and suggested severity (CVSS vector if available)
- Any proposed mitigations or workarounds

## Our commitments (response targets)
- We acknowledge new reports within 3 business days.
- We provide status updates at least every 14 days until resolution.
- We work with you on coordinated disclosure timing and will notify you when a fix or mitigation is available.

## Coordinated disclosure and CVEs
When appropriate, we will open a GitHub Security Advisory and request a CVE ID through GitHub to track the issue and its resolution. We will coordinate public disclosure with the reporter once a fix, mitigation, or release is available.

## Safe harbor for security research
We support good-faith security research and will not pursue legal action for research that:

- Makes a good-faith effort to avoid privacy violations, data destruction, or service degradation
- Does not access, modify, or exfiltrate data beyond what is necessary to demonstrate the vulnerability
- Respects rate limits and avoids denial-of-service conditions
- Stops testing and reports immediately upon discovering a real vulnerability
- Complies with applicable laws

If you require an encrypted channel, note this in your report and we can arrange a secure exchange.

## Out of scope
- Best-practice recommendations without a proven vulnerability
- Vulnerabilities in third-party dependencies that are not owned by this repository (please report upstream)
- Reports that cannot be reproduced
- Denial-of-service demonstrations that require unrealistic conditions or impact only the reporter's own environment

## Non-security issues

For non-security bugs or feature requests, please use the issue tracker instead of the private reporting channel.
