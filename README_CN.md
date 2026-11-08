# JsxposedX

- 英文文档：[`README_EN.md`](README_EN.md)
- 默认 README：[`README.md`](README.md)
- 发布版本：[`Download`](https://jsxposed.org)

> **关于本仓库**
>
> 本仓库取自 [`dugongzi/JsxposedX`](https://github.com/dugongzi/JsxposedX)，基线为 `6.1.1 (611)`，
> 主要针对 **libxposed API 102（LSPosed 2.x 新 API）下模块无法正常工作** 的问题做了重构，
> 当前版本 `6.1.2 (612)`。项目归属与开源协议不变，本分支只做兼容性修复与构建链路完善。

## 相对上游的改动

### 1. 模块在 API 102 下加载失败

上游的模块入口 `NewApiHook` 同时混用了两套 API：`XpHelper` 是旧版内部接口，API 102 的
LSPosed 已不再提供；`MainHook` 又依赖 `XposedHelpers`、`LoadPackageParam` 等旧 API 才有的类型。
在 LSPosed 2.x 上的表现是：模块能被加载，日志走到 `Loading class com.jsxposed.x.NewApiHook`
之后就不再往下，`onPackageLoaded` 阶段抛 `NoClassDefFoundError`，脚本完全不生效。

本分支的处理方式：

- **新增 `hook/compat` 抽象层**——`HookBridge`（后端注册与统一分发）、`HookBackend`、
  `HookCallback`、`HookParam`、`ReflectX`（自实现反射工具，替代 `XposedHelpers`）。
  业务代码只依赖这层抽象，不再直接引用 `de.robv.android.xposed.*`。
- **API 102 入口彻底去 legacy**——`NewApi102Backend` 直接使用
  `module.hook(executable).intercept(Chain)`，把原来的 `beforeHookedMethod` /
  `afterHookedMethod` / `invokeOriginalMethod` 语义完整还原（原方法调用走
  `Invoker.Type.ORIGIN`），构造方法 Hook 同样按 ORIGIN 方式调用。
- **legacy 实现下沉**——原来基于 `de.robv` 的实现整体迁移到 `src/legacy` 源集，
  由 `api100` / `api101` 两个 flavor 共用，行为与上游保持一致，不影响老框架用户。

### 2. LSPosed 管理器里看不到脚本日志

旧实现走 `XposedBridge.log`，会同时写入 logcat 和 LSPosed 的模块日志通道。改用新 API 后
如果只调用 `Log.i`，应用内控制台正常，但 LSPosed 管理器的日志页是空的。现在
`NewApi102Backend.log()` 同时写两处：`Log.i("Xposed", …)` 供应用内控制台解析 `JXCONSOLE`
协议，`module.log(INFO, "Xposed", …)` 供 LSPosed 日志页展示。

### 3. Windows 本地构建卡在 CMake 配置阶段

`keystone` 依赖的 LLVM 脚本会执行 `if(PYTHON_VERSION_STRING VERSION_LESS 2.7)`。Windows 的
PATH 里常有 `C:\Windows\System32\python` 这类「应用执行别名」（实际不可执行），CMake 选中
它之后 `find_package(Python3)` 判定失败，变量为空，`if()` 直接语法报错：

```
CMake Error at .../keystone-src/llvm/CMakeLists.txt:346 (if):
  if given arguments: "VERSION_LESS" "2.7"  Unknown arguments specified
```

`android/app/src/main/cpp/CMakeLists.txt` 增加了容错探测：依次尝试 `py -3` / `python3` /
`python` 并校验主版本号，仍然找不到时给一个合法版本串兜底，保证配置阶段不再硬失败。

### 4. 构建链路

- ProGuard 增加 `-dontwarn io.github.libxposed.**`，避免 R8 在 release 构建报缺失类。
- CI 中 Gradle 分发改用镜像，规避 GitHub Releases 偶发 504 导致的构建中断。
- 构建完成后自动打印 APK 签名证书 SHA-256 并与原版比对，便于确认产物来源。

## 构建说明

- 构建走 GitHub Actions（`.github/workflows/build.yml`），覆盖 `api100` / `api101` / `api102`
  三个 flavor 的 debug / release 组合，也支持手动 `workflow_dispatch` 指定单个 flavor。
- release 签名由仓库 Secrets 注入（`ANDROID_KEYSTORE_BASE64`、`ANDROID_KEYSTORE_PASSWORD`、
  `ANDROID_KEY_ALIAS`、`ANDROID_KEY_PASSWORD`），密钥文件不入库。
- 本分支产物的证书主体为 `CN=YiTan Interactive Entertainment`，与上游发行版的
  `CN=沐雪社区` **不是同一把密钥**，两者无法相互覆盖安装，切换时需要卸载或使用
  CorePatch 之类关闭签名校验的方案。

JsxposedX 是一个面向 Xposed/LSPosed 和 Frida 工作流的 Flutter Android 应用。它把 Flutter UI 层、Android 侧的 Xposed Hook、LSPosed service 集成、原生 bridge 模块、共享 IDE 运行配置，以及 PowerShell 安装调试脚本放在同一个仓库里。



本项目长期坚持免费开源共享，旨在降低学习门槛，推动技术正向交流

本项目为开源技术研究工具，仅用于 软件调试、程序分析、内存机制学习、开发测试及授权环境研究 等合法用途。

作者开发并公开本项目的目的在于促进技术交流与学习，不针对任何特定游戏、平台、软件或在线服务，不提供任何作弊支持、绕过保护支持或违规使用指导。

严禁用于以下行为：
•	网络游戏作弊、外挂制作、自动化违规操作
•	干扰服务器或客户端正常运行
•	未经授权修改第三方程序数据
•	破坏公平竞争环境
•	任何违反法律法规或平台规则的用途

责任说明：

使用者应自行确保其使用行为符合所在地法律法规及相关服务条款。因使用、传播、二次开发本项目产生的任何直接或间接后果，由使用者自行承担，与作者无关。

开源说明：

本项目源代码公开不代表鼓励滥用行为。任何第三方基于本项目进行的修改、分发、商业化、非法用途，均与原作者立场无关。

作者保留权利：

作者有权拒绝提供任何违规用途支持，并可随时调整、暂停或终止项目公开内容。

继续使用本项目，即视为已阅读并同意本声明。

AI 功能定位

本项目内置 AI 功能仅作为通用智能辅助模块使用，主要用于数据分析、内容解释、结果筛选、信息整理、操作指引及学习研究等场景，用于提升用户使用效率与体验。

AI 功能属于辅助性服务，不直接参与任何目标程序的运行控制、数据破坏、权限绕过、作弊执行或其他违规操作。

内置接口说明

为方便用户使用，本项目提供默认 AI 接口接入服务。用户亦可根据自身需求，自行配置并使用合法取得的第三方 AI 服务接口，本项目不作强制限制。

费用说明

如部分 AI 功能涉及服务器资源、模型调用成本、算力消耗或接口费用，项目方可能对相关服务收取合理成本费用。该费用仅对应 AI 服务资源消耗，不代表对任何违规用途提供支持或收费。

输出免责说明

AI 输出内容基于模型自动生成，可能存在误差、遗漏、不准确或不适用于特定场景的情况，仅供参考。用户应自行判断并承担最终使用责任。

使用规范

严禁利用本项目 AI 功能从事以下行为：
•	外挂制作、作弊辅助、自动化违规操作
•	破解、攻击、干扰第三方系统
•	侵害他人合法权益
•	违反法律法规及平台规则的行为

责任归属

用户基于本项目 AI 功能实施的任何行为及产生的一切后果，由用户自行承担，与项目作者无关。

作者立场

作者始终倡导 AI 技术用于合法、正当、积极的学习研究与效率提升场景，反对任何滥用行为。
## 项目概览

- 应用名：`JsxposedX`
- 语言：英文、简体中文
- 主标签页：`Home`、`Project`、`Repository`、`Settings`
- Home 状态卡片：`AI`、`Root`、`Xposed`、`Frida`、`Zygisk module`
- Project 入口：`Quick Functions`、`AI Reverse`、`Xposed Project`、`Frida Project`

## 主要功能

- Xposed 项目页、代码编辑页、可视化编辑页
- Frida 项目页、脚本管理、目标开关、Hook 打包、编辑页
- AI Reverse 页面，包含 AI 对话、APK 上下文和 SO 分析工具
- Quick Functions 常用 Hook 开关
- Crypto audit log 和 JS editor
- SO analysis 页面，包含 `ELF Info`、`Exports`、`Imports`、`JNI`、`Strings`、`Sections`
- 内置文档位于 `assets/raws/JsxposedX_API.md`、`assets/raws/JsxposedX_API_en.md`、`assets/raws/Frida_API.md`、`assets/raws/Frida_API_en.md`
- `Repository` 标签页当前还是占位页

## 相关仓库

- Frida 模块仓库：[`dugongzi/jsxposedx-frida`](https://github.com/dugongzi/jsxposedx-frida)

## Quick Functions

当前代码里的 quick-function key：

- `injectTip`
- `removeScreenshotDetection`
- `removeDialogs`
- `removeCaptureDetection`
- `modifiedVersion`
- `hideXposed`
- `hideRoot`
- `hideApps`
- `algorithmicTracking`

## 为什么构建和调试流程不同于普通 Flutter App

这个仓库不只是 Flutter UI 项目。

- `android/app/src/main/AndroidManifest.xml` 声明了 Xposed 模块元数据
- `android/app/src/api100/` 和 `android/app/src/api101/` 各自维护一套很薄的 Xposed 壳
- `android/app/src/api100/assets/xposed_init` 指向 `com.jsxposed.x.MainHook`
- `android/app/src/api100/resources/META-INF/xposed/module.prop` 对应 `api100`
- `android/app/src/api101/resources/META-INF/xposed/module.prop` 对应 `api101`
- `android/app/src/main/` 只保留 Flutter UI、共享 Hook 核心和原生 bridge
- `android/app/src/main/kotlin/com/jsxposed/x/App.kt` 初始化 LSPosed service
- `android/app/src/main/kotlin/com/jsxposed/x/NativeProvider.kt` 注册了 `Pinia`、`StatusManagement`、`App`、`Project`、`ApkAnalysis`、`SoAnalysis`、`LSPosed`、`ZygiskFrida` 这些原生 bridge 模块

所以安装和验证流程会同时涉及 Android/Xposed 侧与 Flutter 侧。

## 环境要求

- Flutter SDK
- Android SDK 与 `adb`
- Windows PowerShell，仓库里的共享脚本基于它
- 用于 Xposed/LSPosed 流程的 root 测试环境
- 用于 Frida 流程的 Zygisk/Frida 环境

## 关键目录

- `lib/`：Flutter UI、路由、provider、功能页面
- `lib/pigeons/`：Pigeon bridge 定义
- `lib/generated/`：生成出来的 Dart bridge 代码
- `android/app/src/main/kotlin/com/jsxposed/x/`：共享 Android 代码、Hook 核心、原生 bridge 实现
- `android/app/src/api100/`：`api100` 壳、入口类与模块资源
- `android/app/src/api101/`：`api101` 壳、入口类与模块资源
- `.buildScript/`：Windows PowerShell 与 macOS/Linux Shell 代码生成、Debug 安装脚本
- `.idea/runConfigurations/`：共享 IDE 运行配置

## 共享 IDE 运行配置

这个仓库提交了 `.idea/runConfigurations/`。

拉取仓库后，JetBrains IDE 可以加载这些共享运行配置：

- `watch_pigeons` -> Windows 执行 `.buildScript/pigen_watch.ps1`，macOS/Linux 执行 `.buildScript/pigen_watch.sh`
- `build_for_xposed_type` -> 执行 `.buildScript/run_install_debug.ps1 -SkipAttach`

## Pigeon 代码生成

`.buildScript/pigen_watch.ps1` 和 `.buildScript/pigen_watch.sh` 会监听 `lib/pigeons/**/*.dart`，并且：

- 生成 Dart bridge 文件到 `lib/generated`
- 生成 Kotlin bridge 文件到 `android/app/src/main/kotlin/...`
- 当 `*NativeImpl.kt` 不存在时创建模板文件

在仓库根目录运行：

```powershell
.\.buildScript\pigen_watch.ps1
```

macOS/Linux：

```bash
./.buildScript/pigen_watch.sh
```

只生成一次、不启动监听：

```bash
./.buildScript/pigen_watch.sh --once
```

## Debug 安装脚本

`.buildScript/run_install_debug.ps1` 和 `.buildScript/run_install_debug.sh` 是当前仓库里的 Debug 安装脚本。

它会：

- 默认执行 `:app:installDebug`，这个任务已映射到 `api100Debug`
- 把 `pubspec.yaml` 里的 `versionName` 和 `versionCode` 同步到 `android/local.properties`
- 从 `-DeviceId`、`ANDROID_SERIAL`、Android Studio 当前选中设备或单个已连接 `adb` 设备中解析目标设备
- macOS 默认使用 JDK 17，并以 `--no-daemon` 构建，避免复用长期运行后内存耗尽的 Gradle daemon
- 安装完成后等待一段时间，给包替换广播和 LSPosed 重扫留时间
- 可以执行 force-stop、启动应用以及 `flutter attach`

在仓库根目录运行：

```powershell
.\.buildScript\run_install_debug.ps1
```

macOS/Linux：

```bash
./.buildScript/run_install_debug.sh
```

该命令默认依次完成 `:app:installDebug`、等待 LSPosed 重扫、冷启动应用和 `flutter attach`。它不是裸 `android/gradlew` 命令；直接运行不带任务的 `android/gradlew` 只会显示 Gradle 帮助，不会打包或安装。

仅安装和启动、不自动 attach：

```bash
./.buildScript/run_install_debug.sh --skip-attach
```

常用参数：

```powershell
.\.buildScript\run_install_debug.ps1 -SkipAttach
.\.buildScript\run_install_debug.ps1 -SkipLaunch
.\.buildScript\run_install_debug.ps1 -DeviceId <serial>
.\.buildScript\run_install_debug.ps1 -GradleTask :app:installApi101Debug
```

macOS/Linux 对应参数：

```bash
./.buildScript/run_install_debug.sh --skip-attach
./.buildScript/run_install_debug.sh --skip-launch
./.buildScript/run_install_debug.sh --device-id <serial>
./.buildScript/run_install_debug.sh --java-home /path/to/jdk
./.buildScript/run_install_debug.sh --gradle-task :app:installApi101Debug
```

指定设备和查看 Gradle 完整错误：

```bash
./.buildScript/run_install_debug.sh --device-id <serial> --skip-attach
./.buildScript/run_install_debug.sh --skip-attach --gradle-arg --stacktrace
```

如果脚本在 `Installing app with Gradle task ...` 后停止，说明 Gradle 任务返回了非零退出码。脚本会同步显示 Gradle 输出，并将完整日志保存到 `build/logs/install-debug.log`；加上 `--gradle-arg --stacktrace` 可查看更完整的构建错误。

补充说明：

- `installDebug` / `assembleDebug` 默认仍然走 `api100`
- 要安装 `api101` 壳，请在 PowerShell 显式传 `-GradleTask :app:installApi101Debug`，或在 shell 中传 `--gradle-task :app:installApi101Debug`

这个脚本用于 Xposed/LSPosed 的 Debug 安装流程，不是普通 Flutter hot reload 的替代品。

## 常规构建

Flutter 常规构建命令仍然可以直接使用：

```powershell
flutter build apk --debug
flutter build apk --release
flutter build apk --flavor api100 --release
flutter build apk --flavor api101 --release
flutter build appbundle --flavor api100 --release
flutter build appbundle --flavor api101 --release
```

如果你需要 `api101` 的 release APK，可以直接运行：

```powershell
flutter build apk --flavor api101 --release
```

输出文件通常在：

```text
build/app/outputs/flutter-apk/app-api101-release.apk
```

如果要直接构建某个 Xposed 壳，建议优先使用 Gradle 任务：

```powershell
cd android
.\gradlew.bat :app:assembleApi100Debug
.\gradlew.bat :app:assembleApi101Debug
.\gradlew.bat :app:assembleApi100Release
.\gradlew.bat :app:assembleApi101Release

cd android
.\gradlew.bat :app:bundleApi100Release
.\gradlew.bat :app:bundleApi101Release

```

两类命令的区别：

- `flutter build apk --flavor api101 --release`：从 Flutter CLI 进入，最终生成对应 flavor 的 release APK，例如 `build/app/outputs/flutter-apk/app-api101-release.apk`
- `flutter build appbundle --flavor api100 --release`：从 Flutter CLI 进入，先处理 Flutter/Dart 资源与插件，再调用对应的 Android Gradle 变体任务
- `.\gradlew.bat :app:bundleApi100Release`：直接从 Android Gradle 进入，更适合排查 flavor、Xposed 壳、资源合并和原生构建问题
- `.\gradlew.bat :app:assembleApi101Release`：直接从 Android Gradle 进入，适合只想拿对应 flavor 的 release APK
- 对同一个 flavor 来说，这两种命令最终目标的 Android 产物是一致的，例如 `api100` 对应 `bundleApi100Release`

`.buildScript/run_install_debug.ps1` 和 `.buildScript/run_install_debug.sh` 负责设备侧的安装、启动和 attach 流程。

## Release 签名

Release 签名通过本地 `android/key.properties` 加载。

- `android/key.properties` 不在仓库中
- keystore 文件不在仓库中
- 签名信息只保留在本地文件中

## 常见开发流程

```powershell
flutter pub get
.\.buildScript\pigen_watch.ps1
.\.buildScript\run_install_debug.ps1
flutter attach
```

macOS/Linux：

```bash
flutter pub get
./.buildScript/pigen_watch.sh --once
./.buildScript/run_install_debug.sh --skip-attach
flutter attach
```

安装完成后，再到设备侧做 LSPosed/Xposed 或 Zygisk/Frida 验证。

## 说明

- Windows 使用 `.ps1`，macOS/Linux 使用对应的 `.sh`，无需手动拆分打包、安装和启动步骤。
- `flutter run` 仍然可以用于普通 Flutter 迭代，这个仓库另外保留了 Android/Xposed 的安装验证流程。
