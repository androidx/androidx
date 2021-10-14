# Integrating proprietary components

go/androidx/open_source

<!--*
# Document freshness: For more information, see go/fresh-source.
freshness: { owner: 'alanv' reviewed: '2021-07-15' }
*-->

[TOC]

One of the core principles of Jetpack is "Developed as open-source and
compatible with AOSP Android," but what does that mean in practice? This guide
provides specific, technical guidance on developing an open-source library and
interacting with proprietary or closed-source libraries and services.

## What do we mean by "open-source"?

Our definition of open-source includes products that provide publicly-available
source code that can be compiled by an end-user to generate a functional version
of the product, e.g. an `AAR`, that is equivalent to the one used by the
library.

### Exceptions

The only exception to this definition is the Android platform SDK, which does
not release sources until well after its API surface has been finalized.

Libraries which are developed against the pre-release Android platform SDK _may_
remain closed-source until the platform SDK's API surface is finalized, at which
they **must** move to open-source.

### Examples of products that are _not_ open-source

*   A bundled `.so` file with no publicly-available source code
*   A Maven dependency with no publicly-available source code, either in the
    Maven distribution (ex. source `JAR`) or in a public repository
*   A library that ships source code to GitHub, but the source does not compile
*   A library that ships source code to AOSP, but binary compiled from that
    source is not functionally equivalent to the library used by Jetpack
*   A closed-source web service
*   Google Play Services

## Why do we care?

### Compatibility with AOSP ecosystem

The Android Open-Source Project enables a diverse ecosystem of devices with a
wide array of software environments in which our libraries will operate. Many of
those devices are certified to run Play Services, but it's important for our
libraries to work on all devices that are certified as Android -- even those
with no Google software installed.

*   Features provided by primary artifacts **must** be able to function on AOSP
    devices without the presence of proprietary components like Play Services

### Testing and testability

Isolating behavior makes it easier to write reliable and targeted tests, but
introducing dependencies on proprietary components makes this difficult. In a
well-abstracted library, developers should be able to write integration tests
against the library's documented API surface without concerning themselves with
the implementation details of a backing service.

*   Features provided by primary artifacts that may be backed by proprietary
    components **must** be written in way that makes it feasible for a developer
    to write and delegate to their own backing implementation

### Developer choice

Developers should be able to choose between proprietary components; however,
libraries are also encouraged to provide a sensible default.

*   Features provided by primary artifacts that may be backed by proprietary
    components **must** allow developers to choose a specific backing component
    and **must not** hard-code proprietary components as the default choice
*   Libraries _may_ use a ranking or filtering heuristic based on platform APIs
    such as permissions, presence on the system image, or other properties of
    applications and packages

### Open protocols

Third-party developers should be able to provide their own backing services,
which means service discovery mechanisms, communication protocols, and API
surfaces used to implement a backing service must be publicly available for
implementation.

Third-party developers should also be able to validate that their implementation
conforms to the expectations of the library. Library developers should already
be writing tests to cover their backing service, e.g. that a service
implementing a protocol or interface is correct, and in many cases these tests
will be suitable for third-party developers to verify their own implementations.

While we recommend that developers provide a stub backing implementation in a
`-testing` artifact or use one in their own unit tests, we do not require one to
be provided; only that it is possible to write one.

## Examples of policy violations

*   A primary artifact uses `Intent` handling as a service discovery mechanism
    and hard-codes a reference to `com.google.android` as a ranking heuristic.
    *   **What's wrong?** This conflicts with the developer choice principle.
        Primary artifacts must remain neutral regarding specific proprietary
        components.
    *   **How to fix?** This library should use an alternative ranking heuristic
        that takes advantage of platform APIs such as granted permissions or
        presence of the component on the system image (see
        [FLAG_SYSTEM](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_SYSTEM)
        and
        [FLAG_UPDATED_SYSTEM_APP](https://developer.android.com/reference/android/content/pm/ApplicationInfo#FLAG_UPDATED_SYSTEM_APP)).
        The library will also need to provide an API that allows developers to
        choose an explicit ranking or default component.
*   A primary artifact uses reflection to delegate to a specific fully-qualified
    class name. This class is provided by an optional library that delegates to
    Play Services.
    *   **What's wrong?** This is another situation where the library is
        limiting developer choice. Features in primary artifacts which may
        delegate to proprietary services must allow developers to choose a
        different delegate. Reflection on a fully-qualified class name does
        *not* allow multiple delegates to exist on the classpath and is not a
        suitable service discovery mechanism.
    *   **How to fix?** This library should use a more suitable service
        discovery mechanism that allows multiple providers to coexist and
        ensures the developer is able to choose among them.
*   A primary artifact provides a service discovery mechanism that allows
    multiple providers and exposes an API that lets the developer specify a
    preference. Communication with the service is managed through a `Bundle`
    where they keys, values, and behaviors are documented outside of Jetpack.
    *   **What's wrong?** This conflicts with the open protocols principle.
        Third-party developers should be able to implement their own backing
        services, but using a `Bundle` with a privately-documented protocol
        means that (1) it is not possible to write adqeuate tests in Jetpack and
        (2) developers outside of Google cannot feasibly write correct backing
        implementations.
    *   **How to fix?** At a minimum, the developer should fully document the
        keys, values, and behavior expected by the protocol; however, in this
        case we would strongly recommend replacing or wrapping `Bundle` with a
        strongly-typed and documented API surface and robust suite of tests to
        ensure implementations on either side of the protocol are behaving
        correctly.
*   A primary artifact provides an `interface` and an API that allows developers
    to specify a backing service using classes that implement that interface.
    The `interface` API surface has several `@hide` methods annotated with
    `@RestrictTo(LIBRARY_GROUP)`.
    *   **What's wrong?** This is another open protocols issue. Third-party
        developers should be able to implement their own backing services, but
        using a partially-private `interface` means that only Jetpack libraries
        can feasibly provide a backing implementation.
    *   **How to fix?** At a minimum, the developer should make the `interface`
        fully public and documented so that it can be implemented by a
        third-party. They should also provide robust tests for the default
        backing implementation with the expectation that third-party developers
        will use this to verify their own custom implementations.
