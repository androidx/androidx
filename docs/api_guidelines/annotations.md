## Annotations {#annotation}

### Annotation processors {#annotation-processor}

Annotation processors should opt-in to incremental annotation processing to
avoid triggering a full recompilation on every client source code change. See
Gradle's
[Incremental annotation processing](https://docs.gradle.org/current/userguide/java_plugin.html#sec:incremental_annotation_processing)
documentation for information on how to opt-in.

### `@RequiresOptIn` APIs {#experimental-api}

Jetpack libraries may choose to annotate API surfaces as unstable using either
Kotlin's
[`@RequiresOptIn` meta-annotation](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-requires-opt-in/)
for APIs written in Kotlin or Jetpack's
[`@RequiresOptIn` meta-annotation](https://developer.android.com/reference/kotlin/androidx/annotation/RequiresOptIn)
for APIs written in Java.

> `@RequiresOptIn` at-a-glance:
>
> *   Use for unstable API surfaces
> *   Can be called by anyone
> *   Documented in public documentation
> *   Does not maintain compatibility

For either annotation, API surfaces marked as opt-in are considered alpha and
will be excluded from API compatibility guarantees. Due to the lack of
compatibility guarantees, stable libraries *must never* call experimental APIs
exposed by other libraries outside of their
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
`@RequiresOptIn` APIs that are guaranteed to remain binary compatible *may* be
used in `beta`, but usages must be removed when the library moves to `rc`.

#### When to mark an API surface as experimental

*Do not* use `@RequiresOptIn` for a stable API surface that is difficult to use.
It is not a substitute for a properly-designed API surface.

*Do not* use `@RequiresOptIn` for an API surface that is unreliable or unstable
because it is missing tests. It is not a substitute for a properly-tested API
surface, and all APIs -- including those in `alpha` -- are expected to be
functionally stable.

*Do not* use `@RequiresOptIn` for an internal-facing API surface. Use either the
appropriate language visibility (ex. `private` or `internal`) or `@RestrictTo`.

*Do not* use `@RequiresOptIn` for an API that you expect library developers to
call. Experimental APIs do not maintain binary compatibility guarantees, and you
will put external clients in a difficult situation.

*Do* use `@RequiresOptIn` for API surfaces that must be publicly available and
documented but need the flexibility to stay in `alpha` during the rest of the
library's `beta`, `rc`, or stable cycles, and continue to break compatibility in
`beta`.

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
may only be removed during an alpha pre-release stage. Removing the experimental
marker from an API is equivalent to adding the API to the current API surface.

When transitioning an entire feature surface out of experimental, you *should*
remove the definition for the associated experimental marker annotation.

When making any change to the experimental API surface, you *must* run
`./gradlew updateApi` prior to uploading your change.

NOTE Experimental marker annotation *are themselves* experimental, meaning that
it's considered binary compatible to refactor or remove an experimental marker
annotation.

### `@RestrictTo` APIs {#restricted-api}

Jetpack's library tooling supports hiding JVM-visible (ex. `public` and
`protected`) APIs from developers using a combination of the `@RestrictTo`
source annotation.

> `@RestrictTo` at-a-glance:
>
> *   Use for internal-facing API surfaces
> *   Can be called within the specified `Scope`
> *   Does not appear in public documentation
> *   Does not maintain compatibility in most scopes

While restricted APIs do not appear in documentation and Android Studio will
warn against calling them, hiding an API does *not* provide strong guarantees
about usage:

*   There are no runtime restrictions on calling hidden APIs
*   Android Studio will not warn if hidden APIs are called using reflection
*   Hidden APIs will still show in Android Studio's auto-complete

These annotations indicate that developers should not call an API that is
*technically* public from a JVM visibility perspective. Hiding APIs is often a
sign of a poorly-abstracted API surface, and priority should be given to
creating public, maintainable APIs and using Java visibility modifiers.

*Do not* use `@RestrictTo` to bypass API tracking and review for production
APIs; instead, rely on API+1 and API Council review to ensure APIs are reviewed
on a timely basis.

*Do not* use `@RestrictTo` for implementation detail APIs that are used between
libraries and could reasonably be made public.

*Do* use `@RestrictTo(LIBRARY)` for implementation detail APIs used within a
single library (but prefer Java language `private` or `default` visibility).

#### `RestrictTo.Scope` and inter- versus intra-library API surfaces {#private-api-types}

To maintain binary compatibility between different versions of libraries,
restricted API surfaces that are used between libraries within Jetpack
(inter-library APIs) must follow the same Semantic Versioning rules as public
APIs. Inter-library APIs should be annotated with the
`@RestrictTo(LIBRARY_GROUP)` source annotation.

Restricted API surfaces used within a single library (intra-library APIs), on
the other hand, may be added or removed without any compatibility
considerations. It is safe to assume that developers *never* call these APIs,
even though it is technically feasible. Intra-library APIs should be annotated
with the `@RestrictTo(LIBRARY)` source annotation.

In all cases, correctness and compatibility tracking are handled by AndroidX's
build system and lint checks.

The following table shows the visibility of a hypothetical API within Maven
coordinate `androidx.concurrent:concurrent` when annotated with a variety of
scopes:

<table>
    <tr>
        <td><code>RestrictTo.Scope</code></td>
        <td>Visibility by Maven coordinate</td>
        <td>Versioning</td>
        <td>Note</td>
    </tr>
    <tr>
        <td><code>LIBRARY</code></td>
        <td><code>androidx.concurrent:concurrent</code></td>
        <td>No compatibility guarantees (same as private)</td>
        <td></td>
    </tr>
    <tr>
        <td><code>LIBRARY_GROUP</code></td>
        <td><code>androidx.concurrent:*</code></td>
        <td>Semantic versioning (including deprecation)</td>
        <td></td>
    </tr>
    <tr>
        <td><code>LIBRARY_GROUP_PREFIX</code></td>
        <td><code>androidx.*:*</code></td>
        <td>Semantic versioning (including deprecation)</td>
        <td></td>
    </tr>
    <tr>
        <td><code>TEST</code></td>
        <td><code>*</code></td>
        <td>No compatibility guarantees (same as private)</td>
        <td>Not allowed in Jetpack, use `@VisibleForTesting`</td>
    </tr>
</table>

#### Test APIs and `@VisibleForTesting`

For library APIs that should only be used from test code -- including the
library's own tests, integration test apps, app developers' tests, or
third-party testing libraries -- use the `@VisibleForTesting` annotation to
ensure that the API is only called from test source sets.

Libraries targeted at multi-platform usage in IntelliJ may use `@TestOnly` to
ensure that the IDE enforces usage restrictions.

Jetpack prefers that libraries expose test APIs as public API and maintain
binary compatibility. This ensures that whatever a library needs in its own
integration test app is available to app developers and may be safely called in
third-party testing libraries.

In cases where a test API needs restricted visibility or flexibility around
binary compatibility, the `@VisibleForTesting` annotation may be combined with a
`@RestrictTo` scope or `@RequiresOptIn` feature group.

#### `@IntDef` `@StringDef` and `@LongDef` and visibility

All `@IntDef`, `@StringDef`, and `@LongDef` will be stripped from resulting
artifacts to avoid issues where compiler inlining constants removes information
as to which `@IntDef` defined the value of `1`. The annotations are extracted
and packaged separately to be read by Android Studio and lint which enforces the
types in application code.

*   Libraries *must* `@RestrictTo` all `@IntDef`, `@StringDef`, and `@LongDef`
    declarations to create a warning when the type is used incorrectly.
*   Libraries *must* expose constants used to define the `@IntDef` etc at the
    same Java visibility as the hidden `@IntDef`

Here is a complete example of an `@IntDef`

```java
// constants match Java visibility of ExifStreamType
// code outside this module interacting with ExifStreamType uses these constants
public static final int STREAM_TYPE_FULL_IMAGE_DATA = 1;
public static final int STREAM_TYPE_EXIF_DATA_ONLY = 2;

@RestrictTo(RestrictTo.Scope.LIBRARY) // Don't export ExifStreamType outside module
@Retention(RetentionPolicy.SOURCE)
@IntDef({
  STREAM_TYPE_FULL_IMAGE_DATA,
  STREAM_TYPE_EXIF_DATA_ONLY,
})
public @interface ExifStreamType {}
```

Java visibility should be set as appropriate for the code in question
(`private`, `package`, or `public`) and is unrelated to hiding.

For more, read the section in
[Android API Council Guidelines](https://android.googlesource.com/platform/developers/docs/+/refs/heads/main/api-guidelines/framework.md#framework-hide-typedefs)

#### `*current.txt` File Explanation {#currenttxt}

In this example, `1.3.0-beta02.txt` is just used for an example. This will match
the current library version.

<table>
    <tr>
        <td><code>api/current.txt</code></td>
        <td>All public APIs.</td>
    </tr>
    <tr>
        <td><code>api/1.3.0-beta02.txt</code></td>
        <td>All public APIs available in version <code>1.3.0-beta02</code>.
        Used to enforce compatibility in later versions.  This file is only
        generated during Beta.</td>
    </tr>
    <tr>
        <td><code>api/public_plus_experimental_current.txt </code></td>
        <td>Superset of all public APIs (<code>api/current.txt</code>) and all
        experimental/<code>RequiresOptIn</code> APIs.
        </td>
    </tr>
    <tr>
        <td><code>api/public_plus_experimental_1.3.0-beta03.txt</code></td>
        <td>Superset of all public APIs (<code>api/1.3.0-beta02.txt.txt</code>) and all
        experimental/RequiresOptIn APIs, as available in version
        <code>1.3.0-beta02.txt</code>.  Only generated during Beta.</td>
    <tr>
        <td><code>api/restricted_current.txt</code></td>
        <td>Superset of all public APIs (<code>api/current.txt</code>) and
        all <code>RestrictTo</code> APIs that require compatibility across
        versions.
        <p/>Specifically, includes <code>@RestrictTo(LIBRARY_GROUP)</code> and
        <code>@RestrictTo(LIBRARY_GROUP_PREFIX)</code>.</td>
    </tr>
    <tr>
        <td><code>api/restricted_1.3.0-beta02.txt.txt</code></td>
        <td>Superset of all public APIs (<code>api/current.txt</code>) and
        all <code>RestrictTo</code> APIs that require compatibility across
        versions, as available in version <code>1.3.0-beta02.txt</code>.
        <p/>
        Specifically, includes <code>@RestrictTo(LIBRARY_GROUP)</code> and
        <code>@RestrictTo(LIBRARY_GROUP_PREFIX)</code>. This file is only
        generated during Beta.</td>
    </tr>
</table>
