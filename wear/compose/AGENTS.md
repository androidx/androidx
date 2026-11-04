# Project: Wear Compose

This directory contains Jetpack Compose libraries for Wear OS
(`compose-foundation`, `compose-material`, `compose-material-core`,
`compose-material3`, `compose-navigation`, `compose-navigation3`). Also follow
[`../../compose/AGENTS.md`](../../compose/AGENTS.md).

`remote` is for Remote Compose, a separate library that these rules are less
applicable to; see
[`../../compose/remote/AGENTS.md`](../../compose/remote/AGENTS.md).

## Testing Conventions

*   When a component already has a `*Test.kt` file under `src/androidTest/`, add
    new tests to it instead of creating a parallel test file under `src/test/`,
    unless there is a strong reason.
*   Use one flat test class per file (no `BaseTest` inheritance); prefix test
    names by variant (e.g., `slc_`, `tlc_`).
*   Separate Setup, Action, and Verification blocks with a blank line.
*   Verify regression tests fail without the fix, and test state mutations after
    initial composition to exercise `ModifierNodeElement.update()`.
*   Do not add `*_sample_builds` tests; call `@Sampled` functions from the
    `integration-tests` demos instead.

## Public API & Overload Evolution

*   When adding a parameter, prefer keeping the existing overload un-deprecated
    and forwarding it to a new overload where the new parameter is mandatory
    (placed before `modifier`, avoiding `@Deprecated(HIDDEN)` and
    `current.ignore`).
*   File ordering: `public` declarations at the top (primary overload first for
    DAC docs), `*Defaults` below composables, and `internal`/`private` at the
    bottom.
*   Strip redundant component or `Default` prefixes on single-variant
    `*Defaults` members (e.g., `colors()`, not `durationPickerColors()`).
*   Use `@RememberInComposition` on state constructors instead of trivial
    `rememberFooState()` functions.
*   Default color parameters to `Color.Unspecified` and resolve with
    `.takeOrElse {}`.

## Samples, Demos, & KDoc

*   Every new public API needs a concise `@Sampled` function in `samples/`
    called directly from `compose-material3/integration-tests` (for M3) or
    `integration-tests/demos`. Use `TransformingLazyColumn` (not
    `ScalingLazyColumn`) in M3 samples.
*   Put a 1-line intro (`"Example of ...:"`) above every `@sample` tag.

## General

*   On `ModifierNodeElement`, include all fields in `equals`, `hashCode`, and
    `inspectableProperties` (where possible), in the same order as the
    constructor.
