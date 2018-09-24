package androidx.ui.engine.window

/**
 * States that an application can be in.
 *
 * The values below describe notifications from the operating system.
 * Applications should not expect to always receive all possible
 * notifications. For example, if the users pulls out the battery from the
 * device, no notification will be sent before the application is suddenly
 * terminated, along with the rest of the operating system.
 *
 * See also:
 *
 *  * [WidgetsBindingObserver], for a mechanism to observe the lifecycle state
 *    from the widgets layer.
 */
enum class AppLifecycleState {
    /** The application is visible and responding to user input. */
    RESUMED,

    /**
     * The application is in an inactive state and is not receiving user input.
     *
     * On iOS, this state corresponds to an app or the Flutter host view running
     * in the foreground inactive state. Apps transition to this state when in
     * a phone call, responding to a TouchID request, when entering the app
     * switcher or the control center, or when the UIViewController hosting the
     * Flutter app is transitioning.
     *
     * On Android, this corresponds to an app or the Flutter host view running
     * in the foreground inactive state.  Apps transition to this state when
     * another activity is focused, such as a split-screen app, a phone call,
     * a picture-in-picture app, a system dialog, or another window.
     *
     * Apps in this state should assume that they may be [paused] at any time.
     */
    INACTIVE,

    /**
     * The application is not currently visible to the user, not responding to
     * user input, and running in the background.
     *
     * When the application is in this state, the engine will not call the
     * [Window.onBeginFrame] and [Window.onDrawFrame] callbacks.
     *
     * Android apps in this state should assume that they may enter the
     * [suspending] state at any time.
     */
    PAUSED,

    /**
     * The application will be suspended momentarily.
     *
     * When the application is in this state, the engine will not call the
     * [Window.onBeginFrame] and [Window.onDrawFrame] callbacks.
     *
     * On iOS, this state is currently unused.
     */
    SUSPENDING,
}