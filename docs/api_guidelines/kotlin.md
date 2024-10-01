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

### Target language version {#kotlin-target}

All projects in AndroidX compile using the same version of the Kotlin
compiler -- typically the latest stable version -- and by default use a matching
*target language version*. The target language version specifies which Kotlin
features may be used in source code, which in turn specifies (1) which version
of `kotlin-stdlib` is used as a dependency and thus (2) which version of the
Kotlin compiler is required when the library is used as a dependency.

Libraries may specify `kotlinTarget` in their `build.gradle` to override the
default target language version. Using a higher language version will force
clients to use a newer, typically less-stable Kotlin compiler but allows use of
newer language features. Using a lower language version will allow clients to
use an older Kotlin compiler when building their own projects.

```
androidx {
    kotlinTarget = KotlinVersion.KOTLIN_1_7
}
```

NOTE The client's Kotlin compiler version is bounded by their *transitive
dependencies*. If your library uses target language version 1.7 but you depend
on a library with target language version 1.9, the client will be forced to use
1.9 or higher.

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

### Flow return type {#flow-return-type}

Always prefer non-null for `Flow` objects, return a Flow that does not emit
items as a default. One option is `emptyFlow()` which will complete. Another
option is `flow { awaitCancellation() }` which will not emit and not complete.
Choose the option that best suites the use-case.

```kotlin
fun myFlowFunction(): Flow<Data> {
    return if (canCreateFlow()) {
        createFlow()
    } else {
        emptyFlow()
    }
}
```

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
module that already uses `@IntDef` or compatibility with Java is required.

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

### Extension functions related to classes in the same module or file

When the core type is in Java, one good use for extension functions is to create
more Kotlin friendly versions of the API.

```java
public class MyClass {
    public void addMyListener(Executor e, Consumer<Data> listener) { ... }
    public void removeMyListener(Consumer<Data> listener) { ... }
}
```

```kotlin
fun MyClass.dataFlow(): Flow<Data>
```

When the core type is in Kotlin, extension functions may or may not be a good
fit for the API. Ask if the extension is part of the core abstraction or a new
layer of abstraction. One example of a new layer of abstraction is using a new
dependency that does not otherwise interact with the core class. Another example
is an abstraction that stands on its own such as a `Comparator` that implements
a non-canonical ordering.

In general when adding extension functions, consider splitting them across
different files and naming the Java version of the files related to the use case
as opposed to putting everything in one file and using a `Util` suffix.

```kotlin {.bad}
@file:JvmName("WindowSizeClassUtil")

fun Set<WindowSizeClass>.widestClass() : WindowSizeClass { ... }

fun WindowSizeClass.scoreWithinWidthDp(widthDp: Int) { ... }
```

```kotlin
@file:JvmName("WindowSizeClassSelector")

fun Set<WindowSizeClass>.widestClass() : WindowSizeClass { ... }

// In another file
@file:JvmName("WindowSizeClassScoreCalculator")

fun WindowSizeClass.scoreWithinWidthDp(widthDp: Int) { ... }
```

### Function parameters order {#kotlin-params-order}

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
