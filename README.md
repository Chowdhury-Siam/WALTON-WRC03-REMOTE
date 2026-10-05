# Quiet Remote 1.0.4

A lightweight, ad-free, offline Android IR remote for Walton WRC03.
Built for a Redmi Note 10 Pro controlling a Walton WD1-JX32-SY200.

## Install and test

Install the accompanying `Quiet-Remote-1.0.4.apk` on your phone. If Android
asks, allow installation from the app opening the APK. The package is
`dev.siam.quietremote`, so it installs alongside your existing remote.

Point the top of the phone at the TV. First test Volume + and −, then Menu,
navigation/OK, Input, digits and Power. Hold volume/channel/arrows to repeat.
Tap Numbers to replace the navigation pad with digits; tap Navigation to switch back.
The layout is fixed and never scrolls. Options controls button vibration.
The app remembers the selected pad and vibration preference.

All 25 commands use the exact WRC03 recordings from the working reference
profile. See SIGNALS.md. Settings, Guide, Info and dash had no usable WRC03
commands in the reference app; use Menu for TV settings.

IR is one-way; the app cannot tell whether the TV received a command.
The APK has been compiled, signed and structurally checked, and the signals
verified against the original DEX. A physical phone/TV test has not been run.

## Build

Open this folder in Android Studio, or use JDK 17 and Android SDK 35:

```sh
./gradlew assembleDebug
python3 tools/check_signals.py
mkdir -p build/selfcheck
javac -d build/selfcheck app/src/main/java/dev/siam/quietremote/Wrc03.java tools/SignalSelfTest.java
java -cp build/selfcheck SignalSelfTest
```

On Windows use `gradlew.bat assembleDebug`. The standard debug build produces
`app/build/outputs/apk/debug/app-debug.apk`. A GitHub Actions workflow builds
and uploads a debug APK artifact when you push to `main` or run it manually.

The accompanying APK is a development build. Its development signing key
is preserved in `development-signing/` so future personal test builds can
update the installation without uninstalling. It is intentionally not a
private production key. The Gradle debug build uses this same key. For public
distribution, create your own private release key and keep it outside the
source repository.

## Changes in 1.0.4

- CI SDK setup explicitly installs Android 35 packages, avoiding the removed `tools` package.
- Measure the smaller navigation pad before its buttons, fixing shifted arrows and clipped OK text.
- The pad keeps its compact size and bottom-center position.

## Changes in 1.0.3

- Smaller arrow and OK buttons form a compact square pad at the bottom of their panel.
- The pad fits within available window space and keeps controls closer for one-hand use.

## Changes in 1.0.2

- Smaller launcher artwork keeps the signal arcs inside launcher masks.
- Arrow, OK and number buttons have distinct green backgrounds and visible borders.
- Wider gaps separate keypad buttons. The layout remains fixed without scrolling.

## Changes in 1.0.1

- Proper Android adaptive launcher icon with a dark green full-bleed background.
- Removed the scrolling container. Navigation and digits share the same fixed area.
- Weighted control layout fits portrait and short/landscape windows without expanding.
- Button labels resize within their bounds; controls remain accessible by TalkBack.

## Implementation

Native Java and Android widgets, with no runtime dependencies. The only
permission is TRANSMIT_IR. There is no Internet permission, ad SDK, analytics,
login, background service, or native library. Android 8.0 or later and an IR
emitter are required; 38 kHz support is checked at startup.

Transmissions run on one background worker. A busy gate prevents overlapping
signals or a backlog. Hold repeats stop when released, dragged, cancelled,
when focus is lost, or when the app goes into the background. A very rapid
tap while another transmission is busy is ignored.

Copyright 2026 Siam. Original app code is licensed under MIT; see LICENSE.
The signal data is documented separately in SIGNALS.md.
