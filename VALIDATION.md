# Validation for Quiet Remote 1.0.5

- Java sources compile against Android API 35.
- Java runtime self-check passes (25 commands, immutable lookup, valid durations).
- All 25 source pattern checksums and decoded NEC commands pass.
- All 25 final APK DEX patterns match the original WRC03 recordings exactly.
- Manifest has only TRANSMIT_IR permission; min SDK 26; target SDK 35.
- APK v2/v3 signatures verify; ZIP alignment check passes.
- No third-party runtime dependencies, native libraries or ScrollView reference.
- Signing certificate matches 1.0.4 for in-place upgrades.
- Removed the API 27 windowLightNavigationBar theme item reported by CI lint.
- Workflow YAML checked; lint remains enabled and stable publication requires a successful build.
- Stable release script checks pass for creation, replacement, stale commit skipping,
  missing APK and upload failure, using fake CLI commands without contacting GitHub.

The supplied CI logs show compilation succeeded and lint failed on the API 27
item in the base theme. That item is removed in this build. A local Gradle/lint
attempt could not download the Gradle distribution because the network was
unreachable. The supplied APK was compiled with ECJ, AAPT2, D8 and APKSigner
from Android Build Tools 35.0.0. Full lint must be confirmed by the next CI run.
A physical phone/TV or emulator test has not been performed here. No GitHub
release has been published from this workspace.

APK bytes: 25219

APK SHA-256: `1b78f1d63f6f7075b0a88eda590d104a6e71bd95b48641c506a9e5750a0254cb`
