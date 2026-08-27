---
name: webkit-api-development
description: >-
  Step-by-step guide for implementing AndroidX WebKit APIs in
  frameworks/support/webkit/ after boundary interface roll. Covers feature
  flags, API design, glue delegation, instrumentation tests, and release
  unhiding. Use when implementing, modifying, or unhiding WebKit APIs.
---

# AndroidX WebKit API Implementation Guide

## Overview

This guide defines the workflow for implementing WebKit features or APIs in
the AndroidX repository (`frameworks/support/webkit/`).

## When to use this skill

*   When adding feature flags to `WebViewFeature` and `WebViewFeatureInternal`.
*   When implementing or modifying AndroidX WebKit APIs in
    `frameworks/support/webkit/`.
*   When writing AndroidX WebKit instrumentation tests in
    `webkit/integration-tests/instrumentation/`.
*   When unhiding an existing `@RestrictTo(LIBRARY_GROUP)` WebKit API for public
    release.

---

## Workflow Overview

1.  **Step 0: Verify Boundary Interface**: Check `Features.java` and boundary
    interfaces in `external/webview_support_interfaces/`. Roll from Chromium if
    missing.
2.  **Step 1: Feature Flags**: Add `@RestrictTo(LIBRARY_GROUP)` constant in
    `WebViewFeature.java` and map in `internal/WebViewFeatureInternal.java`
    (NO `@RestrictTo` in internal package).
3.  **Step 2: App-Facing API Design**:
    *   **Interfaces (Option A)**: Only for developer-implemented callbacks.
        Provide `default` method throwing `UnsupportedOperationException`.
    *   **Static Compat Classes (Option B)**: For extending framework classes
        (`WebViewCompat`, `WebSettingsCompat`). Public static method.
    *   **Abstract Controllers (Option C)**: For singletons
        (`ServiceWorkerControllerCompat`). Abstract or concrete method.
    *   **Domain Classes (Option D)**: For objects created by WebView
        (`Navigation`, `Page`). Concrete class wrapping internal `*Impl`
        delegate.
4.  **Step 3: Internal Delegation**: Implement in `internal/*Impl.java` (no
    `@RestrictTo`). Guard with `isSupportedByWebView()`. Callback adapters must
    include feature in `getSupportedFeatures()`.
5.  **Step 4: Instrumentation Tests**: In `webkit/integration-tests/instrumentation/`.
    Check `MULTI_PROFILE` in `setUp()` for `Profile` tests. Use
    `WebkitUtils.checkFeature(<FEATURE>)` (no negative fallback tests).
6.  **Step 5: Unhide and Release**: Remove `@RestrictTo` from public API and
    `WebViewFeature.java`. Add mapping to `PublicFeatureAvailability.kt`. Run
    `./gradlew :webkit:webkit:updateApi`. Add `Relnote:` following
    `webkit/webkit/docs/release_notes.md`.

---

### Step 0: Verify or Roll Upstream Boundary Interface

Verify boundary interface and feature constant exist in
`external/webview_support_interfaces/`:

1.  Check `Features.java` under `external/webview_support_interfaces/` for the
    feature constant.
2.  Check the boundary interface (such as `ProfileBoundaryInterface.java` or
    `WebSettingsBoundaryInterface.java`) for the method signature.
3.  If not present, roll boundary interface from Chromium into
    `external/webview_support_interfaces/` on Gerrit before proceeding.

---

### Step 1: Define Feature Flags (`WebViewFeature` and `WebViewFeatureInternal`)

1.  **Public Constant (`WebViewFeature.java`)**:
    *   Declare a `public static final String` matching the boundary feature name.
    *   Add constant to `@StringDef` annotation (`WebViewSupportFeature`).
    *   Annotate with `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` while in
        development.

    ```java
    /**
     * Feature for {@link #isFeatureSupported(String)}.
     * This feature covers {@link <TargetClass>#<methodName>(...)}.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String <FEATURE_NAME> = "<FEATURE_NAME>";
    ```

2.  **Internal Mapping (`internal/WebViewFeatureInternal.java`)**:
    *   Map to boundary constant from
        `org.chromium.support_lib_boundary.util.Features`.
    *   **Do NOT add `@RestrictTo` in the `internal` package.**

    ```java
    public static final ApiFeature.NoFramework <FEATURE_NAME> =
            new ApiFeature.NoFramework(WebViewFeature.<FEATURE_NAME>,
                    Features.<FEATURE_NAME>);
    ```

---

### Step 2: App-Facing API Design by Class Type

#### Option A: Interfaces

*   **Rule**: Use public interfaces **only** for types the app developer
    implements (callbacks/listeners).
*   **Rule**: When adding methods to existing interfaces, **always provide a
    `default` implementation throwing `UnsupportedOperationException`** to
    prevent breaking embedders and test mocks.

```java
@RequiresFeature(name = WebViewFeature.<FEATURE_NAME>,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
@UiThread
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
default <ReturnType> <methodName>(<Parameters>) {
    throw new UnsupportedOperationException(
            "<TargetClass>#<methodName> is not implemented.");
}
```

#### Option B: Static Compat Utility Classes

*   **Rule**: When extending framework classes (`WebView`, `WebSettings`,
    `CookieManager`), add a `public static` method to `*Compat` taking the
    framework instance as first parameter.

```java
@RequiresFeature(name = WebViewFeature.<FEATURE_NAME>,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public static <ReturnType> <methodName>(
        @NonNull <FrameworkClass> instance, <Parameters>) {
    ApiFeature.<ApiLevel> feature = WebViewFeatureInternal.<FEATURE_NAME>;
    if (feature.isSupportedByFramework()) {
        ApiHelperFor<ApiLevel>.<methodName>(instance, <Parameters>);
    } else if (feature.isSupportedByWebView()) {
        getAdapter(instance).<methodName>(<Parameters>);
    } else {
        throw WebViewFeatureInternal.getUnsupportedOperationException();
    }
}
```

#### Option C: Abstract Controllers / Singletons

*   **Rule**: For singleton controllers (`ServiceWorkerControllerCompat`),
    declare an `abstract` method (or concrete method throwing
    `UnsupportedOperationException` if subclassing is supported).

```java
@RequiresFeature(name = WebViewFeature.<FEATURE_NAME>,
        enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract <ReturnType> <methodName>(<Parameters>);
```

#### Option D: Normal Domain Classes

*   **Rule**: For domain objects provided by WebView (`Navigation`, `Page`),
    expose a `public` concrete class wrapping an internal `*Impl` delegate.

```java
public class Navigation {
    private final NavigationImpl mImpl;

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Navigation(@NonNull NavigationImpl impl) {
        mImpl = impl;
    }

    @RequiresFeature(name = WebViewFeature.<FEATURE_NAME>,
            enforcement = "androidx.webkit.WebViewFeature#isFeatureSupported")
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public @Nullable String getUrl() {
        return mImpl.getUrl();
    }
}
```

---

### Step 3: Implement Internal Delegation (`internal/*Impl.java`)

*   **For Interfaces, Controllers, and Domain Classes**: Implement in
    `internal/<Class>Impl.java`. No `@RestrictTo` in `internal/`.
    Guard with `isSupportedByWebView()`:

    ```java
    @Override
    public <ReturnType> <methodName>(<Parameters>) {
        ApiFeature.NoFramework feature = WebViewFeatureInternal.<FEATURE_NAME>;
        if (feature.isSupportedByWebView()) {
            mBoundaryImpl.<methodName>(<Parameters>);
        } else {
            throw WebViewFeatureInternal.getUnsupportedOperationException();
        }
    }
    ```

*   **For Static Compat Classes**: Delegation logic lives directly in the
    static method in `*Compat.java`.

#### Complex Types and Callbacks (Reflection Adapters)

1.  **Passing Adapters**: Wrap parameter/callback in adapter and create
    `InvocationHandler` using `BoundaryInterfaceReflectionUtil`:

    ```java
    InvocationHandler paramsBoundaryInterface =
            BoundaryInterfaceReflectionUtil.createInvocationHandlerFor(
                    new CustomParametersAdapter(params));

    mBoundaryImpl.performOperation(
            paramsBoundaryInterface,
            CustomCallbackAdapter.buildInvocationHandler(callback));
    ```

2.  **Callback `getSupportedFeatures()`**: Callback adapters implementing
    `FeatureFlagHolderBoundaryInterface` **must add the feature constant in
    `getSupportedFeatures()`**:

    ```java
    public class CustomCallbackAdapter
            implements CustomCallbackBoundaryInterface {
        private final CustomCallback mCallback;

        public CustomCallbackAdapter(@NonNull CustomCallback callback) {
            mCallback = callback;
        }

        @Override
        public @NonNull String[] getSupportedFeatures() {
            return new String[] {
                Features.<FEATURE_NAME>,
            };
        }

        @Override
        public void onResult(int status) {
            mCallback.onResult(status);
        }
    }
    ```

3.  **Receiving Boundary Objects**: Cast `InvocationHandler` back using
    `BoundaryInterfaceReflectionUtil.castToSuppLibClass`:

    ```java
    InvocationHandler boundaryObject = mBoundaryImpl.getFeatureObject();
    mFeatureObject = new FeatureObject(Objects.requireNonNull(
            BoundaryInterfaceReflectionUtil.castToSuppLibClass(
                    FeatureObjectBoundaryInterface.class, boundaryObject)));
    ```

---

### Step 4: Add Instrumentation Test Coverage

Create or update test in `webkit/integration-tests/instrumentation/`:

1.  **Parent Prerequisites**: For `Profile` APIs, check
    `WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)` in `setUp()`.
2.  **Inferred Types**: Do not provide explicit generic type arguments in
    `WebkitUtils.onMainThreadSync`.
3.  **Positive Tests Only**: Use
    `WebkitUtils.checkFeature(WebViewFeature.<FEATURE_NAME>)` to verify execution.
    **Do not write negative or unsupported fallback tests.**

```kotlin
@MediumTest
@RunWith(AndroidJUnit4::class)
class FeatureNameTest {
    private lateinit var targetInstance: TargetClass

    @Before
    fun setUp() {
        // For Profile APIs: WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)
        targetInstance = ...
    }

    @Test
    fun testFeature_doesNotCrash() {
        WebkitUtils.checkFeature(WebViewFeature.YOUR_FEATURE)
        WebkitUtils.onMainThreadSync {
            targetInstance.performAction(...)
        }
    }
}
```

*   **Example References**:
    *   [`aosp/4133893`](https://android-review.googlesource.com/c/platform/frameworks/support/+/4133893):
        *Add Profile#enqueuePreconnect API (Interface).*
    *   [`aosp/4146215`](https://android-review.googlesource.com/c/platform/external/webview_support_interfaces/+/4146215):
        *Roll boundary interfaces.*

---

### Step 5: Unhide and Release Feature

When upstream Chromium development is complete:

1.  **Remove `@RestrictTo`**: Remove
    `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` from `WebViewFeature.java` and
    public API classes.
2.  **Update `PublicFeatureAvailability.kt`**: Add mapping in
    `PUBLIC_FEATURE_UNHIDE_CLS` pointing to the Chromium CL URL:

    ```kotlin
    @Suppress("DEPRECATION")
    internal val PUBLIC_FEATURE_UNHIDE_CLS =
        mapOf(
            // Existing mappings...
            WF.<FEATURE_NAME> to "https://crrev.com/c/<chromium_cl_number>",
        )
    ```

3.  **Update API Signature File**:

    ```bash
    ./gradlew :webkit:webkit:updateApi
    ```

---

## Examples: End-to-End Walkthrough (`Profile#setFooEnabled`)

### Example 1: Implement a Hidden Feature API

1.  **Define Flags**:
    *   `WebViewFeature.java`: Add `@RestrictTo(LIBRARY_GROUP) public static final String FOO_FEATURE = "FOO_FEATURE";`.
    *   `WebViewFeatureInternal.java`: Add `public static final ApiFeature.NoFramework FOO_FEATURE = new ApiFeature.NoFramework(WebViewFeature.FOO_FEATURE, Features.FOO_FEATURE);`.
2.  **Declare API**:
    *   `Profile.java`: Add `default void setFooEnabled(boolean enabled) { throw new UnsupportedOperationException("Profile#setFooEnabled is not implemented."); }` with `@RequiresFeature(name = WebViewFeature.FOO_FEATURE)` and `@RestrictTo(LIBRARY_GROUP)`.
3.  **Implement Delegation**:
    *   `ProfileImpl.java`: Add `setFooEnabled(boolean enabled)` checking `WebViewFeatureInternal.FOO_FEATURE.isSupportedByWebView()` and delegating to `mProfileImpl.setFooEnabled(enabled)`.
4.  **Add Test**:
    *   `FooFeatureTest.kt`: Check `MULTI_PROFILE` in `setUp()`, call `defaultProfile.setFooEnabled(true)` inside `WebkitUtils.checkFeature(WebViewFeature.FOO_FEATURE)`.
5.  **Commit**: Create commit omitting `Relnote:`.

### Example 2: Unhide API for Public Release

1.  **Remove `@RestrictTo`**: Remove from `WebViewFeature.FOO_FEATURE` and `Profile#setFooEnabled`.
2.  **Update `PublicFeatureAvailability.kt`**: Add `WF.FOO_FEATURE to "https://crrev.com/c/<chromium_cl_number>"` to `PUBLIC_FEATURE_UNHIDE_CLS`.
3.  **Update API**: Run `./gradlew :webkit:webkit:updateApi`.
4.  **Verify**: Run `./gradlew :webkit:webkit:lint :webkit:webkit:assemble :webkit:webkit:runErrorProne`.
5.  **Commit**: Create commit with `Relnote: "Added \`Profile.setFooEnabled\` to enable or disable Foo on a per-profile basis."` (following `webkit/webkit/docs/release_notes.md`).

---

## Commit Message Guidelines (`Relnote:` Rules)

Follow AndroidX commit formatting conventions. For release notes rules, read
`webkit/webkit/docs/release_notes.md`.

*   **Hidden API CL (Step 1 - 4)**: Omit `Relnote:` entirely when API is under
    `@RestrictTo`.
*   **Unhiding CL (Step 5)**: Include `Relnote: "Added \`<Class>#<methodName>\` to ..."`
    with backticks around symbol names.

---

## Best Practices and Pitfalls

*   **No `@RestrictTo` in `internal`**: Never put `@RestrictTo` in `androidx.webkit.internal.*`.
*   **Interfaces for Developer Implementation Only**: Do not use interfaces for types returned by WebView; use concrete classes (Option D).
*   **Never break embedder subclasses**: Always use `default` methods throwing `UnsupportedOperationException` on public interfaces.
*   **Callback `getSupportedFeatures()`**: Always include feature constant in callback boundary adapters.
*   **Always guard calls**: Check `WebViewFeatureInternal.<FEATURE>.isSupportedByWebView()` before calling boundary interfaces.
*   **Profile Tests Require `MULTI_PROFILE`**: Check `WebkitUtils.checkFeature(WebViewFeature.MULTI_PROFILE)` in `setUp()`.
*   **Positive Tests Only**: Do not assert `UnsupportedOperationException` for disabled features.
*   **Update `PublicFeatureAvailability.kt` on Unhide**: Always register feature constant and Chromium CL URL when unhiding.
*   **Preserve docstrings**: Do not modify existing KDoc/Javadoc comments when modifying classes.
