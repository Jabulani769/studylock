# 15 — Developer Workstation Setup (Manjaro Linux)

## Known workstation context
The current known setup is a Lenovo ThinkPad T450 running Manjaro Linux, Niri/Wayland, Intel HD 5500, approximately 7.6 GiB RAM and an x86_64 CPU. Treat these values as a starting point; verify the machine before installing.

## Recommended tools
### Required
1. **Android Studio** — official IDE; includes a compatible JetBrains Runtime (JBR) for Gradle.
2. **Android SDK Platform Tools** — includes `adb`, used to install, inspect and debug on a physical Android phone.
3. **Android SDK Platform + Build Tools** — install the API level selected by the project, not every platform.
4. **Git** — version control.
5. **Kotlin** — language used by the project; Android Studio/Gradle manage the compiler, so do not install random standalone Kotlin versions.
6. **A physical Android phone + USB cable** — preferred test target for this laptop.
7. **OpenJDK only if required by command-line tooling** — prefer the JBR bundled with Android Studio and use one consistent JDK for Studio and terminal Gradle builds.

### Useful, optional
- `scrcpy` for viewing/controlling a connected Android phone from Linux.
- `jq` for inspecting JSON output.
- `shellcheck` for shell scripts.
- `ktlint` or Detekt only if adopted by the project.
- `fastboot` only if device-level debugging genuinely requires it.
- A graphical Git client is optional; Git CLI is sufficient.

### Do not install initially
- Multiple JDK versions without a project need.
- Multiple Android SDK platforms/system images.
- Android NDK/CMake unless a dependency requires native code.
- Flutter, React Native, Node.js, Python, Docker or a database server for this native Kotlin app unless a later approved feature needs them.
- A third-party emulator. Prefer a real phone.

## Memory-conscious workflow
- Do not run an Android Emulator on the 7.6 GiB laptop as the normal workflow.
- Close browser tabs and other heavy applications before a full build.
- Keep one Android Studio project open.
- Use a modest IDE heap; start around 2 GiB and adjust only if needed.
- Limit Gradle workers and parallel builds if the laptop starts swapping.
- Build from terminal for repeatable checks; use Studio mainly for editing, inspection and debugging.
- Use a physical phone with Developer Options and USB debugging enabled.
- Disable USB debugging when not developing.
- Keep SDK and Gradle caches on the disk with adequate free space; monitor disk usage.
- Do not delete Gradle caches as a routine fix.

## Manjaro installation approach
Use the official Android Studio Linux distribution from the Android Developers website, or a trusted Manjaro/AUR package only if you understand its maintenance source. The official archive avoids depending on a community packaging recipe. Install SDK components through Android Studio's SDK Manager.

Before installing, inspect current system state:
```bash
uname -m
free -h
df -h "$HOME"
java -version
git --version
```

After Android Studio setup, verify:
```bash
adb version
adb devices
```

If `adb` is not on PATH, add the Android SDK `platform-tools` directory to PATH. Typical SDK location is `~/Android/Sdk`, but use the exact location selected in SDK Manager.

## Physical phone setup
1. Enable Developer Options on the phone.
2. Enable USB debugging.
3. Connect with a data-capable USB cable.
4. Accept the computer authorization prompt on the phone.
5. Run `adb devices` and verify the device is listed as `device`, not `unauthorized`.
6. Install and test debug builds only from the trusted development machine.
7. Revoke USB debugging authorizations when no longer needed.

## Emulator decision
The emulator is optional. If later required, use one small virtual device, one API image, modest RAM allocation and close it after testing. Do not install several images or keep it running alongside heavy workloads. A physical device is more representative for app restrictions, OEM battery behavior, permissions and lockdown testing.

## Project setup conventions
- Use Gradle Wrapper committed to the repository.
- Do not require a globally installed Gradle.
- Use Android Studio's bundled JBR unless project documentation says otherwise.
- Keep `local.properties`, signing files and machine-specific paths out of Git.
- Keep SDK/Gradle versions pinned in project files.
- Record exact setup commands only after the initial project is created and tested on this machine.

## Verified on this workstation (2026-11-15)
- The system default `java` is JDK 26, which Gradle 8.10.2 does not support. CLI builds must use the installed JDK 21:
  ```bash
  JAVA_HOME=/home/jabu/jdks/jdk-21.0.5+11 ./gradlew :app:assembleDebug
  ```
- Install and start the debug build:
  ```bash
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  adb shell monkey -p org.studylock.app -c android.intent.category.LAUNCHER 1
  ```
- Verified on a Samsung SM-A032F (Android 13, API 33).
- The app `namespace` must match the Kotlin source package (`studylock.app`); the `applicationId` remains `org.studylock.app`. A mismatch makes manifest entries such as `.MainActivity` reference classes that do not exist and the process crashes at launch.

## Official references
- Android Studio download and installation: https://developer.android.com/studio
- Android Studio system requirements: https://developer.android.com/studio/install
- Android Debug Bridge: https://developer.android.com/tools/adb
- Android SDK Manager: https://developer.android.com/tools/sdkmanager
- Jetpack Compose: https://developer.android.com/develop/ui/compose
- Room: https://developer.android.com/training/data-storage/room
- WorkManager: https://developer.android.com/develop/background-work/background-tasks/persistent
- Accessibility services: https://developer.android.com/guide/topics/ui/accessibility/service
- Device policy management: https://developer.android.com/work/dpc/device-management

Check these official pages again when setting up because Android tooling and platform policies change.
