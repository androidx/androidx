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
apps / library demos where possible to help ensure that the samples work as
intended.

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

**Generated documentation visible on d.android.com / within Android Studio**

```
fun fancyPrint(str: String)

Fancy prints the given [string]

<code>
 import androidx.printer.fancyPrint

 fancyPrint("Fancy!")
<code>
```

Warning: Only the body of the function is used in generated documentation, so
any other references to elements defined outside the body of the function (such
as variables defined within the sample file) will not be visible. To ensure that
samples can be easily copy and pasted without errors, make sure that any
references are defined within the body of the function.

### Module configuration

The following module setups should be used for sample functions:

**Per-module samples**

For library groups with relatively independent sub-libraries. This is the
recommended project setup, and should be used in most cases.

Gradle project name: `:foo-library:foo-module:foo-module-samples`

```
foo-library/
  foo-module/
    samples/
```

**Group-level samples**

For library groups with strongly related samples that want to share code and be
reused across a library group, a singular shared samples library can be created.
In most cases this is discouraged - samples should be small and show the usage
of a particular API / small set of APIs, instead of more complicated usage
combining multiple APIs from across libraries. For these cases a sample
application is more appropriate.

Gradle project name: `:foo-library:foo-library-samples`

```
foo-library/
  foo-module/
  bar-module/
  samples/
```

**Samples module configuration**

Samples modules are published to GMaven so that they are available to Android
Studio, which displays referenced samples as hover-over pop-ups.

To achieve this, samples modules must declare the same MavenGroup and `publish`
as the library(s) they are samples for.
