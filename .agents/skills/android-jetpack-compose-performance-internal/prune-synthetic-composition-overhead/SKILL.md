---
name: prune-synthetic-composition-overhead
description: >-
  Guides deferring or pruning synthetic Compose composition overhead such as
  unconditional LaunchedEffect or CompositionLocal calls when callbacks or
  dependencies are null or optional. Distinguishes SideEffect vs LaunchedEffect.
---

# Defer or Prune Synthetic Composition Overhead

Do not yield synthetic framework constructs like `LaunchedEffect` or custom
`CompositionLocal` providers unless explicitly required by active callbacks or
non-null parameters.

## Why It Matters

Invoking side-effect primitives like `LaunchedEffect` unconditionally spawns
internal coroutine state tracking, effect nodes, and composition structures even
when the target callback is `null` or unneeded.

Guarding side-effects with explicit `null` checks before invoking Compose effect
primitives eliminates unnecessary runtime overhead and avoids thread context
switching.

## SideEffect vs LaunchedEffect Selection

-   **`SideEffect`**: Use for synchronous updates that must run on every
    successful recomposition (such as updating non-Compose state or external
    listeners).

-   **`LaunchedEffect`**: Use for asynchronous suspend functions tied to
    composition. Always guard `LaunchedEffect` with a non-null key or explicit
    `if (callback != null)` check to avoid allocating unnecessary coroutine jobs
    when inactive.

-   **Event Handlers**: Coroutines launched inside event callbacks (e.g.
    `onClick`) via `coroutineScope.launch` execute asynchronously outside the
    frame rendering pipeline and do not bloat composition overhead.

## Guidelines

-   Check if optional callbacks (e.g. `onSelectionManagerCreated`) are `null`
    before wrapping them in `LaunchedEffect`.

-   Prefer `SideEffect` for synchronous non-suspending state synchronization.

-   Only instantiate `CompositionLocalProvider` or side-effect blocks when child
    nodes or consumers actually require them.

## Anti-Pattern (Before)

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@Composable
fun SelectionContainer(onSelectionManagerCreated: (() -> Unit)? = null) {
    // Anti-pattern: Spawns Compose side-effect machinery even when callback is absent
    LaunchedEffect(Unit) {
        onSelectionManagerCreated?.invoke()
    }
}
```

## Optimization (After)

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect

@Composable
fun SelectionContainer(
    onSelectionManagerCreated: (() -> Unit)? = null,
    externalState: String? = null
) {
    // Optimization: Avoid setting up coroutine/effect tracking when not required
    if (onSelectionManagerCreated != null) {
        LaunchedEffect(onSelectionManagerCreated) {
            onSelectionManagerCreated.invoke()
        }
    }

    // Optimization: Synchronous state sync uses SideEffect directly
    if (externalState != null) {
        SideEffect {
            syncExternalState(externalState)
        }
    }
}

fun syncExternalState(state: String) {}
```
