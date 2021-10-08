# Jetpack Principles

[TOC]

## Ethos of Jetpack

To create components, tools, and guidance that makes it quick and easy to build
great Android apps, including contributions from Google and the open-source
community.

## Core Principles of a Jetpack Library

Jetpack libraries provide the following guarantees to Android Developers:

_formatted as “Jetpack libraries are…” with sub-points “Libraries should…”_

### 1. Optimized for external client adoption

-   Libraries should work for first-party clients and may even have optional
    modules tailored specifically to first-party needs, but primary
    functionality should target external developers.
-   Measure success by 3p client adoption, followed by 1p client adoption.

### 2. Designed to satisfy real-world use cases

-   Meet developers where they are and solve the problems that they have
    building apps -- not designed to just provide parity with existing platform
    APIs and features
-   Expose modules that are tightly-scoped to **developer pain points**
    -   Smaller building blocks for external developers by scoping disjoint use
        cases that are likely not to co-exist in a single app to individual
        modules.
-   Implement layered complexity, with **simple top-level APIs**
    -   Complicated use case support must not be at the expense of increasing
        API complexity for the most common simpler use cases.
-   Have **backing data or a researched hypothesis** (research, demand etc) to
    prove the library is necessary and sufficient.

### 3. Aware of the existing developer ecosystem

-   Avoid reinventing the wheel -- do not create a new library where one already
    exists that is accepted by the community as a best practice

### 4. Consistent with the rest of Jetpack

-   Ensure that concepts learned in one component can be seen and understood in
    other components
-   Leverage Jetpack and community standards, for example:
    -   For async work, uses Kotlin coroutines and/or Kotlin flow
    -   For data persistence, uses Jetpack DataStore for simple and small data
        and uses Room for more complicated Data

### 5. Developed as open-source and compatible with AOSP Android

-   Expose a unified developer-facing API surface across the Android ecosystem
-   Develop in AOSP to provide visibility into new features and bug fixes and
    encourage external participation
-   Avoid proprietary services or closed-source libraries for core
    functionality, and instead provide integration points that allow a developer
    to choose between a variety of services as the backing implementation
-   See [Integrating proprietary components](open_source.md) for guidance on
    using closed-source and proprietary libraries and services

### 6. Written using language-idiomatic APIs

-   Write APIs that feel natural for clients using both
    [Kotlin](https://developer.android.com/kotlin/interop) and Java

### 7. Compatible with a broad range of API levels

-   Support older platforms and API levels according to client needs
-   Provide continued maintenance to ensure compatibility with newer platforms
-   Design with the expectation that every Jetpack API is **write-once,
    run-everywhere** for Android with graceful degradation where necessary

### 8. Integrated with best practices

-   Guide developers toward using existing Jetpack best-practice libraries,
    including Architecture Components

### 9. Designed for tooling and testability

-   Write adequate unit and integration tests for the library itself
-   Provide developers with an accompanying testing story for integration
    testing their own applications (ex. -testing artifacts that some libraries
    expose)
    -   Robolectric shouldn’t need to shadow the library classes
    -   Ex. Room has in-memory testing support
-   Build tooling concurrent with the library when possible, and with tooling in
    mind otherwise

### 10. Released using a clearly-defined process

-   Follow Semantic Versioning and pre-release revision guidelines where each
    library moves through alpha, beta, and rc revisions to gain feedback and
    ensure stability

### 11. Well-documented

-   Provide developers with getting started and use case documentation on
    d.android.com in addition to clear API reference documentation

### 12. Supported for long-term use

-   Plan for long-term support and maintenance

### 13. Examples of modern development

-   Where possible, targeting the latest languages, OS features, and tools. All
    new libraries should be written in Kotlin first. Existing libraries
    implemented in Java should add Kotlin extension libraries to improve the
    interoperability of the Java APIs from Kotlin. New libraries written in Java
    require a significant business reason on why a dependency in Kotlin cannot
    be taken. The following is the order of preference, with each lower tier
    requiring a business reason:
    1.  Implemented in Kotlin that compiles to Java 8 bytecode
    2.  Implemented in Java 8, with `-ktx` extensions for Kotlin
        interoperability
    3.  Implemented in Java 7, with `-ktx` extensions for Kotlin
        interoperability

### 14. High quality APIs and ownership

-   All Jetpack libraries are expected to abide by Android and Jetpack API
    Guidelines

## Target Audience

Jetpack libraries are used by a wide variety of external developers, from
individuals working on their first Android app to huge corporations developing
large-scale production apps. Generally, however, Jetpack libraries are designed
to focus on small- to medium-sized app development teams.

-   Note: If the library targets a niche set of apps, the developer pain
    point(s) addressed by the Jetpack library must be significant enough to
    justify its need.
    -   Example : Jetpack Enterprise library
