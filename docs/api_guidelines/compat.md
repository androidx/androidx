## Implementing compatibility {#compat}

### Referencing new APIs {#compat-newapi}

Generally, methods on library classes should be available to all devices above
the library's `minSdkVersion`; however, the behavior of the method may vary
based on platform API availability.

For example, a method may delegate to a platform API on SDKs where the API is
available, backport a subset of behavior on earlier SDKs, and no-op on very old
SDKs.

#### Checking device SDK version {#compat-sdk}

The most common way of delegating to platform or backport implementations is to
compare the device's `Build.VERSION.SDK_INT` field to a known-good SDK version;
for example, the SDK in which a method first appeared or in which a critical bug
was first fixed.

Non-reflective calls to new APIs gated on `SDK_INT` **must** be made from
version-specific static inner classes to avoid verification errors that
negatively affect run-time performance. This is enforced at build time by the
`ClassVerificationFailure` lint check, which offers auto-fixes in Java sources.

For more information, see Chromium's guide to
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
finalized, SDK checks **must** use `BuildCompat.isAtLeastX()` methods and
**must** use a tip-of-tree `project` dependency to ensure that the
implementation of `BuildCompat` stays up-to-date when the SDK is finalized.

**Do not** assume that the next SDK release's `SDK_INT` will be N+1. The value
is not finalized until SDK finalization happens, at which point the `isAtLeast`
check will be updated. **Never** write your own check for a pre-release SDK.

```java {.good}
@NonNull
public static List<Window> getAllWindows() {
  if (BuildCompat.isAtLeastR()) {
    return ApiRImpl.getAllWindows();
  }
  return Collections.emptyList();
}
```

```kotlin {.good}
dependencies {
  api(project(":core:core"))
}
```

##### Preventing invalid casting {#compat-casting}

Even when a call to a new API is moved to a version-specific class, a class
verification failure is still possible when referencing types introduced in new
APIs.

When a type does not exist on a device, the verifier treats the type as
`Object`. This is a problem if the new type is implicitly cast to a different
type which does exist on the device.

In general, if `A extends B`, using an `A` as a `B` without an explicit cast is
fine. However, if `A` was introduced at a later API level than `B`, on devices
below that API level, `A` will be seen as `Object`. An `Object` cannot be used
as a `B` without an explicit cast. However, adding an explicit cast to `B` won't
fix this, because the compiler will see the cast as redundant (as it normally
would be). So, implicit casts between types introduced at different API levels
should be moved out to version-specific static inner classes, as described
[above](#compat-sdk).

The `ImplicitCastClassVerificationFailure` lint check detects and provides
autofixes for instances of invalid implicit casts.

For instance, the following would **not** be valid, because it implicitly casts
an `AdaptiveIconDrawable` (new in API level 26, `Object` on lower API levels) to
`Drawable`. Instead, the method inside of `Api26Impl` could return `Drawable`,
or the cast could be moved into a version-specific static inner class.

```java {.bad}
private Drawable methodReturnsDrawable() {
  if (Build.VERSION.SDK_INT >= 26) {
    // Implicitly casts the returned AdaptiveIconDrawable to Drawable
    return Api26Impl.createAdaptiveIconDrawable(null, null);
  } else {
    return null;
  }
}

@RequiresApi(26)
static class Api26Impl {
  // Returns AdaptiveIconDrawable, introduced in API level 26
  @DoNotInline
  static AdaptiveIconDrawable createAdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
    return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
  }
}
```

The version-specific static inner class solution would look like this:

```java {.good}
private Drawable methodReturnsDrawable() {
  if (Build.VERSION.SDK_INT >= 26) {
    return Api26Impl.castToDrawable(Api26Impl.createAdaptiveIconDrawable(null, null));
  } else {
    return null;
  }
}

@RequiresApi(26)
static class Api26Impl {
  // Returns AdaptiveIconDrawable, introduced in API level 26
  @DoNotInline
  static AdaptiveIconDrawable createAdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
    return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
  }

  // Method which performs the implicit cast from AdaptiveIconDrawable to Drawable
  @DoNotInline
  static Drawable castToDrawable(AdaptiveIconDrawable adaptiveIconDrawable) {
    return adaptiveIconDrawable;
  }
}
```

The following would also **not** be valid, because it implicitly casts a
`Notification.MessagingStyle` (new in API level 24, `Object` on lower API
levels) to `Notification.Style`. Instead, `Api24Impl` could have a `setBuilder`
method which takes `Notification.MessagingStyle` as a parameter, or the cast
could be moved into a version-specific static inner class.

```java {.bad}
public void methodUsesStyle(Notification.MessagingStyle style, Notification.Builder builder) {
  if (Build.VERSION.SDK_INT >= 24) {
    Api16Impl.setBuilder(
      // Implicitly casts the style to Notification.Style (added in API level 16)
      // when it is a Notification.MessagingStyle (added in API level 24)
      style, builder
    );
  }
}

@RequiresApi(16)
static class Api16Impl {
  private Api16Impl() { }

  @DoNotInline
  static void setBuilder(Notification.Style style, Notification.Builder builder) {
    style.setBuilder(builder);
  }
}
```

The version-specific static inner class solution would look like this:

```java {.good}
public void methodUsesStyle(Notification.MessagingStyle style, Notification.Builder builder) {
  if (Build.VERSION.SDK_INT >= 24) {
    Api16Impl.setBuilder(
      Api24Impl.castToStyle(style), builder
    );
  }
}

@RequiresApi(16)
static class Api16Impl {
  private Api16Impl() { }

  @DoNotInline
  static void setBuilder(Notification.Style style, Notification.Builder builder) {
    style.setBuilder(builder);
  }
}

@RequiresApi(24)
static class Api24Impl {
  private Api24Impl() { }

  // Performs the implicit cast from Notification.MessagingStyle to Notification.Style
  @DoNotInline
  static Notification.Style castToStyle(Notification.MessagingStyle messagingStyle) {
    return messagingStyle;
  }
}
```

#### Validating class verification

To verify that your library does not raise class verification failures, look for
`dex2oat` output during install time.

You can generate class verification logs from test APKs. Simply call the
class/method that should generate a class verification failure in a test.

The test APK will generate class verification logs on install.

```bash
# Enable ART logging (requires root). Note the 2 pairs of quotes!
adb root
adb shell setprop dalvik.vm.dex2oat-flags '"--runtime-arg -verbose:verifier"'

# Restart Android services to pick up the settings
adb shell stop && adb shell start

# Optional: clear logs which aren't relevant
adb logcat -c

# Install the app and check for ART logs
# This line is what triggers log lines, and can be repeated
adb install -d -r someApk.apk

# it's useful to run this _during_ install in another shell
adb logcat | grep 'dex2oat'
...
... I dex2oat : Soft verification failures in
```

#### Device-specific issues {#compat-oem}

Library code may work around device- or manufacturer-specific issues -- issues
not present in AOSP builds of Android -- *only* if a corresponding CTS test
and/or CDD policy is added to the next revision of the Android platform. Doing
so ensures that such issues can be detected and fixed by OEMs.

#### Handling `minSdkVersion` disparity {#compat-minsdk}

Methods that only need to be accessible on newer devices, including
`to<PlatformClass>()` methods, may be annotated with `@RequiresApi(<sdk>)` to
indicate they must not be called when running on older SDKs. This annotation is
enforced at build time by the `NewApi` lint check.

#### Handling `targetSdkVersion` behavior changes {#compat-targetsdk}

To preserve application functionality, device behavior at a given API level may
change based on an application's `targetSdkVersion`. For example, if an app with
`targetSdkVersion` set to API level 22 runs on a device with API level 29, all
required permissions will be granted at installation time and the run-time
permissions framework will emulate earlier device behavior.

Libraries do not have control over the app's `targetSdkVersion` and -- in rare
cases -- may need to handle variations in platform behavior. Refer to the
following pages for version-specific behavior changes:

*   [API level 33](https://developer.android.com/about/versions/13/behavior-changes-13)
*   [API level 31](https://developer.android.com/about/versions/12/behavior-changes-12)
*   [API level 30](https://developer.android.com/about/versions/11/behavior-changes-11)
*   [API level 29](https://developer.android.com/about/versions/10/behavior-changes-10)
*   [API level 28](https://developer.android.com/about/versions/pie/android-9.0-changes-28)
*   [API level 26](https://developer.android.com/about/versions/oreo/android-8.0-changes)
*   [API level 24](https://developer.android.com/about/versions/nougat/android-7.0-changes)
*   [API level 21](https://developer.android.com/about/versions/lollipop/android-5.0-changes)
*   [API level 19](https://developer.android.com/about/versions/kitkat/android-4.4#Behaviors)

#### Working around Lint issues {#compat-lint}

In rare cases, Lint may fail to interpret API usages and yield a `NewApi` error
and require the use of `@TargetApi` or `@SuppressLint('NewApi')` annotations.
Both of these annotations are strongly discouraged and may only be used
temporarily. They **must never** be used in a stable release. Any usage of these
annotation **must** be associated with an active bug, and the usage must be
removed when the bug is resolved.

### Delegating to API-specific implementations {#delegating-to-api-specific-implementations}

#### SDK-dependent reflection {#sdk-reflection}

Note: The
[BanUncheckedReflection](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:lint-checks/src/main/java/androidx/build/lint/BanUncheckedReflection.kt)
lint check detects disallowed usages of reflection.

Starting in API level 28, the platform restricts which
[non-SDK interfaces](https://developer.android.com/distribute/best-practices/develop/restrictions-non-sdk-interfaces)
can be accessed via reflection by apps and libraries. As a general rule, you
will **not** be able to use reflection to access hidden APIs on devices with
`SDK_INT` greater than `Build.VERSION_CODES.P` (28).

In cases where a hidden API is a constant value, **do not** inline the value.
Hidden APIs cannot be tested by CTS and carry no stability guarantees.

On earlier devices or in cases where an API is marked with
`@UnsupportedAppUsage`, reflection on hidden platform APIs is allowed **only**
when an alternative public platform API exists in a later revision of the
Android SDK. For example, the following implementation is allowed:

```java
public AccessibilityDelegate getAccessibilityDelegate(View v) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // Retrieve the delegate using a public API.
        return v.getAccessibilityDelegate();
    } else if (Build.VERSION.SDK_INT >= 11) {
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
} else if (Build.SDK_INT.VERSION >= 23) {
   // make a best-effort using APIs that we expect to be available
} else {
   // no-op or best-effort given no information
}
```

### Inter-process communication {#ipc}

Protocols and data structures used for IPC must support interoperability between
different versions of libraries and should be treated similarly to public API;
however, AndroidX does not currently implement compatibility tracking for IPC.

We recommend the following, in order of preference:

1.  Stable AIDL if (1) your project lives partially in the Android platform and
    has access to Stable AIDL build rules and (2) you need to support Android's
    `Parcelable` data types. The AndroidX workflow **does not** provide Stable
    AIDL compilation or compatibility checks, so these would need to happen in
    the platform build and the resulting `.java` files would need to be copied
    out.
2.  Protobuf if (1) your project needs to persist data to disk or (2) you need
    interoperability with systems already using Protobuf. Similar to Stable
    AIDL, the AndroidX workflow **does not** provide built-in support Protobuf
    compilation or compatibility checks. It is possible to use a Proto plug-in,
    but you will be responsible for bundling the runtime and maintaining
    compatibility on your own.
3.  `Bundle` if you have a very simple data model that is unlikely to change in
    the future. `Bundle` has the weakest type safety and compatibility
    guarantees of any recommendation, and it has many caveats that make it a
    poor choice.
4.  `VersionedParcelable` if your project is already using Versioned Parcelable
    and is aware of its compatibility constraints.

We are currently evaluating Square's [Wire](https://github.com/square/wire) and
Google's [gRPC](https://grpc.io/) libraries for recommendation. If either of
these libraries meets your team's needs based on your own research, feel free to
use them.

**Do not** design your own serialization mechanism or wire format for disk
storage or inter-process communication. Preserving and verifying compatibility
is difficult and error-prone.

In all cases, **do not** expose your serialization mechanism in your API
surface. Neither Stable AIDL nor Protobuf generate stable language APIs.

#### Annotating unstable IPC

Once an API that relies on an IPC contract ships to production in an app, the
contract is locked in and must maintain compatibility to prevent crashing either
end of an inter-process communication channel.

Developers **should** annotate unstable IPC classes with a `@RequiresOptIn`
annotation explaining that they must not be used in production code. Libraries
**must not** opt-in to these annotations when such classes are referenced
internally, and should instead propagate the annotations to public API surfaces.

A single annotation for this purpose may be defined per library or atomic group:

```java
/**
 * Parcelables and AIDL-generated classes bearing this annotation are not
 * guaranteed to be stable and must not be used for inter-process communication
 * in production.
 */
@RequiresOptIn
public @interface UnstableAidlDefinition {}
```

Generally speaking, at this point in time no libraries should have unstable
`Parcelable` classes defined in source code, but for completeness:

```java
@UnstableAidlDefinition
public class ResultReceiver implements Parcelable { ... }
```

AIDL definition files under `src/aidl` should use `@JavaPassthrough` with a
fully-qualified class name to annotate generated classes:

```java
@JavaPassthrough(annotation="@androidx.core.util.UnstableAidlDefinition")
oneway interface IResultReceiver {
    void send(int resultCode, in Bundle resultData);
}
```

For Stable AIDL, the build system enforces per-CL compatibility guarantees. No
annotations are required for Stable AIDL definition files under
`src/stableAidl`.

#### Parcelable {#ipc-parcelable}

**Do not** implement `Parcelable` for any class that may be used for IPC or
otherwise exposed as public API. By default, `Parcelable` does not provide any
compatibility guarantees and will result in crashes if fields are added or
removed between library versions. If you are using Stable AIDL, you *may* use
AIDL-defined parcelables for IPC but not public API.

NOTE As of 2022/12/16, we are working on experimental support for compiling and
tracking Stable AIDL definitions within the AndroidX workflow.

#### Protobuf {#ipc-protobuf}

Developers **should** use protocol buffers for most cases. See
[Protobuf](#dependencies-protobuf) for more information on using protocol
buffers in your library. **Do** use protocol buffers if your data structure is
complex and likely to change over time. If your data includes `FileDescriptor`s,
`Binder`s, or other platform-defined `Parcelable` data structures, they will
need to be stored alongside the protobuf bytes in a `Bundle`.

NOTE We are currently investigating the suitability of Square's
[`wire` library](https://github.com/square/wire) for handling protocol buffers
in Android libraries. If adopted, it will replace `proto` library dependencies.
Libraries that expose their serialization mechanism in their API surface *will
not be able to migrate*.

#### Bundle {#ipc-bundle}

Developers **may** use `Bundle` in simple cases that require sending `Binder`s,
`FileDescriptor`s, or platform `Parcelable`s across IPC
([example](https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:core/core/src/main/java/androidx/core/graphics/drawable/IconCompat.java;l=820)).
Note that `Bundle` has several caveats:

-   When running on Android S and below, accessing *any* entry in a `Bundle`
    will result in the platform attempting to deserialize *every* entry. This
    has been fixed in Android T and later with "lazy" bundles, but developers
    should be careful when accessing `Bundle` on earlier platforms. If a single
    entry cannot be loaded -- for example if a developer added a custom
    `Parcelable` that doesn't exist in the receiver's classpath -- an exception
    will be thrown when accessing *any* entry.
-   On all platforms, library code that receives `Bundle`s data from outside the
    process **must** read the data defensively. See previous note regarding
    additional concerns for Android S and below.
-   On all platforms, library code that sends `Bundle`s outside the process
    *should* discourage clients from passing custom `Parcelable`s.
-   `Bundle` provides no versioning and Jetpack provides no affordances for
    tracking the keys or value types associated with a `Bundle`. Library owners
    are responsible for providing their own system for guaranteeing wire format
    compatibility between versions.

#### Communication protocols {#ipc-protocol}

Any communication prototcol, handshake, etc. must maintain compatibility
consistent with SemVer guidelines. Consider how your protocol will handle
addition and removal of operations or constants, compatibility-breaking changes,
and other modifications without crashing either the host or client process.
