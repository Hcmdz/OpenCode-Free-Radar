# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 0.1.x | Yes |
| < 0.1 | No |

## Reporting a Vulnerability

If you discover a security vulnerability in OpenCode Free Radar, please report
it responsibly:

1. **Do NOT open a public GitHub issue** for security vulnerabilities
2. **Email:** [REDACTED]@[REDACTED]
3. **Include:**
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

## Response Timeline

- **Acknowledgment:** within 48 hours
- **Initial assessment:** within 1 week
- **Fix or mitigation:** within 30 days for critical issues

## Security Measures

- All catalog traffic is HTTPS-only (public model indexes, no auth tokens)
- No accounts, no analytics, no third-party tracking SDKs
- All data stays on device in a local Room database
- Only one exported component: the launcher `MainActivity`
- R8 minification enabled on release builds (`isMinifyEnabled = true`)
- Release keystore lives outside the repository and is never committed
