package androidx.ui.widgets.framework

/// Tracks the lifecycle of [State] objects when asserts are enabled.
enum class _StateLifecycle {
    /// The [State] object has been created. [State.initState] is called at this
    /// time.
    created,

    /// The [State.initState] method has been called but the [State] object is
    /// not yet ready to build. [State.didChangeDependencies] is called at this time.
    initialized,

    /// The [State] object is ready to build and [State.dispose] has not yet been
    /// called.
    ready,

    /// The [State.dispose] method has been called and the [State] object is
    /// no longer able to build.
    defunct
}