# Headless composition host

Everything a RemoteCompose capture needs to run a `Recomposer` without a UI, and nothing that is
specific to RemoteCompose. The package must not import `androidx.compose.remote.*`, so that it can
be lifted into a library shared with the other
headless composition hosts in AndroidX (`androidx.glance.session`, `androidx.appstate.transform`,
Compose UI's `GlobalSnapshotManager`), each of which currently carries its own copy of parts of
this.

| File | Role |
| --- | --- |
| `HeadlessFrameClock.kt` | `MonotonicFrameClock` with a single pump coroutine, frame counting, a frame budget and optional pacing. Modelled on `androidx.appstate.transform.HeadlessTransformClock`. |
| `RecomposerContext.kt` | Derives a single-threaded dispatcher for the recomposer from the caller's context. |
| `SnapshotWriteMonitor.kt` | Ref-counted, process-wide coalescing of global snapshot writes into `Snapshot.sendApplyNotifications()`. |
| `RecomposerQuiescence.kt` | `awaitRecomposerQuiescence`: drives the recomposer and frame clock until the composition has settled, bounded by frames and iterations. Also hosts the workaround for `Recomposer.currentState` staying `PendingWork` after frame-end callbacks. |
| `CaptureWriteTracker.kt` | Tracks a session's own global writes until an apply notification delivers them, covering writes claimed by another thread's `MutableSnapshot.apply()`. |
| `RenderInvalidationTracker.kt` | Decides when a post-composition render phase must run again, using `SnapshotStateObserver` for the states it read; `withRenderSnapshot` isolates the render's writes. |
| `HeadlessSession.kt` | Wires dispatcher, recomposer, write tracker and frame clock together; `start()`, `awaitRunning()`, `awaitQuiescence()`, `invalidations`, `close()`. |
| `RenderLoop.kt` | `renderLoop`: settle, render through `renderIfChanged` (skips unchanged output, observes render reads), wait for the next apply notification; the streaming driver shared by hosts. |
| `HeadlessRenderFlow.kt` | `headlessRenderFlow`: the whole lifecycle of a host that turns a composition into a `Flow` of outputs (session, setting content, settling, re-rendering on change, rate limiting, skipping equal outputs, teardown). A host supplies its `Applier`, its content and a render step. |
| `UpdateRateLimiter.kt` | Token bucket limiting how often outputs are emitted, with a rate-limited report hook for logging. |
| `HeadlessLifecycleOwner.kt` | Thread-agnostic `LifecycleOwner` + `SavedStateRegistryOwner` for compositions that run off the main thread. |

The pieces that stay RemoteCompose-specific live in the parent `capture` package: the public
`captureRemoteDocument` / `captureSingleRemoteDocument` functions, which call `headlessRenderFlow`
with the `RemoteComposeApplier` and `RemoteRootNode`, the RemoteCompose `CompositionLocal`s and the
document render and encode; the single-capture failure messages; and `CaptureUpdateThrottle` (a
public API that creates an `UpdateRateLimiter` and owns its log message).

Android-only pieces (`HeadlessLifecycleOwner`, `SavedState`) would become a separate `-android`
layer in an extracted multiplatform library; `ThreadLocal` and `java.util.concurrent` usages would
need `expect`/`actual` counterparts.
