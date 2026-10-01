# Security Policy

## Supported Versions

| Version | Supported |
|---|---|
| 0.4.x | Yes |
| 0.3.x | Security fixes only |
| < 0.3 | No |

## Reporting a Vulnerability

If you discover a security vulnerability in OpenCode Free Radar, please report
it responsibly:

1. **Do NOT open a public GitHub issue** for security vulnerabilities
2. **Email:** HcmDz.Dev@gmail.com
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
- Cleartext blocked via Network Security Config; user CAs trusted in debug
  builds only
- No accounts, no analytics, no third-party tracking SDKs
- All data stays on device in a local Room database
- `allowBackup="false"` so app data cannot be extracted via ADB backup
- Only one exported component: the launcher `MainActivity`
- R8 minification enabled on release builds (`isMinifyEnabled = true`)
- Release builds strip all `Log.*` calls via ProGuard
- Android security lint rules (`com.android.security.lint`) run on every build
- Memory tagging (`memtagMode="sync"`) enabled where the hardware supports it
- Release keystore lives outside the repository and is never committed

## In-App Updates

The app can download and install its own updates from GitHub Releases. That is
the highest-risk path in the app, since a substituted build would run with the
app's own permissions and identity.

- Update metadata is fetched from a single pinned endpoint:
  `https://api.github.com/repos/Hcmdz/OpenCode-Free-Radar/releases/latest`.
  No redirect to an unpinned host is followed for the metadata itself.
- Download URLs are accepted only over HTTPS and only from an allowlist:
  `github.com`, `api.github.com`, `objects.githubusercontent.com`,
  `release-assets.githubusercontent.com`, or any `*.githubusercontent.com`
  host. Anything else is rejected before the request is made
  (`isAllowedDownloadUrl`).
- The downloaded APK is hashed with SHA-256 and compared against the digest
  parsed from the release body. A mismatch deletes the file and aborts.
- **Limitation:** the digest is only present when the release body carries a
  `SHA-256:` line. When it is absent the comparison is skipped, so the download
  is not verified at this layer. Install integrity then rests on the Android
  package installer rejecting an update whose signing certificate differs from
  the installed app — the app does not check the certificate itself.
- Install goes through `PackageInstaller` and requires the user to have granted
  *Install unknown apps*; without it the app opens that settings screen instead
  of installing.
- Update checks run at most once every 24 hours, through a client without an
  HTTP cache so a release is never missed because of a stale entry.

## Supply Chain

- Dependabot tracks Gradle and GitHub Actions updates weekly, grouped.
- CI runs CodeQL on push and pull request, plus a weekly schedule.
- CI scans full git history with gitleaks and fails if any filename on the
  shared sensitive-filename list is tracked.
- Licence identifiers for every shipped dependency are inventoried in
  [THIRD_PARTY.md](THIRD_PARTY.md), read from each artifact rather than
  assumed.
