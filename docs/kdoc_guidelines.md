# Kotlin documentation (KDoc) guidelines

[TOC]

This guide contains documentation guidance specific to Jetpack Kotlin APIs.
General guidance from
s.android.com/api-guidelines#docs
should still be followed, this guide contains extra guidelines specifically for
Kotlin documentation. For detailed information on KDoc's supported tags and
syntax, see the
[official documentation](https://kotlinlang.org/docs/kotlin-doc.html).

## Guidelines for writing KDoc

### Every parameter / property in a function / class should be documented

Without an explicit `@param` / `@property` tag, a table entry for a particular
parameter / property will not be generated. Make sure to add tags for *every*
element to generate full documentation for an API.

```kotlin {.good}
/**
 * ...
 *
 * @param1 ...
 * @param2 ...
 * @param3 ...
 */
fun foo(param1: Int, param2: String, param3: Boolean) {}
```

### Consider using all available tags to add documentation for elements

Kotlin allows defining public APIs in a concise manner, which means that it can
be easy to forget to write documentation for a specific element. For example,
consider the following contrived example:

```kotlin
class Item<T>(val label: String, content: T)

fun <T> Item<T>.appendContent(content: T): Item<T> { ... }
```

The class declaration contains:

*   A generic type - `T`
*   A property (that is also a constructor parameter) - `label`
*   A constructor parameter - `content`
*   A constructor function - `Item(label, content)`

The function declaration contains:

*   A generic type - `T`
*   A receiver - `Item<T>`
*   A parameter - `content`
*   A return type - `Item<T>`

When writing KDoc, consider adding documentation for each element:

```kotlin {.good}
/**
 * An Item represents content inside a list...
 *
 * @param T the type of the content for this Item
 * @property label optional label for this Item
 * @param content the content for this Item
 * @constructor creates a new Item
 */
class Item<T>(val label: String? = null, content: T)

/**
 * Appends [content] to [this] [Item], returning a new [Item].
 *
 * @param T the type of the content in this [Item]
 * @receiver the [Item] to append [content] to
 * @param content the [content] that will be appended to [this]
 * @return a new [Item] representing [this] with [content] appended
 */
fun <T> Item<T>.appendContent(content: T): Item<T> { ... }
```

You may omit documentation for simple cases, such as a constructor for a data
class that just sets properties and has no side effects, but in general it can
be helpful to add documentation for all parts of an API.

### Use `@sample` for each API that represents a standalone feature, or advanced behavior for a feature

`@sample` allows you to reference a Kotlin function as sample code inside
documentation. The body of the function will be added to the generated
documentation inside a code block, allowing you to show usage for a particular
API. Since this function is real Kotlin code that will be compiled and linted
during the build, the sample will always be up to date with the API, reducing
maintenance. You can use multiple samples per KDoc, with text in between
explaining what the samples are showing. For more information on using
`@sample`, see the
[API guidelines](/company/teams/androidx/api_guidelines.md#sample-code-in-kotlin-modules).

### Do not link to the same identifier inside documentation

Avoid creating self-referential links:

```kotlin {.bad}
/**
 * [Item] is ...
 */
class Item
```

These links are not actionable, as they will take the user to the same element
they are already reading - instead refer to the element in the matching case
without a link:

```kotlin {.good}
/**
 * Item is ...
 */
class Item
```

### Include class name in `@see`

When referring to a function using `@see`, include the class name with the
function - even if the function the `see` is referring to is in the same class.

Instead of:

```kotlin {.bad}
/**
 * @see .myCoolFun
 */
```

Do this:

```kotlin {.good}
/**
 * @see MyCoolClass.myCoolFun
 */
```

### Match `@param` with usage

When using `@param` to refer to a variable, the spelling must match the
variable's code declaration.

Instead of:

```kotlin {.bad}
/**
 * @param myParam
 */
public var myParameter: String
```

Do this:

```kotlin {.good}
/**
 * @param myParameter
 */
public var myParameter: String
```

### Don't mix `@see` and Markdown links

Instead of:

```kotlin {.bad}
/**
 * @see [MyCoolClass.myCoolFun()][androidx.library.myCoolFun] for more details.
 */
```

Do this:

```kotlin {.good}
/**
 * See [MyCoolClass.myCoolFun()][androidx.library.myCoolFun] for more details.
 */
```

### Don't use angle brackets for `@param`

Instead of:

```kotlin {.bad}
/**
 * @param <T> my cool param
 */
```

Do this:

```kotlin {.good}
/**
 * * @param T my cool param
 */
```

This syntax is correct is Javadoc, but angle brackets aren't used in KDoc
([@param reference guide](https://kotlinlang.org/docs/kotlin-doc.html#param-name)).

## Javadoc - KDoc differences

Some tags are shared between Javadoc and KDoc, such as `@param`, but there are
notable differences between the syntax and tags supported. Unsupported syntax /
tags do not currently show as an error in the IDE / during the build, so be
careful to look out for the following important changes.

### Hiding documentation

Using `@suppress` will stop documentation being generated for a particular
element. This is equivalent to using `@hide` in Android Javadoc.

### Deprecation

To mark an element as deprecated, use the `@Deprecated` annotation on the
corresponding declaration, and consider including a `ReplaceWith` fragment to
suggest the replacement for deprecated APIs.

```kotlin {.good}
package androidx.somepackage

@Deprecated(
    "Renamed to Bar",
    replaceWith = ReplaceWith(
        expression = "Bar",
        // import(s) to be added
        "androidx.somepackage.Bar"
    )
)
class Foo

class Bar
```

This is equivalent to using the `@deprecated` tag in Javadoc, but allows
specifying more detailed deprecation messages, and different 'severity' levels
of deprecation. For more information see the documentation for
[@Deprecated](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-deprecated/).

### Linking to elements

To link to another element, put its name in square brackets. For example, to
refer to the class `Foo`, use `[Foo]`. This is equivalent to `{@link Foo}` in
Javadoc. You can also use a custom label, similar to Markdown: `[this
class][Foo]`.

### Code spans

To mark some text as code, surround the text with a backtick (\`) as in
Markdown. For example, \`true\`. This is equivalent to `{@code true}` in
Javadoc.

### Inline markup

KDoc uses Markdown for inline markup, as opposed to Javadoc which uses HTML. The
IDE / build will not show a warning if you use HTML tags such as `<p>`, so be
careful not to accidentally use these in KDoc.
