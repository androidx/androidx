## Platform compatibility API patterns {#platform-compatibility-apis}

NOTE For all library APIs that wrap or provide parity with platform APIs, we
prefer to follow modern API guidelines; however, developers *may* choose to
prioritize parity with the platform APIs over adherence to modern guidelines.
For example, if the platform API being wrapped has incorrect `Executor` and
`Callback` ordering according to the API Guidelines, the corresponding library
API *should* re-order the arguments.

### Static shims (ex. [ViewCompat](https://developer.android.com/reference/androidx/core/view/ViewCompat)) {#static-shim}

When to use?

*   Platform class exists at module's `minSdkVersion`
*   Compatibility implementation does not need to store additional metadata

#### API guidelines {#static-shim-api-guidelines}

##### Naming {#static-shim-naming}

*   Class *should* be added to the `androidx.core:core` library
*   Class name **must** be `<PlatformClass>Compat`
*   Package name **must** be `androidx.<feature>.<platform.package>`
*   Superclass **must** be `Object`

##### Construction {#static-shim-construction}

*   Class **must** be non-instantiable, i.e. constructor is private no-op

#### Implementation {#static-shim-implementation}

*   Static fields and static methods **must** match match signatures with
    `<PlatformClass>`
    *   Static fields that can be inlined, ex. integer constants, **must not**
        be shimmed
*   Public method names **must** match platform method names
*   Public methods **must** be static and take `<PlatformClass>` as first
    parameter (except in the case of static methods on the platform class, as
    shown below)
*   Implementation *may* delegate to `<PlatformClass>` methods when available

#### Sample {#static-shim-sample}

The following sample provides static helper methods for the platform class
`android.os.Process`.

~~~java
package androidx.core.os;

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
        } else if (Build.VERSION.SDK_INT >= 16) {
            // Fall back to using reflection on private APIs.
            // ...
        } else {
            return true;
        }
    }
}

### Wrapper (ex. [AccessibilityNodeInfoCompat](https://developer.android.com/reference/android/view/accessibility/AccessibilityNodeInfo)) {#wrapper}

When to use?

*   Platform class may not exist at module's `minSdkVersion`
*   Compatibility implementation may need to store additional metadata
*   Needs to integrate with platform APIs as return value or method argument
*   **Note:** Should be avoided when possible, as using wrapper classes makes it
    very difficult to deprecate classes and migrate source code when the
    `minSdkVersion` is raised

#### API guidelines {#wrapper-api-guidelines}

##### Naming {#wrapper-naming}

*   Class name **must** be `<PlatformClass>Compat`
*   Package name **must** be `androidx.core.<platform.package>`
*   Superclass **must not** be `<PlatformClass>`

##### Construction {#wrapper-construction}

*   Class *may* have public constructor(s) to provide parity with public
    `PlatformClass` constructors
    *   Constructor used to wrap `PlatformClass` **must not** be public
*   Class **must** implement a static `PlatformClassCompat
    toPlatformClassCompat(PlatformClass)` method to wrap `PlatformClass` on
    supported SDK levels
    *   If class does not exist at module's `minSdkVersion`, method must be
        annotated with `@RequiresApi(<sdk>)` for SDK version where class was
        introduced

#### Implementation {#wrapper-implementation}

*   Class *should* be added to the `androidx.core:core` library
*   Class **must** implement a `PlatformClass toPlatformClass()` method to
    unwrap `PlatformClass` on supported SDK levels
    *   If class does not exist at module's `minSdkVersion`, method must be
        annotated with `@RequiresApi(<sdk>)` for SDK version where class was
        introduced
*   Implementation *may* delegate to `PlatformClass` methods when available (see
    below note for caveats)
*   To avoid runtime class verification issues, all operations that interact
    with the internal structure of `PlatformClass` must be implemented in inner
    classes targeted to the SDK level at which the operation was added.
    *   See the [sample](#wrapper-sample) for an example of interacting with a
        method that was added in SDK level 23.

#### Sample {#wrapper-sample}

The following sample wraps a hypothetical platform class `ModemInfo` that was
added to the platform SDK in API level 23:

```java
public final class ModemInfoCompat {
  // Only guaranteed to be non-null on SDK_INT >= 23. Note that referencing the
  // class itself directly is fine -- only references to class members need to
  // be pushed into static inner classes.
  private final Object wrappedObj;

  /**
   * [Copy platform docs for matching constructor.]
   */
  public ModemInfoCompat() {
    if (SDK_INT >= 23) {
      wrappedObj = Api23Impl.create();
    } else {
      wrappedObj = null;
    }
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
      // ...
    }
    // Default behavior.
    return false;
  }

  // All references to class members -- including the constructor -- must be
  // made on an inner class to avoid soft-verification errors that slow class
  // loading and prevent optimization.
  @RequiresApi(23)
  private static class Api23Impl {
    @DoNotInline
    @NonNull
    static ModemInfo create() {
      return new ModemInfo();
    }

    @DoNotInline
    static boolean isLteSupported(Object obj) {
      return ((ModemInfo) obj).isLteSupported();
    }
  }
}
~~~

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

### Standalone (ex. [ArraySet](https://developer.android.com/reference/androidx/collection/ArraySet), [Fragment](https://developer.android.com/jetpack/androidx/releases/fragment)) {#standalone}

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
    *   In exceptional cases, a *released* standalone class may add conversion
        between itself and the equivalent platform class; however, *new* classes
        that support conversion should follow the [Wrapper](#wrapper)
        guidelines. In these cases, use a `toPlatform<PlatformClass>` and
        `static toCompat<PlatformClass>` method naming convention.
*   Implementation *may* delegate to `PlatformClass` methods when available

### Standalone JAR library (no Android dependencies) {#standalone-jvm}

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

### Addressing class verification failures on `super.` invocation {#compat-super}

Invoking a `super` call on a method introduced in an API level higher than a
class's minimum SDK level will raise a run-time class verification failure, and
will be detected by the `ClassVerificationFailure` lint check.

```java {.bad}
public void performAction() {
  if (SDK_INT >= 31) {
    super.performAction(); // This will cause a verification failure.
  }
}
```

These failures can be addressed by out-of-lining the `super` call to a
non-static inner class.

#### Sample {#compat-super-sample}

```java
class AppCompatTextView : TextView {

  @Nullable
  SuperCaller mSuperCaller = null;

  @Override
  int getPropertyFromApi99() {
  if (Build.VERSION.SDK_INT > 99) {
    getSuperCaller().getPropertyFromApi99)();
  }

  @NonNull
  @RequiresApi(99)
  private SuperCaller getSuperCaller() {
    if (mSuperCaller == null) {
      mSuperCaller = new Api99SuperCaller();
    }
    return mSuperCaller;
  }

  @RequiresApi(99)
  private class Api99SuperCaller {
    int getPropertyFromApi99() {
      return AppCompatTextView.super.getPropertyFromApi99();
    }
  }
}
```
