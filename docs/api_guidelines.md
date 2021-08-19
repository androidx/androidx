# Library API guidelines

[TOC]

## Introduction {#introduction}

This guide is an addendum to
s.android.com/api-guidelines,
which covers standard and practices for designing platform APIs.

All platform API design guidelines also apply to Jetpack libraries, with any
additional guidelines or exceptions noted in this document. Jetpack libraries
also follow
[explicit API mode](https://kotlinlang.org/docs/reference/whatsnew14.html#explicit-api-mode-for-library-authors)
for Kotlin libraries.

## Modules {#module}

### Packaging and naming {#module-naming}

Java packages within Jetpack follow the format `androidx.<feature-name>`. All
classes within a feature's artifact must reside within this package, and may
further subdivide into `androidx.<feature-name>.<layer>` using standard Android
layers (app, widget, etc.) or layers specific to the feature.

Maven specifications use the groupId format `androidx.<feature-name>` and
artifactId format `<feature-name>` to match the Java package. For example,
`androidx.core.role` uses the Maven spec `androidx.core:role`.

Sub-features that can be separated into their own artifact are recommended to
use the following formats:

-   Java package: `androidx.<feature-name>.<sub-feature>.<layer>`
-   Maven groupId: `androidx.<feature-name>`
-   Maven artifactId: `<feature-name>-<sub-feature>`

Gradle project names and directories follow the Maven spec format, substituting
the project name separator `:` or directory separator `/` for the Maven
separators `.` or `:`. For example, `androidx.core:core-role` would use project
name `:core:core-role` and directory `/core/core-role`.

New modules in androidx can be created using the
[project creator script](#module-creator).

#### Project directory structure {#module-structure}

Libraries developed in AndroidX follow a consistent project naming and directory
structure.

Library groups should organize their projects into directories and project names
(in brackets) as:

```
<feature-name>/
  <feature-name>-<sub-feature>/ [<feature-name>:<feature-name>-<sub-feature>]
    samples/ [<feature-name>:<feature-name>-<sub-feature>:samples]
  integration-tests/
    testapp/ [<feature-name>:testapp]
    testlib/ [<feature-name>:testlib]
```

For example, the `navigation` library group's directory structure is:

```
navigation/
  navigation-benchmark/ [navigation:navigation-benchmark]
  ...
  navigation-ui/ [navigation:navigation-ui]
  navigation-ui-ktx/ [navigation:navigation-ui-ktx]
  integration-tests/
    testapp/ [navigation:integration-tests:testapp]
```

#### Project creator script {#module-creation}

Note: The terms _project_, _module_, and _library_ are often used
interchangeably within AndroidX, with project being the technical term used by
Gradle to describe a build target, e.g. a library that maps to a single AAR.

New projects can be created using our
[project creation script](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:development/project-creator/?q=project-creator&ss=androidx%2Fplatform%2Fframeworks%2Fsupport)
available in our repo.

It will create a new project with the proper structure and configuration based
on your project needs!

To use it:

```sh
cd ~/androidx-main/frameworks/support && \
cd development/project-creator && \
./create_project.py androidx.foo foo-bar
```

#### Common sub-feature names {#module-naming-subfeature}

*   `-testing` for an artifact intended to be used while testing usages of your
    library, e.g. `androidx.room:room-testing`
*   `-core` for a low-level artifact that *may* contain public APIs but is
    primarily intended for use by other libraries in the group
*   `-ktx` for an Kotlin artifact that exposes idiomatic Kotlin APIs as an
    extension to a Java-only library (see
    [additional -ktx guidance](#module-ktx))
*   `-<third-party>` for an artifact that integrates an optional third-party API
    surface, e.g. `-proto` or `-rxjava2`. Note that a major version is included
    in the sub-feature name for third-party API surfaces where the major version
    indicates binary compatibility (only needed for post-1.x).

Artifacts **should not** use `-impl` or `-base` to indicate that a library is an
implementation detail shared within the group. Instead, use `-core`.

#### Splitting existing modules

Existing modules _should not_ be split into smaller modules; doing so creates
the potential for class duplication issues when a developer depends on a new
sub-module alongside the older top-level module. Consider the following
scenario:

*   `androidx.library:1.0.0`
    *   contains class `androidx.library.A`
    *   contains class `androidx.library.util.B`

This module is split, moving `androidx.library.util.B` to a new module:

*   `androidx.library:1.1.0`
    *   contains class `androidx.library.A`
    *   depends on `androidx.library.util:1.1.0`
*   `androidx.library.util:1.1.0`
    *   contains class `androidx.library.util.B`

A developer writes an app that depends directly on `androidx.library.util:1.1.0`
and also transitively pulls in `androidx.library:1.0.0`. Their app will no
longer compile due to class duplication of `androidx.library.util.B`.

While it is possible for the developer to fix this by manually specifying a
dependency on `androidx.library:1.1.0`, there is no easy way for the developer
to discover this solution from the class duplication error raised at compile
time.

Same-version groups are a special case for this rule. Existing modules that are
already in a same-version group may be split into sub-modules provided that (a)
the sub-modules are also in the same-version group and (b) the full API surface
of the existing module is preserved through transitive dependencies, e.g. the
sub-modules are added as dependencies of the existing module.

#### Same-version (atomic) groups {#modules-atomic}

Library groups are encouraged to opt-in to a same-version policy whereby all
libraries in the group use the same version and express exact-match dependencies
on libraries within the group. Such groups must increment the version of every
library at the same time and release all libraries at the same time.

Atomic groups are specified in
[`LibraryGroups.kt`](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:buildSrc/src/main/kotlin/androidx/build/LibraryGroups.kt):

```kotlin
// Non-atomic library group
val APPCOMPAT = LibraryGroup("androidx.appcompat", null)
// Atomic library group
val APPSEARCH = LibraryGroup("androidx.appsearch", LibraryVersions.APPSEARCH)
```

Libraries within an atomic group should not specify a version in their
`build.gradle`:

```groovy
androidx {
    name = 'AppSearch'
    publish = Publish.SNAPSHOT_AND_RELEASE
    mavenGroup = LibraryGroups.APPSEARCH
    inceptionYear = '2019'
    description = 'Provides local and centralized app indexing'
}
```

The benefits of using an atomic group are:

-   Easier for developers to understand dependency versioning
-   `@RestrictTo(LIBRARY_GROUP)` APIs are treated as private APIs and not
    tracked for binary compatibility
-   `@RequiresOptIn` APIs defined within the group may be used without any
    restrictions between libraries in the group

Potential drawbacks include:

-   All libraries within the group must be versioned identically at head
-   All libraries within the group must release at the same time

#### Early-stage development {#modules-atomic-alpha}

There is one exception to the same-version policy: newly-added libraries within
an atomic group may be "quarantined" from other libraries to allow for rapid
iteration until they are API-stable.

A quarantined library must stay within the `1.0.0-alphaXX` cycle until it is
ready to conform to the same-version policy. While in quarantime, a library is
treated at though it is in a separate group from its nomical same-version group:

-   Must stay in `1.0.0-alphaXX`, e.g. same-version policy is not enforced
-   May use `project` or pinned version dependencies, e.g. strict-match
    dependencies are not enforced
-   May release on a separate cadence from other libraries within group
-   Must not reference restricted `LIBRARY-GROUP`-scoped APIs

When the library would like to leave quarantine, it must wait for its atomic
group to be within a `beta` cycle and then match the version. It is okay for a
library in this situation to skip versions, e.g. move directly from
`1.0.0-alpha02` to `2.1.3-beta06`.

### Choosing a `minSdkVersion` {#module-minsdkversion}

The recommended minimum SDK version for new Jetpack libraries is currently
**19** (Android 4.4, KitKat). This SDK was chosen to represent 99% of active
devices based on Play Store check-ins (see Android Studio
[distribution metadata](https://dl.google.com/android/studio/metadata/distributions.json)
for current statistics). This maximizes potential users for external developers
while minimizing the amount of overhead necessary to support legacy versions.

However, if no explicit minimum SDK version is specified for a library, the
default is **14** (Android 4.0, Ice Cream Sandwich).

Note that a library **must not** depend on another library with a higher
`minSdkVersion` that its own, so it may be necessary for a new library to match
its dependent libraries' `minSdkVersion`.

Individual modules may choose a higher minimum SDK version for business or
technical reasons. This is common for device-specific modules such as Auto or
Wear.

Individual classes or methods may be annotated with the
[@RequiresApi](https://developer.android.com/reference/android/annotation/RequiresApi.html)
annotation to indicate divergence from the overall module's minimum SDK version.
Note that this pattern is _not recommended_ because it leads to confusion for
external developers and should be considered a last-resort when backporting
behavior is not feasible.

### Kotlin extension `-ktx` libraries {#module-ktx}

New libraries should prefer Kotlin sources with built-in Java compatibility via
`@JvmName` and other affordances of the Kotlin language; however, existing Java
sourced libraries may benefit from extending their API surface with
Kotlin-friendly APIs in a `-ktx` library.

A Kotlin extension library **may only** provide extensions for a single base
library's API surface and its name **must** match the base library exactly. For
example, `work:work-ktx` may only provide extensions for APIs exposed by
`work:work`.

Additionally, an extension library **must** specify an `api`-type dependency on
the base library and **must** be versioned and released identically to the base
library.

Kotlin extension libraries _should not_ expose new functionality; they should
only provide Kotlin-friendly versions of existing Java-facing functionality.

## Platform compatibility API patterns {#platform-compatibility-apis}

NOTE For all library APIs that wrap or provide parity with platform APIs,
_parity with the platform APIs overrides API guidelines_. For example, if the
platform API being wrapped has incorrect `Executor` and `Callback` ordering
according to the API Guidelines, the corresponding library API should have the
exact same (incorrect) ordering.

### Static shims (ex. [ViewCompat](https://developer.android.com/reference/android/support/v4/view/ViewCompat.html)) {#static-shim}

When to use?

*   Platform class exists at module's `minSdkVersion`
*   Compatibility implementation does not need to store additional metadata

Implementation requirements

*   Class name **must** be `<PlatformClass>Compat`
*   Package name **must** be `androidx.<feature>.<platform.package>`
*   Superclass **must** be `Object`
*   Class **must** be non-instantiable, i.e. constructor is private no-op
*   Static fields and static methods **must** match match signatures with
    `PlatformClass`
    *   Static fields that can be inlined, ex. integer constants, **must not**
        be shimmed
*   Public method names **must** match platform method names
*   Public methods **must** be static and take `PlatformClass` as first
    parameter
*   Implementation _may_ delegate to `PlatformClass` methods when available

#### Sample {#static-shim-sample}

The following sample provides static helper methods for the platform class
`android.os.Process`.

```java
/**
 * Helper for accessing features in {@link Process}.
 */
public final class ProcessCompat {
    private ProcessCompat() {
        // This class is non-instantiable.
    }

    /**
     * [Docs should match platform docs.]
     *
     * Compatibility behavior:
     * <ul>
     * <li>SDK 24 and above, this method matches platform behavior.
     * <li>SDK 16 through 23, this method is a best-effort to match platform behavior, but may
     * default to returning {@code true} if an accurate result is not available.
     * <li>SDK 15 and below, this method always returns {@code true} as application UIDs and
     * isolated processes did not exist yet.
     * </ul>
     *
     * @param [match platform docs]
     * @return [match platform docs], or a value based on platform-specific fallback behavior
     */
    public static boolean isApplicationUid(int uid) {
        if (Build.VERSION.SDK_INT >= 24) {
            return Api24Impl.isApplicationUid(uid);
        } else if (Build.VERSION.SDK_INT >= 17) {
            return Api17Impl.isApplicationUid(uid);
        } else if (Build.VERSION.SDK_INT == 16) {
            return Api16Impl.isApplicationUid(uid);
        } else {
            return true;
        }
    }

    @RequiresApi(24)
    static class Api24Impl {
        static boolean isApplicationUid(int uid) {
            // In N, the method was made public on android.os.Process.
            return Process.isApplicationUid(uid);
        }
    }

    @RequiresApi(17)
    static class Api17Impl {
        private static Method sMethod_isAppMethod;
        private static boolean sResolved;

        static boolean isApplicationUid(int uid) {
            // In JELLY_BEAN_MR2, the equivalent isApp(int) hidden method moved to public class
            // android.os.UserHandle.
            try {
                if (!sResolved) {
                    sResolved = true;
                    sMethod_isAppMethod = UserHandle.class.getDeclaredMethod("isApp",int.class);
                }
                if (sMethod_isAppMethod != null) {
                    return (Boolean) sMethod_isAppMethod.invoke(null, uid);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return true;
        }
    }

    ...
}
```

### Wrapper (ex. [AccessibilityNodeInfoCompat](https://developer.android.com/reference/android/support/v4/view/accessibility/AccessibilityNodeInfoCompat.html)) {#wrapper}

When to use?

*   Platform class may not exist at module's `minSdkVersion`
*   Compatibility implementation may need to store additional metadata
*   Needs to integrate with platform APIs as return value or method argument
*   **Note:** Should be avoided when possible, as using wrapper classes makes it
    very difficult to deprecate classes and migrate source code when the
    `minSdkVersion` is raised

#### Sample {#wrapper-sample}

The following sample wraps a hypothetical platform class `ModemInfo` that was
added to the platform SDK in API level 23:

```java
public final class ModemInfoCompat {
  // Only guaranteed to be non-null on SDK_INT >= 23. Note that referencing the
  // class itself directly is fine -- only references to class members need to
  // be pushed into static inner classes.
  private final ModemInfo wrappedObj;

  /**
   * [Copy platform docs for matching constructor.]
   */
  public ModemInfoCompat() {
    if (SDK_INT >= 23) {
      wrappedObj = Api23Impl.create();
    } else {
      wrappedObj = null;
    }
    ...
  }

  @RequiresApi(23)
  private ModemInfoCompat(@NonNull ModemInfo obj) {
    mWrapped = obj;
  }

  /**
   * Provides a backward-compatible wrapper for {@link ModemInfo}.
   * <p>
   * This method is not supported on devices running SDK < 23 since the platform
   * class will not be available.
   *
   * @param info platform class to wrap
   * @return wrapped class, or {@code null} if parameter is {@code null}
   */
  @RequiresApi(23)
  @NonNull
  public static ModemInfoCompat toModemInfoCompat(@NonNull ModemInfo info) {
    return new ModemInfoCompat(obj);
  }

  /**
   * Provides the {@link ModemInfo} represented by this object.
   * <p>
   * This method is not supported on devices running SDK < 23 since the platform
   * class will not be available.
   *
   * @return platform class object
   * @see ModemInfoCompat#toModemInfoCompat(ModemInfo)
   */
  @RequiresApi(23)
  @NonNull
  public ModemInfo toModemInfo() {
    return mWrapped;
  }

  /**
   * [Docs should match platform docs.]
   *
   * Compatibility behavior:
   * <ul>
   * <li>API level 23 and above, this method matches platform behavior.
   * <li>API level 18 through 22, this method ...
   * <li>API level 17 and earlier, this method always returns false.
   * </ul>
   *
   * @return [match platform docs], or platform-specific fallback behavior
   */
  public boolean isLteSupported() {
    if (SDK_INT >= 23) {
      return Api23Impl.isLteSupported(mWrapped);
    } else if (SDK_INT >= 18) {
      // Smart fallback behavior based on earlier APIs.
      ...
    }
    // Default behavior.
    return false;
  }

  // All references to class members -- including the constructor -- must be
  // made on an inner class to avoid soft-verification errors that slow class
  // loading and prevent optimization.
  @RequiresApi(23)
  private static class Api23Impl {
    @NonNull
    static ModemInfo create() {
      return new ModemInfo();
    }

    static boolean isLteSupported(PlatformClass obj) {
      return obj.isLteSupported();
    }
  }
}
```

Note that libraries written in Java should express conversion to and from the
platform class differently than Kotlin classes. For Java classes, conversion
from the platform class to the wrapper should be expressed as a `static` method,
while conversion from the wrapper to the platform class should be a method on
the wrapper object:

```java
@NonNull
public static ModemInfoCompat toModemInfoCompat(@NonNull ModemInfo info);

@NonNull
public ModemInfo toModemInfo();
```

In cases where the primary library is written in Java and has an accompanying
`-ktx` Kotlin extensions library, the following conversion should be provided as
an extension function:

```kotlin
fun ModemInfo.toModemInfoCompat() : ModemInfoCompat
```

Whereas in cases where the primary library is written in Kotlin, the conversion
should be provided as an extension factory:

```kotlin
class ModemInfoCompat {
  fun toModemInfo() : ModemInfo

  companion object {
    @JvmStatic
    @JvmName("toModemInfoCompat")
    fun ModemInfo.toModemInfoCompat() : ModemInfoCompat
  }
}
```

#### API guidelines {#wrapper-api-guidelines}

##### Naming {#wrapper-naming}

*   Class name **must** be `<PlatformClass>Compat`
*   Package name **must** be `androidx.core.<platform.package>`
*   Superclass **must not** be `<PlatformClass>`

##### Construction {#wrapper-construction}

*   Class _may_ have public constructor(s) to provide parity with public
    `PlatformClass` constructors
    *   Constructor used to wrap `PlatformClass` **must not** be public
*   Class **must** implement a static `PlatformClassCompat
    toPlatformClassCompat(PlatformClass)` method to wrap `PlatformClass` on
    supported SDK levels
    *   If class does not exist at module's `minSdkVersion`, method must be
        annotated with `@RequiresApi(<sdk>)` for SDK version where class was
        introduced

#### Implementation {#wrapper-implementation}

*   Class **must** implement a `PlatformClass toPlatformClass()` method to
    unwrap `PlatformClass` on supported SDK levels
    *   If class does not exist at module's `minSdkVersion`, method must be
        annotated with `@RequiresApi(<sdk>)` for SDK version where class was
        introduced
*   Implementation _may_ delegate to `PlatformClass` methods when available (see
    below note for caveats)
*   To avoid runtime class verification issues, all operations that interact
    with the internal structure of `PlatformClass` must be implemented in inner
    classes targeted to the SDK level at which the operation was added.
    *   See the [sample](#wrapper-sample) for an example of interacting with a
        method that was added in SDK level 23.

### Standalone (ex. [ArraySet](https://developer.android.com/reference/android/support/v4/util/ArraySet.html), [Fragment](https://developer.android.com/reference/android/support/v4/app/Fragment.html)) {#standalone}

When to use?

*   Platform class may exist at module's `minSdkVersion`
*   Does not need to integrate with platform APIs
*   Does not need to coexist with platform class, ex. no potential `import`
    collision due to both compatibility and platform classes being referenced
    within the same source file

Implementation requirements

*   Class name **must** be `<PlatformClass>`
*   Package name **must** be `androidx.<platform.package>`
*   Superclass **must not** be `<PlatformClass>`
*   Class **must not** expose `PlatformClass` in public API
    *   In exceptional cases, a _released_ standalone class may add conversion
        between itself and the equivalent platform class; however, _new_ classes
        that support conversion should follow the [Wrapper](#wrapper)
        guidelines. In these cases, use a `toPlatform<PlatformClass>` and
        `static toCompat<PlatformClass>` method naming convention.
*   Implementation _may_ delegate to `PlatformClass` methods when available

### Standalone JAR library (no Android dependencies) {#standalone-jar-library-no-android-dependencies}

When to use

*   General purpose library with minimal interaction with Android types
    *   or when abstraction around types can be used (e.g. Room's SQLite
        wrapper)
*   Lib used in parts of app with minimal Android dependencies
    *   ex. Repository, ViewModel
*   When Android dependency can sit on top of common library
*   Clear separation between android dependent and independent parts of your
    library
*   Clear that future integration with android dependencies can be layered
    separately

**Examples:**

The **Paging Library** pages data from DataSources (such as DB content from Room
or network content from Retrofit) into PagedLists, so they can be presented in a
RecyclerView. Since the included Adapter receives a PagedList, and there are no
other Android dependencies, Paging is split into two parts - a no-android
library (paging-common) with the majority of the paging code, and an android
library (paging-runtime) with just the code to present a PagedList in a
RecyclerView Adapter. This way, tests of Repositories and their components can
be tested in host-side tests.

**Room** loads SQLite data on Android, but provides an abstraction for those
that want to use a different SQL implementation on device. This abstraction, and
the fact that Room generates code dynamically, means that Room interfaces can be
used in host-side tests (though actual DB code should be tested on device, since
DB impls may be significantly different on host).

## Implementing compatibility {#compat}

### Referencing new APIs {#compat-newapi}

Generally, methods on extension library classes should be available to all
devices above the library's `minSdkVersion`.

#### Checking device SDK version {#compat-sdk}

The most common way of delegating to platform or backport implementations is to
compare the device's `Build.VERSION.SDK_INT` field to a known-good SDK version;
for example, the SDK in which a method first appeared or in which a critical bug
was first fixed.

Non-reflective calls to new APIs gated on `SDK_INT` **must** be made from
version-specific static inner classes to avoid verification errors that
negatively affect run-time performance. For more information, see Chromium's
guide to
[Class Verification Failures](https://chromium.googlesource.com/chromium/src/+/HEAD/build/android/docs/class_verification_failures.md).

Methods in implementation-specific classes **must** be paired with the
`@DoNotInline` annotation to prevent them from being inlined.

```java {.good}
public static void saveAttributeDataForStyleable(@NonNull View view, ...) {
  if (Build.VERSION.SDK_INT >= 29) {
    Api29Impl.saveAttributeDataForStyleable(view, ...);
  }
}

@RequiresApi(29)
private static class Api29Impl {
  @DoNotInline
  static void saveAttributeDataForStyleable(@NonNull View view, ...) {
    view.saveAttributeDataForStyleable(...);
  }
}
```

Alternatively, in Kotlin sources:

```kotlin {.good}
@RequiresApi(29)
private object Api29Impl {
  @JvmStatic
  @DoNotInline
  fun saveAttributeDataForStyleable(view: View, ...) { ... }
}
```

When developing against pre-release SDKs where the `SDK_INT` has not been
finalized, SDK checks **must** use `BuildCompat.isAtLeastX()` methods.

```java {.good}
@NonNull
public static List<Window> getAllWindows() {
  if (BuildCompat.isAtLeastR()) {
    return ApiRImpl.getAllWindows();
  }
  return Collections.emptyList();
}
```

#### Device-specific issues {#compat-oem}

Library code may work around device- or manufacturer-specific issues -- issues
not present in AOSP builds of Android -- *only* if a corresponding CTS test
and/or CDD policy is added to the next revision of the Android platform. Doing
so ensures that such issues can be detected and fixed by OEMs.

#### Handling `minSdkVersion` disparity {#compat-minsdk}

Methods that only need to be accessible on newer devices, including
`to<PlatformClass>()` methods, may be annotated with `@RequiresApi(<sdk>)` to
indicate they will fail to link on older SDKs. This annotation is enforced at
build time by Lint.

#### Handling `targetSdkVersion` behavior changes {#compat-targetsdk}

To preserve application functionality, device behavior at a given API level may
change based on an application's `targetSdkVersion`. For example, if an app with
`targetSdkVersion` set to API level 22 runs on a device with API level 29, all
required permissions will be granted at installation time and the run-time
permissions framework will emulate earlier device behavior.

Libraries do not have control over the app's `targetSdkVersion` and -- in rare
cases -- may need to handle variations in platform behavior. Refer to the
following pages for version-specific behavior changes:

*   API level 29:
    [Android Q behavior changes: apps targeting Q](https://developer.android.com/preview/behavior-changes-q)
*   API level 28:
    [Behavior changes: apps targeting API level 28+](https://developer.android.com/about/versions/pie/android-9.0-changes-28)
*   API level 26:
    [Changes for apps targeting Android 8.0](https://developer.android.com/about/versions/oreo/android-8.0-changes#o-apps)
*   API level 24:
    [Changes for apps targeting Android 7.0](https://developer.android.com/about/versions/nougat/android-7.0-changes#n-apps)
*   API level 21:
    [Android 5.0 Behavior Changes](https://developer.android.com/about/versions/android-5.0-changes)
*   API level 19:
    [Android 4.4 APIs](https://developer.android.com/about/versions/android-4.4)

#### Working around Lint issues {#compat-lint}

In rare cases, Lint may fail to interpret API usages and yield a `NewApi` error
and require the use of `@TargetApi` or `@SuppressLint('NewApi')` annotations.
Both of these annotations are strongly discouraged and may only be used
temporarily. They **must never** be used in a stable release. Any usage of these
annotation **must** be associated with an active bug, and the usage must be
removed when the bug is resolved.

### Delegating to API-specific implementations {#delegating-to-api-specific-implementations}

#### SDK-dependent reflection

Starting in API level 28, the platform restricts which
[non-SDK interfaces](https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces)
can be accessed via reflection by apps and libraries. As a general rule, you
will **not** be able to use reflection to access hidden APIs on devices with
`SDK_INT` greater than `Build.VERSION_CODES.P` (28).

On earlier devices, reflection on hidden platform APIs is allowed **only** when
an alternative public platform API exists in a later revision of the Android
SDK. For example, the following implementation is allowed:

```java
public AccessibilityDelegate getAccessibilityDelegate(View v) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Retrieve the delegate using a public API.
        return v.getAccessibilityDelegate();
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
        // Retrieve the delegate by reflecting on a private field. If the
        // field does not exist or cannot be accessed, this will no-op.
        if (sAccessibilityDelegateField == null) {
            try {
                sAccessibilityDelegateField = View.class
                        .getDeclaredField("mAccessibilityDelegate");
                sAccessibilityDelegateField.setAccessible(true);
            } catch (Throwable t) {
                sAccessibilityDelegateCheckFailed = true;
                return null;
            }
        }
        try {
            Object o = sAccessibilityDelegateField.get(v);
            if (o instanceof View.AccessibilityDelegate) {
                return (View.AccessibilityDelegate) o;
            }
            return null;
        } catch (Throwable t) {
            sAccessibilityDelegateCheckFailed = true;
            return null;
        }
    } else {
        // There is no way to retrieve the delegate, even via reflection.
        return null;
    }
```

Calls to public APIs added in pre-release revisions *must* be gated using
`BuildCompat`:

```java
if (BuildCompat.isAtLeastQ()) {
   // call new API added in Q
} else if (Build.SDK_INT.VERSION >= Build.VERSION_CODES.SOME_RELEASE) {
   // make a best-effort using APIs that we expect to be available
} else {
   // no-op or best-effort given no information
}
```

### Inter-process communication {#inter-process-communication}

Protocols and data structures used for IPC must support interoperability between
different versions of libraries and should be treated similarly to public API.

#### Data structures

**Do not** use Parcelable for any class that may be used for IPC or otherwise
exposed as public API. The data format used by Parcelable does not provide any
compatibility guarantees and will result in crashes if fields are added or
removed between library versions.

**Do not** design your own serialization mechanism or wire format for disk
storage or inter-process communication. Preserving and verifying compatibility
is difficult and error-prone.

Developers **should** use protocol buffers for most cases. See
[Protobuf](#dependencies-protobuf) for more information on using protocol
buffers in your library.

Developers **may** use `Bundle` in simple cases that require sending `Binder`s
or `FileDescriptor`s across IPC. If you expose a `Bundle` to callers that can
cross processes, you should
[prevent apps from adding their own custom parcelables](https://android.googlesource.com/platform/frameworks/base/+/6cddbe14e1ff67dc4691a013fe38a2eb0893fe03)
as top-level entries; if *any* entry in a `Bundle` can't be loaded, even if it's
not actually accessed, the receiving process is likely to crash.

#### Communication protocols

Any communication prototcol, handshake, etc. must maintain compatibility
consistent with SemVer guidelines. Consider how your protocol will handle
addition and removal of operations or constants, compatibility-breaking changes,
and other modifications without crashing either the host or client process.

## Deprecation and removal

While SemVer's binary compatibility guarantees restrict the types of changes
that may be made within a library revision and make it difficult to remove an
API, there are many other ways to influence how developers interact with your
library.

### Deprecation (`@deprecated`)

Deprecation lets a developer know that they should stop using an API or class.
All deprecations must be marked with a `@Deprecated` Java annotation as well as
a `@deprecated <migration-docs>` docs annotation explaining how the developer
should migrate away from the API.

Deprecation is an non-breaking API change that must occur in a **major** or
**minor** release.

APIs that are added during a pre-release cycle and marked as `@Deprecated`
within the same cycle, e.g. added in `alpha01` and deprecated in `alpha06`,
[must be removed](versioning.md#beta-checklist) before moving to `beta01`.

### Soft removal (@removed)

Soft removal preserves binary compatibility while preventing source code from
compiling against an API. It is a *source-breaking change* and not recommended.

Soft removals **must** do the following:

*   Mark the API as deprecated for at least one stable release prior to removal.
*   Mark the API with a `@RestrictTo(LIBRARY)` Java annotation as well as a
    `@removed <reason>` docs annotation explaining why the API was removed.
*   Maintain binary compatibility, as the API may still be called by existing
    dependent libraries.
*   Maintain behavioral compatibility and existing tests.

This is a disruptive change and should be avoided when possible.

Soft removal is a source-breaking API change that must occur in a **major** or
**minor** release.

### Hard removal

Hard removal entails removing the entire implementation of an API that was
exposed in a public release. Prior to removal, an API must be marked as
`@deprecated` for a full **minor** version (`alpha`->`beta`->`rc`->stable),
prior to being hard removed.

This is a disruptive change and should be avoided when possible.

Hard removal is a binary-breaking API change that must occur in a **major**
release.

### For entire artifacts

We do not typically deprecate or remove entire artifacts; however, it may be
useful in cases where we want to halt development and focus elsewhere or
strongly discourage developers from using a library.

Halting development, either because of staffing or prioritization issues, leaves
the door open for future bug fixes or continued development. This quite simply
means we stop releasing updates but retain the source in our tree.

Deprecating an artifact provides developers with a migration path and strongly
encourages them -- through Lint warnings -- to migrate elsewhere. This is
accomplished by adding a `@Deprecated` and `@deprecated` (with migration
comment) annotation pair to *every* class and interface in the artifact.

Entire packages (including Kotlin) can be deprecated by using a
`package-info.java` file and applying the `@Deprecated` annotation there.

The fully-deprecated artifact will be released as a deprecation release -- it
will ship normally with accompanying release notes indicating the reason for
deprecation and migration strategy, and it will be the last version of the
artifact that ships. It will ship as a new minor stable release. For example, if
`1.0.0` was the last stable release, then the deprecation release will be
`1.1.0`. This is so Android Studio users will get a suggestion to update to a
new stable version, which will contain the `@deprecated` annotations.

After an artifact has been released as fully-deprecated, it can be removed from
the source tree.

## Resources {#resources}

Generally, follow the official Android guidelines for
[app resources](https://developer.android.com/guide/topics/resources/providing-resources).
Special guidelines for library resources are noted below.

### Defining new resources

Libraries may define new value and attribute resources using the standard
application directory structure used by Android Gradle Plugin:

```
src/main/res/
  values/
    attrs.xml   Theme attributes and styleables
    dimens.xml  Dimensional values
    public.xml  Public resource definitions
    ...
```

However, some libraries may still be using non-standard, legacy directory
structures such as `res-public` for their public resource declarations or a
top-level `res` directory and accompanying custom source set in `build.gradle`.
These libraries will eventually be migrated to follow standard guidelines.

#### Naming conventions

Libraries follow the Android platform's resource naming conventions, which use
`camelCase` for attributes and `underline_delimited` for values. For example,
`R.attr.fontProviderPackage` and `R.dimen.material_blue_grey_900`.

#### Attribute formats

At build time, attribute definitions are pooled globally across all libraries
used in an application, which means attribute `format`s *must* be identical for
a given `name` to avoid a conflict.

Within Jetpack, new attribute names *must* be globally unique. Libraries *may*
reference existing public attributes from their dependencies. See below for more
information on public attributes.

When adding a new attribute, the format should be defined *once* in an `<attr
/>` element in the definitions block at the top of `src/main/res/attrs.xml`.
Subsequent references in `<declare-styleable>` elements *must* not include a
`format`:

`src/main/res/attrs.xml`

```xml
<resources>
  <attr name="fontProviderPackage" format="string" />

  <declare-styleable name="FontFamily">
      <attr name="fontProviderPackage" />
  </declare-styleable>
</resources>
```

### Public resources

Library resources are private by default, which means developers are discouraged
from referencing any defined attributes or values from XML or code; however,
library resources may be declared public to make them available to developers.

Public library resources are considered API surface and are thus subject to the
same API consistency and documentation requirements as Java APIs.

Libraries will typically only expose theme attributes, ex. `<attr />` elements,
as public API so that developers can set and retrieve the values stored in
styles and themes. Exposing values -- such as `<dimen />` and `<string />` -- or
images -- such as drawable XML and PNGs -- locks the current state of those
elements as public API that cannot be changed without a major version bump. That
means changing a publicly-visible icon would be considered a breaking change.

#### Documentation

All public resource definitions should be documented, including top-level
definitions and re-uses inside `<styleable>` elements:

`src/main/res/attrs.xml`

```xml
<resources>
  <!-- String specifying the application package for a Font Provider. -->
  <attr name="fontProviderPackage" format="string" />

  <!-- Attributes that are read when parsing a <fontfamily> tag. -->
  <declare-styleable name="FontFamily">
      <!-- The package for the Font Provider to be used for the request. This is
           used to verify the identity of the provider. -->
      <attr name="fontProviderPackage" />
  </declare-styleable>
</resources>
```

`src/main/res/colors.xml`

```xml
<resources>
  <!-- Color for Material Blue-Grey 900. -->
  <color name="material_blue_grey_900">#ff263238</color>
</resources>
```

#### Public declaration

Resources are declared public by providing a separate `<public />` element with
a matching type:

`src/main/res/public.xml`

```xml
<resources>
  <public name="fontProviderPackage" type="attr" />
  <public name="material_blue_grey_900" type="color" />
</resources>
```

#### More information

See also the official Android Gradle Plugin documentation for
[Private Resources](https://developer.android.com/studio/projects/android-library#PrivateResources).

### Manifest entries (`AndroidManifest.xml`) {#resources-manifest}

#### Metadata tags (`<meta-data>`) {#resources-manifest-metadata}

Developers **must not** add `<application>`-level `<meta-data>` tags to library
manifests or advise developers to add such tags to their application manifests.
Doing so may _inadvertently cause denial-of-service attacks against other apps_.

Assume a library adds a single item of meta-data at the application level. When
an app uses the library, that meta-data will be merged into the resulting app's
application entry via manifest merger.

If another app attempts to obtain a list of all activities associated with the
primary app, that list will contain multiple copies of the `ApplicationInfo`,
each of which in turn contains a copy of the library's meta-data. As a result,
one `<metadata>` tag may become hundreds of KB on the binder call to obtain the
list -- resulting in apps hitting transaction too large exceptions and crashing.

```xml {.bad}
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.librarypackage">
  <application>
    <meta-data
        android:name="keyName"
        android:value="@string/value" />
  </application>
</manifest>
```

Instead, developers may consider adding `<metadata>` nested inside of
placeholder `<service>` tags.

```xml {.good}
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="androidx.librarypackage">
  <application>
    <service
        android:name="androidx.librarypackage.MetadataHolderService"
        android:enabled="false"
        android:exported="false">
      <meta-data
          android:name="androidx.librarypackage.MetadataHolderService.KEY_NAME"
          android:resource="@string/value" />
    </service>
  </application>
```

```java {.good}
package androidx.libraryname.featurename;

/**
 * A placeholder service to avoid adding application-level metadata. The service
 * is only used to expose metadata defined in the library's manifest. It is
 * never invoked.
 */
public final class MetadataHolderService {
  private MetadataHolderService() {}

  @Override
  public IBinder onBind(Intent intent) {
    throw new UnsupportedOperationException();
  }
}
```

## Dependencies {#dependencies}

Artifacts may depend on other artifacts within AndroidX as well as sanctioned
third-party libraries.

### Versioned artifacts {#dependencies-versioned}

One of the most difficult aspects of independently-versioned releases is
maintaining compatibility with public artifacts. In a mono repo such as Google's
repository or Android Git at master revision, it's easy for an artifact to
accidentally gain a dependency on a feature that may not be released on the same
schedule.

-   Project `project(":core:core")` uses the tip-of-tree sources for the
    `androidx.core:core` library and requires that they be loaded in the
    workspace.
-   Playground `projectOrArtifact(":core:core")` is used for
    [Playground](playground.md) projects and will use tip-of-tree sources, if
    present in the workspace, or `SNAPSHOT` prebuilt artifacts from
    [androidx.dev](http://androidx.dev) otherwise.
-   Explicit `"androidx.core:core:1.4.0"` uses the prebuilt AAR and requires
    that it be checked in to the `prebuilts/androidx/internal` local Maven
    repository.

Libraries should prefer explicit dependencies with the lowest possible versions
that include the APIs or behaviors required by the library, using project or
Playground specs only in cases where tip-of-tree APIs or behaviors are required.

#### Pre-release dependencies {#dependencies-pre-release}

Pre-release suffixes **must** propagate up the dependency tree. For example, if
your artifact has API-type dependencies on pre-release artifacts, ex.
`1.1.0-alpha01`, then your artifact must also carry the `alpha` suffix. If you
only have implementation-type dependencies, your artifact may carry either the
`alpha` or `beta` suffix.

Note: This does not apply to test dependencies: suffixes of test dependencies do
_not_ carry over to your artifact.

#### Pinned versions {#dependencies-prebuilt}

To avoid issues with dependency versioning, consider pinning your artifact's
dependencies to the oldest version (available via local `maven_repo` or Google
Maven) that satisfies the artifact's API requirements. This will ensure that the
artifact's release schedule is not accidentally tied to that of another artifact
and will allow developers to use older libraries if desired.

```
dependencies {
   api("androidx.collection:collection:1.0.0")
   ...
}
```

Artifacts should be built and tested against both pinned and tip-of-tree
versions of their dependencies to ensure behavioral compatibility.

#### Tip-of-tree versions {#dependencies-project}

Below is an example of a non-pinned dependency. It ties the artifact's release
schedule to that of the dependency artifact, because the dependency will need to
be released at the same time.

```
dependencies {
   api(project(":collection"))
   ...
}
```

### Non-public APIs {#dependencies-non-public-apis}

Artifacts may depend on non-public (e.g. `@hide`) APIs exposed within their own
artifact or another artifact in the same `groupId`; however, cross-artifact
usages are subject to binary compatibility guarantees and
`@RestrictTo(Scope.LIBRARY_GROUP)` APIs must be tracked like public APIs.

```
Dependency versioning policies are enforced at build time in the createArchive task. This task will ensure that pre-release version suffixes are propagated appropriately.

Cross-artifact API usage policies are enforced by the checkApi and checkApiRelease tasks (see Life of a release).
```

### Third-party libraries {#dependencies-3p}

Artifacts may depend on libraries developed outside of AndroidX; however, they
must conform to the following guidelines:

*   Prebuilt **must** be checked into Android Git with both Maven and Make
    artifacts
    *   `prebuilts/maven_repo` is recommended if this dependency is only
        intended for use with AndroidX artifacts, otherwise please use
        `external`
*   Prebuilt directory **must** contains an `OWNERS` file identifying one or
    more individual owners (e.g. NOT a group alias)
*   Library **must** be approved by legal

Please see Jetpack's [open-source policy page](open_source.md) for more details
on using third-party libraries.

### System health {#dependencies-health}

Libraries should consider the system health implications of their dependencies,
including:

-   Large dependencies where only a small portion is needed (e.g. APK bloat)
-   Dependencies that slow down build times through annotation processing or
    compiler overhead

#### Kotlin {#dependencies-kotlin}

Kotlin is _strongly recommended_ for new libraries; however, it's important to
consider its size impact on clients. Currently, the Kotlin stdlib adds a minimum
of 40kB post-optimization. It may not make sense to use Kotlin for a library
that targets Java-only clients or space-constrained (ex. Android Go) clients.

Existing Java-based libraries are _strongly discouraged_ from using Kotlin,
primarily because our documentation system does not currently provide a
Java-facing version of Kotlin API reference docs. Java-based libraries _may_
migrate to Kotlin, but they must consider the docs usability and size impacts on
existing Java-only and space-constrained clients.

#### Kotlin coroutines {#dependencies-coroutines}

Kotlin's coroutine library adds around 100kB post-shrinking. New libraries that
are written in Kotlin should prefer coroutines over `ListenableFuture`, but
existing libraries must consider the size impact on their clients. See
[Asynchronous work with return values](#async-return) for more details on using
Kotlin coroutines in Jetpack libraries.

#### Guava {#dependencies-guava}

The full Guava library is very large and *must not* be used. Libraries that
would like to depend on Guava's `ListenableFuture` may instead depend on the
standalone `com.google.guava:listenablefuture` artifact. See
[Asynchronous work with return values](#async-return) for more details on using
`ListenableFuture` in Jetpack libraries.

#### Java 8 {#dependencies-java8}

Libraries that take a dependency on a library targeting Java 8 must _also_
target Java 8, which will incur a ~5% build performance (as of 8/2019) hit for
clients. New libraries targeting Java 8 may use Java 8 dependencies.

The default language level for `androidx` libraries is Java 8, and we encourage
libraries to stay on Java 8. However, if you have a business need to target Java
7, you can specify Java 7 in your `build.gradle` as follows:

```groovy
android {
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_7
        targetCompatibility = JavaVersion.VERSION_1_7
    }
}
```

#### Protobuf {#dependencies-protobuf}

[Protocol buffers](https://developers.google.com/protocol-buffers) provide a
language- and platform-neutral mechanism for serializing structured data. The
implementation enables developers to maintain protocol compatibility across
library versions, meaning that two clients can communicate regardless of the
library versions included in their APKs.

The Protobuf library itself, however, does not guarantee ABI compatibility
across minor versions and a specific version **must** be bundled with a library
to avoid conflict with other dependencies used by the developer.

Additionally, the Java API surface generated by the Protobuf compiler is not
guaranteed to be stable and **must not** be exposed to developers. Library
owners should wrap the generated API surface with well-documented public APIs
that follow an appropriate language-specific paradigm for constructing data
classes, e.g. the Java `Builder` pattern.

### Open-source compatibility {#dependencies-aosp}

[Jetpack Principles](principles.md) require that libraries consider the
open-source compatibility implications of their dependencies, including:

-   Closed-source or proprietary libraries or services that may not be available
    on AOSP devices
-   Dependencies that may prevent developers from effectively isolating their
    tests from third-party libraries or services

Primary artifacts, e.g. `workmanager`, **must not** depend on closed-source
components including libraries and hard-coded references to packages,
permissions, or IPC mechanisms that may only be fulfulled by closed-source
components.

Optional artifacts, e.g. `workmanager-gcm`, _may_ depend on closed-source
components or configure a primary artifact to be backed by a closed-source
component via service discovery or initialization.

Some examples of safely depending on closed-source components include:

-   WorkManager's GCM Network Manager integration, which uses manifest metadata
    for service discovery and provides an optional artifact exposing the
    service.
-   Ads Identifier's Play Services integration, which provides a default backend
    and uses `Intent` handling as a service discovery mechanism for Play
    Services.
-   Downloadable Fonts integration with Play Services, which plugs in via a
    `ContentProvider` as a service discovery mechanism with developer-specified
    signature verification for additional security.

Note that in all cases, the developer is not _required_ to use GCM or Play
Services and may instead use another compatible service implementing the same
publicly-defined protocols.

## More API guidelines {#more-api-guidelines}

### Annotations {#annotation}

#### Annotation processors {#annotation-processor}

Annotation processors should opt-in to incremental annotation processing to
avoid triggering a full recompilation on every client source code change. See
Gradle's
[Incremental annotation processing](https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing)
documentation for information on how to opt-in.

### Experimental `@RequiresOptIn` APIs {#experimental-api}

Jetpack libraries may choose to annotate API surfaces as unstable using either
Kotlin's
[`@RequiresOptIn` annotation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-requires-opt-in/)
for APIs written in Kotlin or Jetpack's
[`@RequiresOptIn` annotation](https://developer.android.com/reference/kotlin/androidx/annotation/RequiresOptIn)
for APIs written in Java.

In both cases, API surfaces marked as experimental are considered alpha and will
be excluded from API compatibility guarantees. Due to the lack of compatibility
guarantees, stable libraries *must never* call experimental APIs exposed by
other libraries outside of their
[same-version group](#same-version-atomic-groups) and *may not* use the `@OptIn`
annotation except in the following cases:

*   A library within a same-version group *may* call an experimental API exposed
    by another library **within its same-version group**. In this case, API
    compatibility guarantees are covered under the same-version group policies
    and the library *may* use the `@OptIn` annotation to prevent propagation of
    the experimental property. **Library owners must exercise care to ensure
    that post-alpha APIs backed by experimental APIs actually meet the release
    criteria for post-alpha APIs.**
*   An `alpha` library may use experimental APIs from outside its same-version
    group. These usages must be removed when the library moves to `beta`.

NOTE JetBrains's own usage of `@RequiresOptIn` in Kotlin language libraries
varies and may indicate binary instability, functional instability, or simply
that an API is really difficult to use. Jetpack libraries should treat instances
of `@RequiresOptIn` in JetBrains libraries as indicating **binary instability**
and avoid using them outside of `alpha`; however, teams are welcome to obtain
written assurance from JetBrains regarding binary stability of specific APIs.
`@RequiresOptIn` APIs that are guaranteed to remain binary compatible _may_ be
used in `beta`, but usages must be removed when the library moves to `rc`.

#### How to mark an API surface as experimental

All libraries using `@RequiresOptIn` annotations *must* depend on the
`androidx.annotation:annotation-experimental` artifact regardless of whether
they are using the `androidx` or Kotlin annotation. This artifact provides Lint
enforcement of experimental usage restrictions for Kotlin callers as well as
Java (which the Kotlin annotation doesn't handle on its own, since it's a Kotlin
compiler feature). Libraries *may* include the dependency as `api`-type to make
`@OptIn` available to Java clients; however, this will also unnecessarily expose
the `@RequiresOptIn` annotation.

```java
dependencies {
    implementation(project(":annotation:annotation-experimental"))
}
```

See Kotlin's
[opt-in requirements documentation](https://kotlinlang.org/docs/reference/opt-in-requirements.html)
for general usage information. If you are writing experimental Java APIs, you
will use the Jetpack
[`@RequiresOptIn` annotation](https://developer.android.com/reference/kotlin/androidx/annotation/RequiresOptIn)
rather than the Kotlin compiler's annotation.

#### How to transition an API out of experimental

When an API surface is ready to transition out of experimental, the annotation
may only be removed during an alpha pre-release stage since removing the
experimental marker from an API is equivalent to adding the API to the current
API surface.

When transitioning an entire feature surface out of experimental, you *should*
remove the associated annotations.

When making any change to the experimental API surface, you *must* run
`./gradlew updateApi` prior to uploading your change.

### Restricted APIs {#restricted-api}

Jetpack's library tooling supports hiding Java-visible (ex. `public` and
`protected`) APIs from developers using a combination of the `@RestrictTo`
source annotation, and the `@hide` docs annotation (`@suppress` in Kotlin).
These annotations **must** be paired together when used, and are validated as
part of presubmit checks for Java code (Kotlin not yet supported by Checkstyle).

The effects of hiding an API are as follows:

*   The API will not appear in documentation
*   Android Studio will warn the developer not to use the API

Hiding an API does *not* provide strong guarantees about usage:

*   There are no runtime restrictions on calling hidden APIs
*   Android Studio will not warn if hidden APIs are called using reflection
*   Hidden APIs will still show in Android Studio's auto-complete

#### When to use `@hide` {#restricted-api-usage}

In other cases, avoid using `@hide` / `@suppress`. These annotations indicates
that developers should not call an API that is _technically_ public from a Java
visibility perspective. Hiding APIs is often a sign of a poorly-abstracted API
surface, and priority should be given to creating public, maintainable APIs and
using Java visibility modifiers.

*Do not* use `@hide`/`@suppress` to bypass API tracking and review for
production APIs; instead, rely on API+1 and API Council review to ensure APIs
are reviewed on a timely basis.

*Do not* use `@hide`/`@suppress` for implementation detail APIs that are used
between libraries and could reasonably be made public.

*Do* use `@hide`/`@suppress` paired with `@RestrictTo(LIBRARY)` for
implementation detail APIs used within a single library (but prefer Java
language `private` or `default` visibility).

#### `RestrictTo.Scope` and inter- versus intra-library API surfaces {#private-api-types}

To maintain binary compatibility between different versions of libraries,
restricted API surfaces that are used between libraries (inter-library APIs)
must follow the same Semantic Versioning rules as public APIs. Inter-library
APIs should be annotated with the `@RestrictTo(LIBRARY_GROUP)` source
annotation.

Restricted API surfaces used within a single library (intra-library APIs), on
the other hand, may be added or removed without any compatibility
considerations. It is safe to assume that developers _never_ call these APIs,
even though it is technically feasible. Intra-library APIs should be annotated
with the `@RestrictTo(LIBRARY)` source annotation.

The following table shows the visibility of a hypothetical API within Maven
coordinate `androidx.concurrent:concurrent` when annotated with a variety of
scopes:

<table>
    <tr>
        <td><code>RestrictTo.Scope</code></td>
        <td>Visibility by Maven coordinate</td>
        <td>Versioning</td>
    </tr>
    <tr>
        <td><code>LIBRARY</code></td>
        <td><code>androidx.concurrent:concurrent</code></td>
        <td>No compatibility gurantees (same as private)</td>
    </tr>
    <tr>
        <td><code>LIBRARY_GROUP</code></td>
        <td><code>androidx.concurrent:*</code></td>
        <td>Semantic versioning (including deprecation)</td>
    </tr>
    <tr>
        <td><code>LIBRARY_GROUP_PREFIX</code></td>
        <td><code>androidx.*:*</code></td>
        <td>Semantic versioning (including deprecation)</td>
    </tr>
</table>

#### `@IntDef` `@StringDef` and `@LongDef` and visibility

All `@IntDef`, `@StringDef`, and `@LongDef` will be stripped from resulting
artifacts to avoid issues where compiler inlining constants removes information
as to which `@IntDef` defined the value of `1`. The annotations are extracted
and packaged separately to be read by Android Studio and lint which enforces the
types in application code.

*   Libraries _must_ `@hide` all `@IntDef`, `@StringDef`, and `@LongDef`
    declarations.
*   Libraries _must_ expose constants used to define the `@IntDef` etc at the
    same Java visibility as the hidden `@IntDef`
*   Libraries _should_ also use @RestrictTo to create a warning when the type
    used incorrectly.

Here is a complete example of an `@IntDef`

```java
// constants match Java visibility of ExifStreamType
// code outside this module interacting with ExifStreamType uses these constants
public static final int STREAM_TYPE_FULL_IMAGE_DATA = 1;
public static final int STREAM_TYPE_EXIF_DATA_ONLY = 2;

/** @hide */
@RestrictTo(RestrictTo.Scope.LIBRARY) // Don't export ExifStreamType outside module
@Retention(RetentionPolicy.SOURCE)
@IntDef({
  STREAM_TYPE_FULL_IMAGE_DATA,
  STREAM_TYPE_EXIF_DATA_ONLY,
})
public @interface ExifStreamType {}
```

Java visibilty should be set as appropriate for the code in question (`private`,
`package` or `public`) and is unrelated to hiding.

For more, read the section in
[Android API Council Guidelines](https://android.googlesource.com/platform/developers/docs/+/refs/heads/master/api-guidelines/index.md#no-public-typedefs)

### Constructors {#constructors}

#### View constructors {#view-constructors}

The four-arg View constructor -- `View(Context, AttributeSet, int, int)` -- was
added in SDK 21 and allows a developer to pass in an explicit default style
resource rather than relying on a theme attribute to resolve the default style
resource. Because this API was added in SDK 21, care must be taken to ensure
that it is not called through any < SDK 21 code path.

Views _may_ implement a four-arg constructor in one of the following ways:

1.  Do not implement.
1.  Implement and annotate with `@RequiresApi(21)`. This means the three-arg
    constructor **must not** call into the four-arg constructor.

### Asynchronous work {#async}

#### With return values {#async-return}

Traditionally, asynchronous work on Android that results in an output value
would use a callback; however, better alternatives exist for libraries.

Kotlin libraries should prefer
[coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) and
`suspend` functions, but please refer to the guidance on
[allowable dependencies](#dependencies-coroutines) before adding a new
dependency on coroutines.

Java libraries should prefer `ListenableFuture` and the
[`CallbackToFutureAdapter`](https://developer.android.com/reference/androidx/concurrent/futures/CallbackToFutureAdapter)
implementation provided by the `androidx.concurrent:concurrent-futures` library.

Libraries **must not** use `java.util.concurrent.CompletableFuture`, as it has a
large API surface that permits arbitrary mutation of the future's value and has
error-prone defaults.

See the [Dependencies](#dependencies) section for more information on using
Kotlin coroutines and Guava in your library.

#### Cancellation

Libraries that expose APIs for performing asynchronous work should support
cancellation. There are _very few_ cases where it is not feasible to support
cancellation.

Libraries that use `ListenableFuture` must be careful to follow the exact
specification of
[`Future.cancel(boolean mayInterruptIfRunning)`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Future.html?is-external=true#cancel-boolean-)
behavior.

```java {.bad}
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    // Does not support cancellation.
    return false;
}
```

```java {.bad}
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    // Aggressively does not support cancellation.
    throw new UnsupportedOperationException();
}
```

```java {.good}
@Override
public boolean cancel(boolean mayInterruptIfRunning) {
    // Pseudocode that ignores threading but follows the spec.
    if (mCompleted
            || mCancelled
            || mRunning && !mayInterruptIfRunning) {
        return false;
    }
    mCancelled = true;
    return true;
}
```

#### Avoid `synchronized` methods

Whenever multiple threads are interacting with shared (mutable) references those
reads and writes must be synchronized in some way. However synchronized blocks
make your code thread-safe at the expense of concurrent execution. Any time
execution enters a synchronized block or method any other thread trying to enter
a synchronized block on the same object has to wait; even if in practice the
operations are unrelated (e.g. they interact with different fields). This can
dramatically reduce the benefit of trying to write multi-threaded code in the
first place.

Locking with synchronized is a heavyweight form of ensuring ordering between
threads, and there are a number of common APIs and patterns that you can use
that are more lightweight, depending on your use case:

*   Compute a value once and make it available to all threads
*   Update Set and Map data structures across threads
*   Allow a group of threads to process a stream of data concurrently
*   Provide instances of a non-thread-safe type to multiple threads
*   Update a value from multiple threads atomically
*   Maintain granular control of your concurrency invariants

### Kotlin {#kotlin}

#### Nullability from Java (new APIs)

All new Java APIs should be annotated either `@Nullable` or `@NonNull` for all
reference parameters and reference return types.

```java
    @Nullable
    public Object someNewApi(@NonNull Thing arg1, @Nullable List<WhatsIt> arg2) {
        if(/** something **/) {
            return someObject;
        } else {
            return null;
    }
```

#### Nullability from Java (existing APIs)

Adding `@Nullable` or `@NonNull` annotations to existing APIs to document their
existing nullability is OK. This is a source breaking change for Kotlin
consumers, and you should ensure that it's noted in the release notes and try to
minimize the frequency of these updates in releases.

Changing the nullability of an API is a breaking change.

#### Extending APIs that expose types without nullability annotations

[Platform types](https://kotlinlang.org/docs/java-interop.html#null-safety-and-platform-types)
are exposed by Java types that do not have a `@Nullable` or `@NonNull`
annotation. In Kotlin they are indicated with the `!` suffix.

When interacting with an Android platform API that exposes APIs with unknown
nullability follow these rules:

1.  If wrapping the type in a new API, define and handle `@Nullable` or
    `@NonNull` in the library. Treat types with unknown nullability passed into
    or return from Android as `@Nullable` in the library.
2.  If extending an existing API (e.g. `@Override`), pass through the existing
    types with unknown nullability and annotate each with
    `@SuppressLint("UnknownNullness")`

In Kotlin, a type with unknown nullability is exposed as a "platform type"
(indicated with a `!` suffix) which has unknown nullability in the type checker,
and may bypass type checking leading to runtime errors. When possible, do not
directly expose types with unknown nullability in new public APIs.

#### Extending `@RecentlyNonNull` and `@RecentlyNullable` APIs

Platform APIs are annotated in the platform SDK artifacts with fake annotations
`@RecentlyNonNull` and `@RecentlyNullable` to avoid breaking builds when we
annotated platform APIs with nullability. These annotations cause warnings
instead of build failures. The `RecentlyNonNull` and `RecentlyNullable`
annotations are added by Metalava and do not appear in platform code.

When extending an API that is annotated `@RecentlyNonNull`, you should annotate
the override with `@NonNull`, and the same for `@RecentlyNullable` and
`@Nullable`.

For example `SpannableStringBuilder.append` is annotated `RecentlyNonNull` and
an override should look like:

```java
    @NonNull
    @Override
    public SpannableStringBuilder append(@SuppressLint("UnknownNullness") CharSequence text) {
        super.append(text);
        return this;
    }
```

#### Data classes {#kotlin-data}

Kotlin `data` classes provide a convenient way to define simple container
objects, where Kotlin will generate `equals()` and `hashCode()` for you.
However, they are not designed to preserve API/binary compatibility when members
are added. This is due to other methods which are generated for you -
[destructuring declarations](https://kotlinlang.org/docs/reference/multi-declarations.html),
and [copying](https://kotlinlang.org/docs/reference/data-classes.html#copying).

Example data class as tracked by metalava:

<pre>
  public final class TargetAnimation {
    ctor public TargetAnimation(float target, androidx.animation.AnimationBuilder animation);
    <b>method public float component1();</b>
    <b>method public androidx.animation.AnimationBuilder component2();</b>
    <b>method public androidx.animation.TargetAnimation copy(float target, androidx.animation.AnimationBuilder animation);</b>
    method public androidx.animation.AnimationBuilder getAnimation();
    method public float getTarget();
  }
</pre>

Because members are exposed as numbered components for destructuring, you can
only safely add members at the end of the member list. As `copy` is generated
with every member name in order as well, you'll also have to manually
re-implement any old `copy` variants as items are added. If these constraints
are acceptable, data classes may still be useful to you.

As a result, Kotlin `data` classes are _strongly discouraged_ in library APIs.
Instead, follow best-practices for Java data classes including implementing
`equals`, `hashCode`, and `toString`.

See Jake Wharton's article on
[Public API challenges in Kotlin](https://jakewharton.com/public-api-challenges-in-kotlin/)
for more details.

#### Exhaustive `when` and `sealed class`/`enum class` {#exhaustive-when}

A key feature of Kotlin's `sealed class` and `enum class` declarations is that
they permit the use of **exhaustive `when` expressions.** For example:

```kotlin
enum class CommandResult { Permitted, DeniedByUser }

val message = when (commandResult) {
    Permitted -> "the operation was permitted"
    DeniedByUser -> "the user said no"
}

println(message)
```

This highlights challenges for library API design and compatibility. Consider
the following addition to the `CommandResult` possibilities:

```kotlin {.bad}
enum class CommandResult {
    Permitted,
    DeniedByUser,
    DeniedByAdmin // New in androidx.mylibrary:1.1.0!
}
```

This change is both **source and binary breaking.**

It is **source breaking** because the author of the `when` block above will see
a compiler error about not handling the new result value.

It is **binary breaking** because if the `when` block above was compiled as part
of a library `com.example.library:1.0.0` that transitively depends on
`androidx.mylibrary:1.0.0`, and an app declares the dependencies:

```kotlin
implementation("com.example.library:1.0.0")
implementation("androidx.mylibrary:1.1.0") // Updated!
```

`com.example.library:1.0.0` does not handle the new result value, leading to a
runtime exception.

**Note:** The above example is one where Kotlin's `enum class` is the correct
tool and the library should **not** add a new constant! Kotlin turns this
semantic API design problem into a compiler or runtime error. This type of
library API change could silently cause app logic errors or data corruption
without the protection provided by exhaustive `when`. See
[When to use exhaustive types](#when-to-use-exhaustive-types).

`sealed class` exhibits the same characteristic; adding a new subtype of an
existing sealed class is a breaking change for the following code:

```kotlin
val message = when (command) {
    is Command.Migrate -> "migrating to ${command.destination}"
    is Command.Quack -> "quack!"
}
```

##### Non-exhaustive alternatives to `enum class`

Kotlin's `@JvmInline value class` with a `private constructor` can be used to
create type-safe sets of non-exhaustive constants as of Kotlin 1.5. Compose's
`BlendMode` uses the following pattern:

```kotlin {.good}
@JvmInline
value class BlendMode private constructor(val value: Int) {
    companion object {
        /** Drop both the source and destination images, leaving nothing. */
        val Clear = BlendMode(0)
        /** Drop the destination image, only paint the source image. */
        val Src = BlendMode(1)
        // ...
    }
}
```

**Note:** This recommendation may be temporary. Kotlin may add new annotations
or other language features to declare non-exhaustive enum classes in the future.

Alternatively, the existing `@IntDef` mechanism used in Java-language androidx
libraries may also be used, but type checking of constants will only be
performed by lint, and functions overloaded with parameters of different value
class types are not supported. Prefer the `@JvmInline value class` solution for
new code unless it would break local consistency with other API in the same
module that already uses `@IntDef`.

##### Non-exhaustive alternatives to `sealed class`

Abstract classes with constructors marked as `internal` or `private` can
represent the same subclassing restrictions of sealed classes as seen from
outside of a library module's own codebase:

```kotlin
abstract class Command private constructor() {
    class Migrate(val destination: String) : Command()
    object Quack : Command()
}
```

Using an `internal` constructor will permit non-nested subclasses, but will
**not** restrict subclasses to the same package within the module, as sealed
classes do.

##### When to use exhaustive types

Use `enum class` or `sealed class` when the values or subtypes are intended to
be exhaustive by design from the API's initial release. Use non-exhaustive
alternatives when the set of constants or subtypes might expand in a minor
version release.

Consider using an **exhaustive** (`enum class` or `sealed class`) type
declaration if:

*   The developer is expected to **accept** values of the type
*   The developer is expected to **act** on **any and all** values received

Consider using a **non-exhaustive** type declaration if:

*   The developer is expected to **provide** values of the type to APIs exposed
    by the same module **only**
*   The developer is expected to **ignore** unknown values received

The `CommandResult` example above is a good example of a type that **should**
use the exhaustive `enum class`; `CommandResult`s are **returned** to the
developer and the developer cannot implement correct app behavior by ignoring
unrecognized result values. Adding a new result value would semantically break
existing code regardless of the language facility used to express the type.

```kotlin {.good}
enum class CommandResult { Permitted, DeniedByUser, DeniedByAdmin }
```

Compose's `BlendMode` is a good example of a type that **should not** use the
exhaustive `enum class`; blending modes are used as arguments to Compose
graphics APIs and are not intended for interpretation by app code. Additionally,
there is historical precedent from `android.graphics` for new blending modes to
be added in the future.

#### Extension and top-level functions {#kotlin-extension-functions}

If your Kotlin file contains any symbols outside of class-like types
(extension/top-level functions, properties, etc), the file must be annotated
with `@JvmName`. This ensures unanticipated use-cases from Java callers don't
get stuck using `BlahKt` files.

Example:

```kotlin {.bad}
package androidx.example

fun String.foo() = // ...
```

```kotlin {.good}
@file:JvmName("StringUtils")

package androidx.example

fun String.foo() = // ...
```

NOTE This guideline may be ignored for libraries that only work in Kotlin (think
Compose).

## Testing Guidelines

### [Do not Mock, AndroidX](do_not_mock.md)

## Android Lint Guidelines

### Suppression vs Baselines

Lint sometimes flags false positives, even though it is safe to ignore these
errors (for example WeakerAccess warnings when you are avoiding synthetic
access). There may also be lint failures when your library is in the middle of a
beta / rc / stable release, and cannot make the breaking changes needed to fix
the root cause. There are two ways of ignoring lint errors:

1.  Suppression - using `@SuppressLint` (for Java) or `@Suppress` annotations to
    ignore the warning per call site, per method, or per file. *Note
    `@SuppressLint` - Requires Android dependency*.
2.  Baselines - allowlisting errors in a lint-baseline.xml file at the root of
    the project directory.

Where possible, you should use a **suppression annotation at the call site**.
This helps ensure that you are only suppressing the *exact* failure, and this
also keeps the failure visible so it can be fixed later on. Only use a baseline
if you are in a Java library without Android dependencies, or when enabling a
new lint check, and it is prohibitively expensive / not possible to fix the
errors generated by enabling this lint check.

To update a lint baseline (`lint-baseline.xml`) after you have fixed issues,
first **manually delete the `lint-baseline.xml` file** for your project and then
run the `lintDebug` task for your project with the argument
`-PupdateLintBaseline`.

```shell
rm core/core/lint-baseline.xml
./gradlew :core:core:lintDebug -PupdateLintBaseline
```

## Metalava API Lint

As well as Android Lint, which runs on all source code, Metalava will also run
checks on the public API surface of each library. Similar to with Android Lint,
there can sometimes be false positives / intended deviations from the API
guidelines that Metalava will lint your API surface against. When this happens,
you can suppress Metalava API lint issues using `@SuppressLint` (for Java) or
`@Suppress` annotations. In cases where it is not possible, update Metalava's
baseline with the `updateApiLintBaseline` task.

```shell
./gradlew core:updateApiLintBaseline
```

This will create/amend the `api_lint.ignore` file that lives in a library's
`api` directory.

## Build Output Guidelines

In order to more easily identify the root cause of build failures, we want to
keep the amount of output generated by a successful build to a minimum.
Consequently, we track build output similarly to the way in which we track Lint
warnings.

### Invoking build output validation

You can add `-Pandroidx.validateNoUnrecognizedMessages` to any other AndroidX
gradlew command to enable validation of build output. For example:

```shell
/gradlew -Pandroidx.validateNoUnrecognizedMessages :help
```

### Exempting new build output messages

Please avoid exempting new build output and instead fix or suppress the warnings
themselves, because that will take effect not only on the build server but also
in Android Studio, and will also run more quickly.

If you cannot prevent the message from being generating and must exempt the
message anyway, follow the instructions in the error:

```shell
$ ./gradlew -Pandroidx.validateNoUnrecognizedMessages :help

Error: build_log_simplifier.py found 15 new messages found in /usr/local/google/workspace/aosp-androidx-git/out/dist/gradle.log.

Please fix or suppress these new messages in the tool that generates them.
If you cannot, then you can exempt them by doing:

  1. cp /usr/local/google/workspace/aosp-androidx-git/out/dist/gradle.log.ignore /usr/local/google/workspace/aosp-androidx-git/frameworks/support/development/build_log_simplifier/messages.ignore
  2. modify the new lines to be appropriately generalized
```

Each line in this exemptions file is a regular expressing matching one or more
lines of output to be exempted. You may want to make these expressions as
specific as possible to ensure that the addition of new, similar messages will
also be detected (for example, discovering an existing warning in a new source
file).

## Behavior changes

### Changes that affect API documentation

Do not make behavior changes that require altering API documentation in a way
that would break existing clients, even if such changes are technically binary
compatible. For example, changing the meaning of a method's return value to
return true rather than false in a given state would be considered a breaking
change. Because this change is binary-compatible, it will not be caught by
tooling and is effectively invisible to clients.

Instead, add new methods and deprecate the existing ones if necessary, noting
behavior changes in the deprecation message.

### High-risk behavior changes

Behavior changes that conform to documented API contracts but are highly complex
and difficult to comprehensively test are considered high-risk and should be
implemented using behavior flags. These changes may be flagged on initially, but
the original behaviors must be preserved until the library enters release
candidate stage and the behavior changes have been appropriately verified by
integration testing against public pre-release
revisions.

It may be necessary to soft-revert a high-risk behavior change with only 24-hour
notice, which should be achievable by flipping the behavior flag to off.

```java
// Flag for whether to throw exceptions when the state is known to be bad. This
// is expected to be a high-risk change since apps may be working fine even with
// a bad state, so we may need to disable this as a hotfix.
private static final boolean FLAG_EXCEPTION_ON_BAD_STATE = false;
```

```java
/**
 * Allows a developer to toggle throwing exceptions when the state is known to
 * be bad. This method is intended to give developers time to update their code.
 * It is temporary and will be removed in a future release.
 */
@TemporaryFeatureFlag
public void setExceptionOnBadStateEnabled(boolean enabled);
```

Avoid adding multiple high-risk changes during a feature cycle, as verifying the
interaction of multiple feature flags leads to unnecessary complexity and
exposes clients to high risk even when a single change is flagged off. Instead,
wait until one high-risk change has landed in RC before moving on to the next.

#### Testing

Relevant tests should be run for the behavior change in both the on and off
flagged states to prevent regressions.

## Sample code in Kotlin modules

### Background

Public API can (and should!) have small corresponding code snippets that
demonstrate functionality and usage of a particular API. These are often exposed
inline in the documentation for the function / class - this causes consistency
and correctness issues as this code is not compiled against, and the underlying
implementation can easily change.

KDoc (JavaDoc for Kotlin) supports a `@sample` tag, which allows referencing the
body of a function from documentation. This means that code samples can be just
written as a normal function, compiled and linted against, and reused from other
modules such as tests! This allows for some guarantees on the correctness of a
sample, and ensuring that it is always kept up to date.

### Enforcement

There are still some visibility issues here - it can be hard to tell if a
function is a sample, and is used from public documentation - so as a result we
have lint checks to ensure sample correctness.

Primarily, there are three requirements when using sample links:

1.  All functions linked to from a `@sample` KDoc tag must be annotated with
    `@Sampled`
2.  All sample functions annotated with `@Sampled` must be linked to from a
    `@sample` KDoc tag
3.  All sample functions must live inside a separate `samples` library
    submodule - see the section on module configuration below for more
    information.

This enforces visibility guarantees, and make it easier to know that a sample is
a sample. This also prevents orphaned samples that aren't used, and remain
unmaintained and outdated.

### Sample usage

The follow demonstrates how to reference sample functions from public API. It is
also recommended to reuse these samples in unit tests / integration tests / test
apps / library demos where possible.

**Public API:**

```
/*
 * Fancy prints the given [string]
 *
 * @sample androidx.printer.samples.fancySample
 */
fun fancyPrint(str: String) ...
```

**Sample function:**

```
package androidx.printer.samples

import androidx.printer.fancyPrint

@Sampled
fun fancySample() {
   fancyPrint("Fancy!")
}
```

**Generated documentation visible on d.android.com\***

```
fun fancyPrint(str: String)

Fancy prints the given [string]

<code>
 import androidx.printer.fancyPrint

 fancyPrint("Fancy!")
<code>
```

\**still some improvements to be made to DAC side, such as syntax highlighting*

### Module configuration

The following module setups should be used for sample functions, and are
enforced by lint:

**Group-level samples**

For library groups with strongly related samples that want to share code.

Gradle project name: `:foo-library:foo-library-samples`

```
foo-library/
  foo-module/
  bar-module/
  samples/
```

**Per-module samples**

For library groups with complex, relatively independent sub-libraries

Gradle project name: `:foo-library:foo-module:foo-module-samples`

```
foo-library/
  foo-module/
    samples/
```

**Samples module configuration**

Samples modules are published to GMaven so that they are available to Android
Studio, which displays code in @Sample annotations as hover-over pop-ups.

To achieve this, samples modules must declare the same MavenGroup and `publish`
as the library(s) they are samples for.
