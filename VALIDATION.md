# Validation for Quiet Remote 1.0.4

- Java sources compile against Android API 35.
- Java runtime self-check passes (25 buttons, immutable lookup, valid durations).
- All 25 source pattern checksums and decoded NEC commands pass.
- Original q30 DEX payloads match the extracted recordings.
- All 25 final APK DEX patterns match the original recordings exactly.
- Manifest has only TRANSMIT_IR permission; min SDK 26; target SDK 35.
- APK v2/v3 signatures verify; ZIP alignment check passes.
- No third-party runtime dependencies or native libraries.
- No ScrollView reference in the final APK.
- Launcher and round icon use the compiled adaptive-icon resource.
- Signing certificate is unchanged from 1.0.3 for in-place installation.
- Source launcher vector rendered and inspected inside a circular adaptive-icon crop.
- Keypad buttons use contrasting surfaces, 2 dp borders and 5 dp margins.
- Navigation pad is capped at 252 dp square, bounded by actual page dimensions and aligned bottom-center.
- Compiled navigation page measures its pad before children; sizing no longer occurs during layout.

A physical phone/TV test and Android UI/emulator run have not been performed.
The Gradle workflow and lint task were supplied but not executed locally;
the APK was compiled with ECJ, AAPT2, D8 and APKSigner from Android Build Tools 35.0.0.

APK bytes: 25219

APK SHA-256: `fd4ab9c6b04a06faf3729b774bb0210f0f38e0e72b628f36b0ee4c5d807d2f92`
