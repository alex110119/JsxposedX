# JsxposedX

- Frida module: [`jsxposedx-frida`](https://github.com/dugongzi/jsxposedx-frida)
- English: [`README_EN.md`](README_EN.md)
- 中文：[`README_CN.md`](README_CN.md)
- Release：[`Download`](https://jsxposed.org)
- 沐雪AI中转站[`前往`](https://api.muxueai.pro)

> **This repository / 本仓库说明**
>
> Forked from [`dugongzi/JsxposedX`](https://github.com/dugongzi/JsxposedX) at `6.1.1 (611)`.
> The focus of this branch is making the module work again on **libxposed API 102**
> (the new API shipped with LSPosed 2.x), where upstream fails to load. Current version `6.1.2 (612)`.
> Project ownership and license are unchanged; only compatibility and build fixes are applied.
> 中文详情见 [`README_CN.md`](README_CN.md)。

## What changed in this fork

**1. Module failed to load on API 102.**
Upstream's entry `NewApiHook` mixed two API generations: `XpHelper` is an old internal interface no
longer provided by API 102, and `MainHook` depends on `XposedHelpers` / `LoadPackageParam`, which only
exist in the legacy API. On LSPosed 2.x the module was loaded but died at `onPackageLoaded` with
`NoClassDefFoundError`, so scripts never ran.

- Added a `hook/compat` layer: `HookBridge` (backend registry + dispatch), `HookBackend`,
  `HookCallback`, `HookParam`, and `ReflectX` (self-contained reflection replacing `XposedHelpers`).
  Business code no longer references `de.robv.android.xposed.*` directly.
- Rebuilt the API 102 entry to use only the new API: `module.hook(executable).intercept(Chain)`,
  restoring the original `before` / `proceed` / `after` / `invokeOriginal` semantics via
  `Invoker.Type.ORIGIN`.
- Moved the legacy implementation into the `src/legacy` source set shared by the `api100` / `api101`
  flavors, leaving behaviour on the old API untouched.

**2. Script logs missing in the LSPosed manager.**
`XposedBridge.log` used to write to both logcat and the framework log. With the new API, calling only
`Log.i` keeps the in-app console working but leaves the LSPosed log page empty. `NewApi102Backend.log()`
now writes to both: `Log.i("Xposed", …)` for the in-app `JXCONSOLE` parser and
`module.log(INFO, "Xposed", …)` for the framework log.

**3. Windows builds stuck in CMake configure.**
Keystone's bundled LLVM script runs `if(PYTHON_VERSION_STRING VERSION_LESS 2.7)`. On Windows, CMake
often picks up the non-executable `C:\Windows\System32\python` app-execution alias, so
`find_package(Python3)` fails, the variable stays empty and the `if()` becomes a syntax error:

```
CMake Error at .../keystone-src/llvm/CMakeLists.txt:346 (if):
  if given arguments: "VERSION_LESS" "2.7"  Unknown arguments specified
```

`android/app/src/main/cpp/CMakeLists.txt` now probes `py -3` / `python3` / `python`, validates the
major version, and falls back to a placeholder version string so configuration no longer hard-fails.

**4. Build pipeline.**
ProGuard gained `-dontwarn io.github.libxposed.**`; CI fetches the Gradle distribution from a mirror to
avoid intermittent 504s from GitHub Releases; each release build prints the APK signing certificate
SHA-256 and compares it with the upstream one.

## Build & signing

- GitHub Actions (`.github/workflows/build.yml`) builds the `api100` / `api101` / `api102` flavors for
  debug and release, and can be triggered manually via `workflow_dispatch`.
- Release signing uses repository secrets (`ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`,
  `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`); the keystore itself is never committed.
- Artifacts from this branch are signed by `CN=YiTan Interactive Entertainment`, which is **not** the
  same key as the upstream release (`CN=沐雪社区`) — the two cannot be installed over each other.

JsxposedX is a Flutter Android application for Xposed/LSPosed and Frida workflows.


This project has long adhered to free and open source sharing, aiming to lower the learning threshold and promote positive technology exchange

This project is an open source technical research tool, which is only used for legal purposes such as software debugging, program analysis, memory mechanism learning, development testing, and authorized environment research.

The author develops and publishes this project for the purpose of promoting technical exchange and learning, and does not provide any support for cheating, bypassing protection, or providing guidance on illegal use.

It is strictly prohibited to use the following acts:
• Cheating in online games, creating plug-ins, and automated illegal operations
• Interferes with the normal operation of the server or client
• Unauthorized modification of third-party program data
• Undermining the level playing field
• Any use that violates laws, regulations, or platform rules

Responsibility Description:

Users should ensure that their use of the service complies with local laws and regulations and relevant service terms. Any direct or indirect consequences arising from the use, dissemination, and secondary development of this project shall be borne by the user and have nothing to do with the author.

Open Source Notes:

The disclosure of the source code of this project does not encourage abuse. Any modification, distribution, commercialization, or illegal use by any third party based on this project has nothing to do with the original author's position.

The author reserves the right to:

The author has the right to refuse to provide support for any violating use and may adjust, suspend, or terminate the project's public content at any time.

By continuing to use this project, you are deemed to have read and agreed to this statement.

AI feature targeting

The built-in AI functions of this project are only used as general intelligent auxiliary modules, mainly used for data analysis, content interpretation, result screening, information organization, operation guidance, learning and research, and other scenarios to improve user efficiency and experience.

AI functions are auxiliary services and are not directly involved in the operation control, data destruction, permission bypass, cheat execution, or other illegal operations of any target program.

Built-in interface description

For user convenience, this project provides default AI interface access services. Users can also configure and use legally obtained third-party AI service interfaces according to their own needs, and there are no mandatory restrictions on this project.

Fee description

If some AI functions involve server resources, model call costs, computing power consumption, or interface fees, the project team may charge reasonable costs for related services. This fee only corresponds to the consumption of AI service resources and does not represent support or charges for any violating uses.

Output disclaimer

The AI output content is automatically generated based on the model and may contain errors, omissions, inaccuracies, or circumstances that may not be suitable for specific scenarios, and is for reference only. Users should make their own judgment and bear the responsibility for their final use.

Usage specifications

It is strictly prohibited to use the AI features of this project to engage in the following acts:
• Plugin production, cheat assistance, and automated illegal operations
• Hack, attack, or interfere with third-party systems
• Infringe on the legitimate rights and interests of others
• Violations of laws, regulations and platform rules

Attribution of responsibility

Any actions and consequences implemented by the user based on the AI functions of this project shall be borne by the user and have nothing to do with the project author.

Author's position

The author has always advocated for the use of AI technology in legal, legitimate, and positive learning research and efficiency improvement scenarios, and opposes any abuse.

## Summary

- Flutter UI with Android-side Xposed hooks, LSPosed service integration, and native bridge modules
- Project entries for `Quick Functions`, `AI Reverse`, `Xposed Project`, and `Frida Project`
- Additional pages for crypto audit and SO analysis
- Pigeon code generation via `.buildScript/pigen_watch.ps1` on Windows or `.buildScript/pigen_watch.sh` on macOS/Linux
- Debug install flow via `.buildScript/run_install_debug.ps1` on Windows or `.buildScript/run_install_debug.sh` on macOS/Linux
- Shared run configurations in `.idea/runConfigurations/`: `watch_pigeons` and `build_for_xposed_type`
## Build Note

This repository is not a normal Flutter-only app. Device-side verification also involves the Android/Xposed side, so the repository includes shared PowerShell scripts and shared IDE run configurations.

The Android module now uses two thin Xposed shells:

- `android/app/src/api100/` for the legacy `api100` shell
- `android/app/src/api101/` for the modern `api101` shell
- `android/app/src/main/` for shared Flutter/UI and hook core code

The debug install script builds, installs, launches, and attaches to the app. Run it from the repository root. Default debug tasks point to `api100`.

```powershell
.\.buildScript\run_install_debug.ps1
```

```bash
./.buildScript/run_install_debug.sh
```

To install and launch on macOS/Linux without keeping the terminal in `flutter attach`:

```bash
./.buildScript/run_install_debug.sh --skip-attach
```

Install the `api101` shell explicitly with:

```powershell
.\.buildScript\run_install_debug.ps1 -GradleTask :app:installApi101Debug
```

```bash
./.buildScript/run_install_debug.sh --gradle-task :app:installApi101Debug
```

The script selects `--device-id`, then `ANDROID_SERIAL`, then the Android Studio selected device, and finally a single connected `adb` device. On macOS it selects JDK 17 for Gradle and uses a single-use daemon so stale daemons are not reused. Override the JDK with `--java-home PATH`. Gradle output is also saved to `build/logs/install-debug.log`. To diagnose a failure on macOS/Linux, rerun with:

```bash
./.buildScript/run_install_debug.sh --skip-attach --gradle-arg --stacktrace
```

Running `android/gradlew` without a task only displays Gradle help; it does not build or install the app.

Build/release note:

- `flutter build appbundle --flavor api100 --release` goes through the Flutter CLI first, then calls the matching Android variant
- `flutter build apk --flavor api101 --release` builds the corresponding release APK, for example `build/app/outputs/flutter-apk/app-api101-release.apk`
- `.\gradlew.bat :app:bundleApi100Release` goes directly through Android Gradle
- `.\gradlew.bat :app:assembleApi101Release` goes directly through Android Gradle for the matching release APK
- for the same flavor, both commands target the same Android release variant output
