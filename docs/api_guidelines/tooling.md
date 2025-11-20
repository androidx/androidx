## Tooling {#tooling}

In some cases, libraries may need to write public-facing tooling like lint
checks or -- less frequently -- Gradle plugins, annotation processors, or
command-line tools to provide a consistent and high-quality developer
experience.

### Lint checks {#tooling-lint}

Lint is a static analysis tool that checks Android project source files. Lint
checks come with Android Studio by default, but custom lint checks can be
bundled inside AARs or distributed as standalone JARs to help avoid potential
bugs and encourage best practices.

Writing lint checks is generally low-risk, but writing correct lint checks that
support all use cases (for example, both Kotlin and Java sources) is complicated
and not well-documented. We have also seen frequent issues with compatibility
that result in lint checks being silently disabled on the client side, so we
recommend that any critical correctness issues be handled as part of API design
and documentation.

See go/androidx/lint_guide for implementation details.

Libraries that ship bundled lint checks include Compose, Work Manager, Room,
Fragment, and many others.

### Gradle plugins {#tooling-gradle-plugin}

In rare cases, a library may need a Gradle plugin to help developers integrate
library behavior with functions performed by Android Gradle Plugin or the build
system.

Unlike lint checks, writing Gradle plugins can be high-risk since they integrate
with the build system and can affect the overall performance and correctness of
every project in a repository. Failing to handle project isolation or
incorrectly annotating task inputs can have serious consequences.

Writing Gradle plugins -- especially plugins that integrate with Android Gradle
Plugin -- is unfortunately also complicated and not always documented in a way
that allows jumping right into writing code. We **require** that any team
attempting to write a Gradle plugin works directly with the Android Gradle
Plugin team, at a minimum, to review their initial design doc and provide
ongoing consultation to ensure correctness and adherence to best practices.

Keep in mind that not all developers use Gradle, and you should attempt to write
your library in such a way that a third-party can integrate with Bazel, Buck, or
other build systems.

Libraries that include a Gradle plugin include Room, Benchmark, and Stable AIDL
(internal-only).

### Annotation processors {#tooling-annotation-processor}

Some libraries work based on annotating developer source code and generating
code that interacts with their library. Historically, libraries targeting Kotlin
sources used `kapt`, but more modern libraries use Kotlin Symbol Processing
(KSP). KSP allows development of lightweight compiler plugins and covers
annotation processing as well as more advanced use cases.

There are many examples available for writing KSP plugins, but the KSP APIs
themselves continue to evolve and library owners should exercise caution as
compatibility may break when clients update their Kotlin compiler version.

Many libraries that include a KSP plugin will also need to develop a Gradle
plugin to help developers integrate the plugin's functionality with Android
Gradle Plugin.

We **require** that library owners consult with an existing team that has
shipped and maintained a KSP plugin before committing to writing their own, and
that they receive ongoing support to ensure correctness.

Libraries that ship a KSP compiler plugin include Room.
