## General requirements

- Java 17 (should be specified in JAVA_HOME)
- Android SDK downloaded from Android Studio and specified in `ANDROID_SDK_ROOT`

## Developing in IDE

1. Download Android Studio from [the official site](https://developer.android.com/studio/archive) (it is mandatory to use the version, written [here](https://github.com/JetBrains/androidx/blob/jb-main/gradle/libs.versions.toml#L11)). As an alternative you can use IDEA, which is compatible with [this AGP version](https://github.com/JetBrains/androidx/blob/jb-main/gradle/libs.versions.toml#L5), or you can disable Android plugin in IDEA plugins, to develop non-Android targets.
2. Download Android SDK via [Android Studio](https://developer.android.com/studio/intro/update#sdk-manager) and specify it in `ANDROID_SDK_ROOT` environment variable.
4. Specify Gradle JVM to use JDK 17 in InteliJ IDEA Preferences (`Build, Execution, Deployment -> Build Tools -> Gradle`)

### Run tests

Run tests for Desktop:

```bash
./gradlew desktopTest
```

Run tests for Web:

```bash
./gradlew :mpp:testWeb
```

Run tests for iOS:

```bash
./gradlew :mpp:testIos'
```

Run iOS instrumented tests.
Note: To ensure the test runs on an iOS simulator with a detached hardware keyboard,
we must shut down all simulators and update the ConnectHardwareKeyboard flag.
```bash
xcrun simctl shutdown all

defaults write com.apple.iphonesimulator ConnectHardwareKeyboard -bool false

cd compose/ui/ui/src/uikitInstrumentedTest/launcher

xcodebuild test -scheme Launcher -project Launcher.xcodeproj -destination 'platform=iOS Simulator,name=iPhone 16'
```

### API checks

Compose Multiplatform stores all public API in *.api files. If any API is added/changed, `./gradlew jbApiCheck` will fail with an error that API is changed (it runs on CI). Example:

```
Execution failed for task ':compose:material3:material3:desktopApiCheck'.
> API check failed for project material3.
  --- D:\Work\compose-multiplatform-core\compose\material3\material3\api\desktop\material3.api
  +++ D:\Work\compose-multiplatform-core\out\androidx\compose\material3\material3\build\api\desktop\material3.api
  @@ -552,6 +552,11 @@
   public abstract interface annotation class androidx/compose/material3/ExperimentalMaterial3Api : java/lang/annotation/Annotation {
   }

  +public final class androidx/compose/material3/FF {
  +     public static final field $stable I
  +     public fun <init> ()V
  +}
  +
   public final class androidx/compose/material3/FabPosition {
        public static final field Companion Landroidx/compose/material3/FabPosition$Companion;
        public static final synthetic fun box-impl (I)Landroidx/compose/material3/FabPosition;

   You can run :material3:apiDump task to overwrite API declarations
```

To fix this error:

1. Run `./gradlew jbApiDump` or (for Linux/Window host) [CI task](https://teamcity.jetbrains.com/buildConfiguration/JetBrainsPublicProjects_Compose_CommitDumpApi) that creates a commit "Dump API" in a branch
2. See what has changed in *.api files.
3. If there are only additions - there is no binary incompatible change.
4. If there are some removals - most probably there is a binary incompatible change and it needs to be fixed before merging it to the main branch.

### Publishing

Compose Multiplatform core libraries can be published to local Maven with the following steps:

1. Use these gradle properties to set the published libraries versions

   `-Pjetbrains.publication.version.CORE_BUNDLE`,
   `-Pjetbrains.publication.version.CORE_URI`,
   `-Pjetbrains.publication.version.COMPOSE`,
   `-Pjetbrains.publication.version.COMPOSE_MATERIAL3_ADAPTIVE`,
   `-Pjetbrains.publication.version.LIFECYCLE`,
   `-Pjetbrains.publication.version.NAVIGATION`,
   `-Pjetbrains.publication.version.NAVIGATION_3`,
   `-Pjetbrains.publication.version.NAVIGATION_EVENT`,
   `-Pjetbrains.publication.version.SAVEDSTATE`,
   `-Pjetbrains.publication.version.WINDOW`,

   The default value for the version is `0.0.0-SNAPSHOT`

   And library groups:
   `-Pjetbrains.publication.libraries=COMPOSE,COMPOSE_MATERIAL3_ADAPTIVE,LIFECYCLE,NAVIGATION,NAVIGATION_3,NAVIGATION_EVENT,SAVEDSTATE,WINDOW`

   The default value includes all libraries.

2. Publish core libraries

   ```bash
   ./gradlew :mpp:publishComposeJbToMavenLocal -Pcompose.platforms=all -Pjetbrains.publication.version.COMPOSE=0.1.0-dev1000 -Pjetbrains.publication.version.LIFECYCLE=0.1.0-dev1000
   ```

   `-Pcompose.platforms=all` could be replace with comma-separated list of platforms, such as `js,jvm,androidDebug,androidRelease,macosx64,ios`.

3. Publish extended icons

   ```bash
   ./gradlew :mpp:publishComposeJbExtendedIconsToMavenLocal -Pcompose.platforms=all --max-workers=1
   ```

4. (Optional) Publish Gradle plugin using [instructions](https://github.com/JetBrains/compose-multiplatform/tree/master/compose#publishing) to check changes locally.

### Run samples

Run jvm desktop samples:

```bash
./gradlew :compose:mpp:demo:runDesktop
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:run1
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:run2
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:run3
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:runSwing
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:runWindowApi
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:runVsync
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples:runLayout
```

```bash
./gradlew :compose:desktop:desktop:desktop-samples-material3:runScaffold
```

Run wasm sample:

```bash
./gradlew :compose:mpp:demo:jsRun
```

Run native macos X64 sample:

```bash
./gradlew :compose:mpp:demo:runDebugExecutableMacosX64
```

Run native macos Arm64 sample:

```bash
./gradlew :compose:mpp:demo:runDebugExecutableMacosArm64
```

### Run in KMP Wizard project

To use a locally built compose in [KMP with Compose wizard project](https://kmp.jetbrains.com) you need to perform some extra steps:

- Checkout <https://github.com/JetBrains/compose-multiplatform>.
- Open `gradle-plugins` in `compose-multiplatform`.
- Update `gradle.properties` by setting `compose.version` and `deploy.version` to the version you've published (`0.1.0-dev1000` in example above).
- Run `./gradlew publishToMavenLocal`.
- Open `components` in `compose-multiplatform`.
- Update `gradle.properties` by setting `compose.useMavenLocal` to `true` and `compose.version` to the version you've published.
- Run `./gradlew publishToMavenLocal`.
- Open KMP wizard project.
- Update `settings.gradle.kts` by adding `mavenLocal()` to the end of both `repositories` blocks.
- Update `gradle/libs.versions.toml` by setting `compose-plugin` to the version you've published.
- Sync gradle.

Now the project will build with the locally published Compose.

### Run mpp/demo-swiftui sample on iOS with Xcode

Open the `iosApp.xcodeproj` with XCode and press the Run button.

### Run mpp/demo sample on iOS with Xcode

Run script:

```bash
./compose/mpp/demo/regenerate_xcode_project.sh
```

Wait while Xcode is opening, and press run button.

### Clean IDE and Gradle cache

- Close project

- ```bash
  ./cleanTempFiles.sh
  ```
