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
        <td>Not recommended. Prefer language visibility, e.g. `internal` or package-private.</td>
    </tr>
</table>

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

Java visibilty should be set as appropriate for the code in question (`private`,
`package` or `public`) and is unrelated to hiding.

For more, read the section in
[Android API Council Guidelines](https://android.googlesource.com/platform/developers/docs/+/refs/heads/master/api-guidelines/index.md#no-public-typedefs)

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

## Constructors {#constructors}

### View constructors {#view-constructors}

The four-arg View constructor -- `View(Context, AttributeSet, int, int)` -- was
added in SDK 21 and allows a developer to pass in an explicit default style
resource rather than relying on a theme attribute to resolve the default style
resource. Because this API was added in SDK 21, care must be taken to ensure
that it is not called through any < SDK 21 code path.

Views *may* implement a four-arg constructor in one of the following ways:

1.  Do not implement.
1.  Implement and annotate with `@RequiresApi(21)`. This means the three-arg
    constructor **must not** call into the four-arg constructor.

## Asynchronous work {#async}

### With return values {#async-return}

#### Kotlin

Traditionally, asynchronous work on Android that results in an output value
would use a callback; however, better alternatives exist for libraries.

Kotlin libraries should consider
[coroutines](https://kotlinlang.org/docs/reference/coroutines-overview.html) and
`suspend` functions for APIs according to the following rules, but please refer
to the guidance on [allowable dependencies](#dependencies-coroutines) before
adding a new dependency on coroutines.

Kotlin suspend fun vs blocking       | Behavior
------------------------------------ | --------------------------
blocking function with @WorkerThread | API is blocking
suspend                              | API is async (e.g. Future)

In general, do not introduce a suspend function entirely to switch threads for
blocking calls. To do so correctly requires that we allow the developer to
configure the Dispatcher. As there is already a coroutines-based API for
changing dispatchers (withContext) that the caller may use to switch threads, it
is unecessary API overhead to provide a duplicate mechanism. In addition, it
unecessary limits callers to coroutine contexts.

```kotlin
// DO expose blocking calls as blocking calls
@WorkerThread
fun blockingCall()

// DON'T wrap in suspend functions (only to switch threads)
suspend fun blockingCallWrappedInSuspend(
  dispatcher: CoroutineDispatcher = Dispatchers.Default
) = withContext(dispatcher) { /* ... */ }

// DO expose async calls as suspend funs
suspend fun asyncCall(): ReturnValue

// DON'T expose async calls as a callback-based API (for the main API)
fun asyncCall(executor: Executor, callback: (ReturnValue) -> Unit)
```

#### Java

Java libraries should prefer `ListenableFuture` and the
[`CallbackToFutureAdapter`](https://developer.android.com/reference/androidx/concurrent/futures/CallbackToFutureAdapter)
implementation provided by the `androidx.concurrent:concurrent-futures` library.
Functions and methods that return `ListenableFuture` should be suffixed by,
`Async` to reserve the shorter, unmodified name for a `suspend` method or
extension function in Kotlin that returns the value normally in accordance with
structured concurrency.

Libraries **must not** use `java.util.concurrent.CompletableFuture`, as it has a
large API surface that permits arbitrary mutation of the future's value and has
error-prone defaults.

See the [Dependencies](#dependencies) section for more information on using
Kotlin coroutines and Guava in your library.

### Cancellation

Libraries that expose APIs for performing asynchronous work should support
cancellation. There are *very few* cases where it is not feasible to support
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

### Avoid `synchronized` methods

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

## Kotlin-specific guidelines {#kotlin}

Generally speaking, Kotlin code should follow the compatibility guidelines
outlined at:

-   The official Android Developers
    [Kotlin-Java interop guide](https://developer.android.com/kotlin/interop)
-   Android API guidelines for
    [Kotlin-Java interop](https://android.googlesource.com/platform/developers/docs/+/refs/heads/master/api-guidelines/index.md#kotin-interop)
-   Android API guidelines for
    [asynchronous and non-blocking APIs](https://android.googlesource.com/platform/developers/docs/+/refs/heads/master/api-guidelines/async.md)
-   Library-specific guidance outlined below

### Nullability

#### Annotations on new Java APIs

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

#### Adding annotations to existing Java APIs

Adding `@Nullable` or `@NonNull` annotations to existing APIs to document their
existing nullability is allowed. This is a source-breaking change for Kotlin
consumers, and you should ensure that it's noted in the release notes and try to
minimize the frequency of these updates in releases.

Changing the nullability of an API is a behavior-breaking change and should be
avoided.

#### Extending APIs that are missing annotations

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

### Data classes {#kotlin-data}

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

As a result, Kotlin `data` classes are *strongly discouraged* in library APIs.
Instead, follow best-practices for Java data classes including implementing
`equals`, `hashCode`, and `toString`.

See Jake Wharton's article on
[Public API challenges in Kotlin](https://jakewharton.com/public-api-challenges-in-kotlin/)
for more details.

### Exhaustive `when` and `sealed class`/`enum class` {#exhaustive-when}

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

#### Non-exhaustive alternatives to `enum class`

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

#### Non-exhaustive alternatives to `sealed class`

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

#### When to use exhaustive types

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

### Extension and top-level functions {#kotlin-extension-functions}

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

NOTE This guideline may be ignored for APIs that will only be referenced from
Kotlin sources, such as Compose.

### Extension functions on platform classes {#kotlin-extension-platform}

While it may be tempting to backport new platform APIs using extension
functions, the Kotlin compiler will always resolve collisions between extension
functions and platform-defined methods by calling the platform-defined method --
even if the method doesn't exist on earlier SDKs.

```kotlin {.bad}
fun AccessibilityNodeInfo.getTextSelectionEnd() {
    // ... delegate to platform on SDK 18+ ...
}
```

For the above example, any calls to `getTextSelectionEnd()` will resolve to the
platform method -- the extension function will never be used -- and crash with
`MethodNotFoundException` on older SDKs.

Even when an extension function on a platform class does not collide with an
existing API *yet*, there is a possibility that a conflicting API with a
matching signature will be added in the future. As such, Jetpack libraries
should avoid adding extension functions on platform classes.

### Function paremeters order {#kotlin-params-order}

In Kotlin function parameters can have default values, which are used when you
skip the corresponding argument.

If a default parameter precedes a parameter with no default value, the default
value can only be used by calling the function with named arguments:

```kotlin
fun foo(
    someBoolean: Boolean = true,
    someInt: Int,
) { /*...*/ }

// usage:
foo(1) // does not compile as we try to set 1 as a value for "someBoolean" and
       // didn't specify "someInt".
foo(someInt = 1) // this compiles as we used named arguments syntax.
```

To not force our users to use named arguments we enforce the following
parameters order for the public Kotlin functions:

1.  All parameters without default values.
2.  All parameters with default values.
3.  An optional last parameter without default value which can be used as a
    trailing lambda.

### Default interface methods {#kotlin-jvm-default}

The Kotlin compiler is capable of generating Kotlin-specific default interface
methods that are compatible with Java 7 language level; however, Jetpack
libraries ship as Java 8 language level and should use the native Java
implementation of default methods.

To maximize compatibility, Jetpack libraries should pass `-Xjvm-default=all` to
the Kotlin compiler:

```
tasks.withType(KotlinCompile).configureEach {
    kotlinOptions {
        freeCompilerArgs += ["-Xjvm-default=all"]
    }
}
```

Before adding this argument, library owners must ensure that existing interfaces
with default methods in stable API surfaces are annotated with
`@JvmDefaultWithCompatibility` to preserve binary compatibility:

1.  Any interface with stable default method implementations from before the
    `all` conversion
1.  Any interface with stable methods that have default argument values from
    before the `all` conversion
1.  Any interface that extends another `@JvmDefaultWithCompatibility` interface

Unstable API surfaces do not need to be annotated, e.g. if the methods or whole
interface is `@RequiresOptIn` or was never released in a stable library version.

One way to handle this task is to search the API `.txt` file from the latest
release for `default` or `optional` and add the annotation by hand, then look
for public sub-interfaces and add the annotation there as well.

## Proguard configuration

Proguard configurations allow libraries to specify how post-processing tools
like optimizers and shrinkers should operate on library bytecode. Note that
while Proguard is the name of a specific tool, a Proguard configuration may be
read by R8 or any number of other post-processing tools.

NOTE Jetpack libraries **must not** run Proguard on their release artifacts. Do
not specify `minifyEnabled`, `shrinkResources`, or `proguardFiles` in your build
configuration.

### Bundling with a library

**Android libraries (AARs)** can bundle consumer-facing Proguard rules using the
`consumerProguardFiles` (*not* `proguardFiles`) field in their `build.gradle`
file's `defaultConfig`:

```
android {
    defaultConfig {
        consumerProguardFiles 'proguard-rules.pro'
    }
}
```

Libraries *do not* need to specify this field on `buildTypes.all`.

**Java-only libraries (JARs)** can bundle consumer-facing Proguard rules by
placing the file under the `META-INF` resources directory. The file **must** be
named using the library's unique Maven coordinate to avoid build-time merging
issues:

```
<project>/src/main/resources/META-INF/proguard/androidx.core_core.pro
```

### Conditional Proguard Rules

Libraries are strongly encouraged to minimize the number of classes that are
kept as part of keep rules. More specifically, library authors are expected to
identify the entry points of their library that call into code paths that may
require classes to be exempt from proguard rules. This may be due to internal
reflection usages or JNI code. In the case of JNI code, java/kotlin classes and
methods that are implemented in native must be exempt in order to avoid JNI
linking errors in libraries that are consumed by applications built with
proguard enabled.

A common pattern is to create an annotation class that is used to annotate all
classes and methods that are to be excluded from proguard obfuscation.

For example:

```
/// in MyProguardExceptionAnnotation.kt
internal annotation class MyProguardExemptionAnnotation
```

Then reference this annotation within your proguard config conditionally
whenever the public API is consumed that leverages facilities that need to be
excluded from proguard optimization.

```
# in proguard-rules.pro
# The following keeps classes annotated with MyProguardExemptionAnnotation
# defined above
-if class androidx.mylibrary.MyPublicApi
-keep @androidx.mylibrary.MyProguardExemptionAnnotation public class *

# The following keeps methods annotated with MyProguardExcemptionAnnotation
-if class androidx.mylibrary.MyPublicApi
-keepclasseswithmembers class * {
    @androidx.mylibrary.MyProguardExcemptionAnnotation *;
}
```

Note that for each public API entry point an additional proguard rule would need
to be introduced in the corresponding proguard-rules.pro. This is because as of
writing there is no "or" operator within proguard that can be used to include
the keep rules for multiple conditions. So each rule would need to be
copy/pasted for each public API entrypoint.
