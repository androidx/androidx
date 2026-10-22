/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.wear.compose.material3

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.annotation.MainThread
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.TraversableNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.findNearestAncestor
import androidx.compose.ui.node.observeReads
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.utils.WearApiVersionHelper
import com.google.wear.settings.WearSettings
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Suppresses the status bar and collapses top status bar inset padding to 0.dp.
 *
 * In cases where the system status bar overlay is not yet supported by the platform, the fallback
 * [TimeText] will also be suppressed for consistency. Outside of a scaffold (such as in a
 * standalone dialog or overlay window), hides the status bar directly on the current window and
 * restores the initial visibility state when leaving composition or becoming inactive.
 *
 * For design guidance and best practices on status bar suppression and immersive experiences on
 * Wear OS, see the [Status Bar](https://developer.android.com/training/wearables/status-bar) guide.
 *
 * **Layout-Phase Usage:** When using [ScreenScaffold], pass `contentPadding` directly to containers
 * like TransformingLazyColumn as content padding or to layout modifiers (such as
 * `Modifier.padding(contentPadding)`) rather than inspecting `contentPadding.calculateTopPadding()`
 * directly during the composition phase. Inspecting padding values during composition before the
 * first layout pass can observe initial unsuppressed insets before suppression nodes are fully
 * attached, whereas layout modifiers evaluate insets during measure/layout where suppression state
 * is fully resolved:
 * ```
 * // Recommended (evaluated in layout phase after child nodes attach):
 * ScreenScaffold { contentPadding ->
 *     Box(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
 *         DatePicker() // Suppresses status bar internally
 *     }
 * }
 *
 * // Avoid (eager composition read before child nodes attach):
 * ScreenScaffold { contentPadding ->
 *     val topPadding = contentPadding.calculateTopPadding()
 *     DatePicker() // Suppresses status bar internally
 * }
 * ```
 *
 * Example of status bar suppression for an individual component or overlay within a screen (such as
 * a picker or media player):
 *
 * @sample androidx.wear.compose.material3.samples.ComponentSuppressStatusBarSample
 *
 * Example of status bar suppression if only a specific screen in your app should be full-screen /
 * immersive:
 *
 * @sample androidx.wear.compose.material3.samples.ScreenScaffoldSuppressStatusBarSample
 *
 * Example of status bar suppression if the entire application (or Activity) is an immersive
 * full-screen experience across all screens - this is not recommended for most apps because users
 * will lose access to the time and important status indicators on every screen:
 *
 * @sample androidx.wear.compose.material3.samples.AppScaffoldSuppressStatusBarSample
 */
@Stable public fun Modifier.suppressStatusBar(): Modifier = this then StatusBarSuppressionElement

private object StatusBarSuppressionElement : ModifierNodeElement<StatusBarSuppressionNode>() {
    override fun create(): StatusBarSuppressionNode = StatusBarSuppressionNode()

    override fun update(node: StatusBarSuppressionNode) {}

    override fun InspectorInfo.inspectableProperties() {
        name = "suppressStatusBar"
    }

    override fun hashCode(): Int = System.identityHashCode(this)

    override fun equals(other: Any?): Boolean = (other === this)
}

/**
 * [Modifier.Node] backing [Modifier.suppressStatusBar].
 *
 * Coordinates status bar suppression depending on its hierarchy position:
 * - Inside an [AppScaffold], [ScreenScaffold], or pager scaffold, discovers the nearest ancestor
 *   [StatusBarCoordinatorModifierNode] via [TraversableNode] traversal, incrementing suppression
 *   count on attach and decrementing on detach.
 * - Outside of any scaffold (e.g. standalone dialog windows or tests), falls back to coordinating
 *   the status bar directly on the host window [View] via [StandaloneStatusBarCoordinator], hiding
 *   the status bar while [LocalScreenIsActive] is true and ref-counting across concurrent
 *   standalone callers to preserve the window's initial status bar baseline.
 */
private class StatusBarSuppressionNode :
    Modifier.Node(), CompositionLocalConsumerModifierNode, ObserverModifierNode {

    private var releaseSuppression: (() -> Unit)? = null

    override fun onAttach() {
        updateSuppression()
    }

    override fun onObservedReadsChanged() {
        updateSuppression()
    }

    private fun updateSuppression() {
        releaseSuppression?.invoke()
        releaseSuppression = null

        // Walk up the LayoutNode hierarchy looking for the nearest scaffold coordinator.
        val coordinator =
            findNearestAncestor(StatusBarCoordinatorTraverseKey)
                as? StatusBarCoordinatorModifierNode
        if (coordinator != null) {
            // Inside a scaffold: coordinate suppression with the scaffold via ref-counting.
            // The scaffold will hide the status bar, silence fallback time text, and
            // collapse top padding.
            releaseSuppression = coordinator.attach()
        } else {
            observeReads {
                val isEnabled = currentValueOf(LocalStatusBarEnabled)
                val isActive = currentValueOf(LocalScreenIsActive)
                val view = currentValueOf(LocalView)
                if (isEnabled && isActive) {
                    val standalone = StandaloneStatusBarCoordinator.getOrCreate(view)
                    standalone.acquireSuppression()
                    releaseSuppression = standalone::releaseSuppression
                }
            }
        }
    }

    override fun onReset() {
        super.onReset()
        releaseSuppression?.invoke()
        releaseSuppression = null
    }

    override fun onDetach() {
        releaseSuppression?.invoke()
        releaseSuppression = null
    }
}

private const val StatusBarCoordinatorTraverseKey =
    "androidx.wear.compose.material3.StatusBarCoordinator"

internal fun Modifier.statusBarCoordinator(suppressionCount: MutableIntState?): Modifier =
    this then StatusBarCoordinatorElement(suppressionCount)

internal data class StatusBarCoordinatorElement(val suppressionCount: MutableIntState?) :
    ModifierNodeElement<StatusBarCoordinatorModifierNode>() {
    override fun create(): StatusBarCoordinatorModifierNode =
        StatusBarCoordinatorModifierNode(suppressionCount)

    override fun update(node: StatusBarCoordinatorModifierNode) {
        node.suppressionCount = suppressionCount
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "statusBarCoordinator"
    }
}

/**
 * Coordinator modifier node attached to scaffold containers ([AppScaffold], [ScreenScaffold], and
 * [PagerScaffoldImpl]) to observe descendant [Modifier.suppressStatusBar] nodes via
 * [TraversableNode] traversal without requiring composition local propagation.
 */
internal class StatusBarCoordinatorModifierNode(var suppressionCount: MutableIntState?) :
    Modifier.Node(), TraversableNode {
    override val traverseKey: Any = StatusBarCoordinatorTraverseKey

    /**
     * Increments the active suppression count and returns a detach lambda that decrements the count
     * when the suppressing node is detached or reset.
     */
    fun attach(): () -> Unit {
        val count = suppressionCount ?: return {}
        count.intValue++
        return { count.intValue = maxOf(0, count.intValue - 1) }
    }
}

/** Encapsulates status bar suppression and status bar visibility state for scaffold components. */
internal class ScaffoldStatusBarState(
    timeText: State<(@Composable () -> Unit)?>,
    unsuppressedStatusBarMode: State<StatusBarMode>,
    isSupported: State<Boolean>,
    parentStatusBarState: State<ScaffoldStatusBarState?>,
) {
    // Ref-counts active Modifier.suppressStatusBar() nodes (both on the scaffold itself and
    // on descendant components like DatePicker, Stepper, etc.) attached during layout.
    private val suppressionCount = mutableIntStateOf(0)

    val isSuppressed: State<Boolean> = derivedStateOf {
        suppressionCount.intValue > 0 || (parentStatusBarState.value?.isSuppressed?.value == true)
    }

    val showStatusBar: State<Boolean> = derivedStateOf {
        isSupported.value &&
            !isSuppressed.value &&
            when (unsuppressedStatusBarMode.value) {
                StatusBarMode.Enabled -> true
                StatusBarMode.Disabled -> false
                else -> parentStatusBarState.value?.showStatusBar?.value ?: false
            }
    }

    // When the status bar is suppressed for this screen or an ancestor scaffold, provide an empty
    // time text composable so that no time text is rendered while this screen is active.
    val timeText: State<(@Composable () -> Unit)?> = derivedStateOf {
        if (isSuppressed.value) EmptyTimeText else timeText.value
    }

    // The coordinator modifier attached to the scaffold's root container. Descendant
    // `Modifier.suppressStatusBar()` nodes discover this via TraversableNode traversal.
    val coordinatorModifier: Modifier = Modifier.statusBarCoordinator(suppressionCount)
}

/**
 * Remembers a unified [ScaffoldStatusBarState] managing suppression count, effective status bar
 * visibility, silenced or inherited time text, and the ancestor [statusBarCoordinator] modifier.
 *
 * @param timeText The screen-level time text composable, or null if inheriting default time text.
 * @param unsuppressedStatusBarMode Resolves the [StatusBarMode] when suppression is not active.
 *   Defaults to [StatusBarMode.Inherit].
 */
@Composable
internal fun rememberScaffoldStatusBarState(
    timeText: (@Composable () -> Unit)? = null,
    unsuppressedStatusBarMode: StatusBarMode = StatusBarMode.Inherit,
): ScaffoldStatusBarState {
    val updatedTimeText = rememberUpdatedState(timeText)
    val unsuppressedModeState = rememberUpdatedState(unsuppressedStatusBarMode)
    val isSupportedState = rememberUpdatedState(LocalStatusBarEnabled.current)
    val parentState = rememberUpdatedState(LocalScaffoldStatusBarState.current)
    return remember {
        ScaffoldStatusBarState(
            timeText = updatedTimeText,
            unsuppressedStatusBarMode = unsuppressedModeState,
            isSupported = isSupportedState,
            parentStatusBarState = parentState,
        )
    }
}

internal val EmptyTimeText: @Composable () -> Unit = {}

/**
 * View-scoped coordinator that ref-counts status bar suppression across standalone components
 * sharing the same [View], preserving the window's original status bar visibility baseline.
 */
@MainThread
internal class StandaloneStatusBarCoordinator
private constructor(private val viewRef: WeakReference<View>) {
    internal val view: View?
        get() = viewRef.get()

    // Underlying orchestrator directly driving WindowInsetsController for this View's window.
    private var orchestrator: StatusBarOrchestrator =
        view?.let { StatusBarOrchestrator(it) } ?: NoOpStatusBarOrchestrator

    // Number of active standalone components currently requesting status bar suppression on this
    // View.
    internal var suppressionCount: Int = 0
        private set

    @MainThread
    fun acquireSuppression() {
        suppressionCount++
        // On transition from 0 to 1, this is the first active suppressor on the window:
        // capture initial system baseline visibility and hide the physical status bar.
        if (suppressionCount == 1) {
            orchestrator.hide()
        }
    }

    @MainThread
    fun releaseSuppression() {
        if (suppressionCount > 0) {
            suppressionCount--
            // When all active suppressors have released their hold (count drops back to 0),
            // restore the window's original system baseline visibility.
            if (suppressionCount == 0) {
                orchestrator.restoreInitialStatusBarState()
            }
        }
    }

    @MainThread
    fun onAttachedToWindow() {
        val v = view ?: return
        // Dispose existing orchestrator and unregister its listeners before re-creating
        // for the newly attached window hierarchy to capture fresh insets and
        // WindowInsetsController handles.
        orchestrator.dispose()
        orchestrator = StatusBarOrchestrator(v)
        if (suppressionCount > 0) {
            orchestrator.hide()
        }
    }

    @MainThread
    fun onDetachedFromWindow() {
        // Restore window baseline while detached so transient detachments don't leave
        // the status bar permanently hidden, and unregister view listeners to prevent leaks.
        if (suppressionCount > 0) {
            orchestrator.restoreInitialStatusBarState()
        }
        orchestrator.dispose()
    }

    companion object {
        // Registry of active coordinators keyed by View.
        private val coordinators = WeakHashMap<View, StandaloneStatusBarCoordinator>()

        @MainThread
        fun getOrCreate(view: View): StandaloneStatusBarCoordinator =
            synchronized(coordinators) {
                coordinators.getOrPut(view) {
                    val coordinator = StandaloneStatusBarCoordinator(WeakReference(view))
                    // Listen for window attachment/detachment to restore status bar baselines
                    // and re-apply suppression across view re-attachments (e.g. view recycling,
                    // fragment transactions).
                    view.addOnAttachStateChangeListener(
                        object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(v: View) {
                                coordinator.onAttachedToWindow()
                            }

                            override fun onViewDetachedFromWindow(v: View) {
                                coordinator.onDetachedFromWindow()
                            }
                        }
                    )
                    coordinator
                }
            }
    }
}

/**
 * Drives window status bar suppression for standalone containers (such as [ScreenScaffold] or
 * [PagerScaffoldImpl] rendered without an enclosing [AppScaffold]).
 */
@Composable
internal fun StandaloneStatusBarSuppression(isSuppressed: Boolean) {
    if (LocalStatusBarEnabled.current) {
        val screenIsActive = LocalScreenIsActive.current
        val view = LocalView.current
        // Retrieve or instantiate the shared view-scoped coordinator for this window.
        val coordinator = remember(view) { StandaloneStatusBarCoordinator.getOrCreate(view) }

        DisposableEffect(screenIsActive, isSuppressed, coordinator) {
            val shouldSuppress = screenIsActive && isSuppressed
            if (shouldSuppress) {
                // Screen is active in the foreground and requests suppression: increment ref-count.
                coordinator.acquireSuppression()
            }
            onDispose {
                if (shouldSuppress) {
                    // Screen left composition or became unsuppressed: decrement ref-count.
                    // If no other standalone components are suppressing, this restores the window
                    // status bar.
                    coordinator.releaseSuppression()
                }
            }
        }
    }
}

/**
 * Synchronizes status bar registration for scaffolds ([ScreenScaffold] and [PagerScaffoldImpl]).
 *
 * When enclosed within an [AppScaffold], delegates status bar visibility to the parent scaffold's
 * screen content layer across screen transitions.
 *
 * When rendered standalone (no enclosing [AppScaffold]), the scaffold's
 * [StatusBarCoordinatorModifierNode] intercepts descendant [Modifier.suppressStatusBar] nodes,
 * which prevents them from falling back to standalone window orchestration. This helper coordinates
 * the physical window status bar directly via [StandaloneStatusBarSuppression] so suppression
 * properly hides the physical bar and coordinates across multiple standalone components on the same
 * window.
 */
@Composable
internal fun ScaffoldStatusBarRegistration(
    scaffoldState: ScaffoldState?,
    statusBarState: ScaffoldStatusBarState,
) {
    if (scaffoldState is AppScaffoldState) {
        StatusBarRegistration(
            showStatusBar = statusBarState.showStatusBar,
            scaffoldState = scaffoldState,
        )
    } else {
        StandaloneStatusBarSuppression(isSuppressed = statusBarState.isSuppressed.value)
    }
}

/** [CompositionLocal] to determine if the system status bar is enabled on the device. */
internal val LocalStatusBarEnabled: CompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        isStatusBarEnabled(LocalContext.currentValue.applicationContext)
    }

/** Represents the status bar display mode for a screen within [ScreenScaffold]. */
@Immutable
@JvmInline
internal value class StatusBarMode internal constructor(internal val value: Int) {
    internal companion object {
        /**
         * Inherits the status bar mode from the underlying screen in the screen stack, or falls
         * back to the [AppScaffold] setting if no screen in the stack specifies a mode.
         */
        internal val Inherit: StatusBarMode = StatusBarMode(0)

        /** Explicitly enables the system status bar overlay for this screen. */
        internal val Enabled: StatusBarMode = StatusBarMode(1)

        /** Explicitly disables the system status bar overlay for this screen. */
        internal val Disabled: StatusBarMode = StatusBarMode(2)
    }

    override fun toString(): String =
        when (this) {
            Inherit -> "Inherit"
            Enabled -> "Enabled"
            Disabled -> "Disabled"
            else -> "Unknown"
        }
}

/**
 * CompositionLocal providing the enclosing scaffold's [ScaffoldStatusBarState].
 *
 * Used by [ScreenScaffold] and pager scaffolds to inherit status bar suppression and status bar
 * visibility from an enclosing [AppScaffold] or pager scaffold.
 */
internal val LocalScaffoldStatusBarState: ProvidableCompositionLocal<ScaffoldStatusBarState?> =
    compositionLocalOf {
        null
    }

private const val TAG = "StatusBar"
// TODO(b/548550572): Replace with WearApiVersionHelper.WEAR_CINNAMON_BUN_2 once wear-core
// releases with it.
private const val WEAR_CINNAMON_BUN_2 = "WEAR_CINNAMON_BUN_2"

/** Checks if the global status bar is enabled for the given application [Context]. */
internal fun isStatusBarEnabled(context: Context): Boolean =
    try {
        context.applicationInfo.targetSdkVersion >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            WearApiVersionHelper.isApiVersionAtLeast(WEAR_CINNAMON_BUN_2) &&
            WearSettings.isStatusBarEnabled(context)
    } catch (e: Throwable) {
        false
    }

/**
 * Orchestrates calls directly to the view's window insets controller for managing status bar
 * visibility.
 *
 * This class tracks the initial status bar visibility prior to any scaffold-driven modifications
 * (e.g. scroll-away or hiding) and restores that initial state when the scaffold is disposed.
 */
internal interface StatusBarOrchestrator {
    /** Requests the status bar overlay to be displayed. */
    fun show()

    /** Requests the status bar overlay to be hidden. */
    fun hide()

    /**
     * Restores the status bar visibility back to the initial state prior to scaffold modifications.
     */
    fun restoreInitialStatusBarState()

    /** Cleans up all active view listeners without mutating window insets. */
    fun dispose()

    /** Determines whether this orchestrator manages the window of [view]. */
    fun isForWindow(view: View): Boolean
}

internal fun StatusBarOrchestrator(view: View): StatusBarOrchestrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        StatusBarOrchestratorImpl(view)
    } else {
        NoOpStatusBarOrchestrator
    }

internal object NoOpStatusBarOrchestrator : StatusBarOrchestrator {
    override fun show() {}

    override fun hide() {}

    override fun restoreInitialStatusBarState() {}

    override fun dispose() {}

    override fun isForWindow(view: View): Boolean = false
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
private class StatusBarOrchestratorImpl(view: View) : StatusBarOrchestrator {
    private val viewRef: WeakReference<View> = WeakReference(view)
    private val view: View?
        get() = viewRef.get()

    /**
     * The original system status bar visibility state prior to any scaffold-driven modifications.
     *
     * Captured once from [View.getRootWindowInsets] as soon as root insets become available on the
     * host [view]. This value remains fixed once captured and serves as the baseline target
     * restored during [restoreInitialStatusBarState] when the scaffold leaves composition.
     */
    private var initialStatusBarVisibility: Boolean? = null

    /**
     * The target status bar visibility requested by Compose (true for show, false for hide).
     * Applied once the host view is attached and baseline initial status bar visibility is secured.
     */
    private var desiredStatusBarVisibility: Boolean? = null

    /**
     * Tracks whether this orchestrator has actually dispatched a status bar visibility mutation.
     */
    private var hasMutatedStatusBar: Boolean = false

    /**
     * Listener attached to [view] when initialized before window attachment.
     *
     * Lifecycle & Management:
     * - Registered via [View.addOnAttachStateChangeListener] in `init` if [View.isAttachedToWindow]
     *   is `false`.
     * - When [View.OnAttachStateChangeListener.onViewAttachedToWindow] is invoked, it calls
     *   [attemptCaptureAndApply].
     * - Automatically unregistered and set to `null` via [clearListeners] once
     *   [isStatusBarBaselineCapturedAndControllerReady] becomes `true` (meaning both initial
     *   baseline status bar visibility is captured and [View.getWindowInsetsController] is bound).
     * - Also explicitly unregistered during [restoreInitialStatusBarState] if disposal occurs prior
     *   to window attachment.
     */
    private var onAttachStateChangeListener: View.OnAttachStateChangeListener? = null

    /**
     * Listener attached to [view] to observe layout passes for asynchronous insets and controller
     * availability.
     *
     * Lifecycle & Management:
     * - Registered via [View.addOnLayoutChangeListener] in `init` if
     *   [isStatusBarBaselineCapturedAndControllerReady] is `false`.
     * - Window insets and [View.getWindowInsetsController] handles are often dispatched
     *   asynchronously during initial layout passes right after window attachment.
     * - On every [View.OnLayoutChangeListener.onLayoutChange] callback, it calls
     *   [attemptCaptureAndApply] to re-evaluate baseline capture and apply any pending
     *   [desiredStatusBarVisibility].
     * - Retained across layout passes until [isStatusBarBaselineCapturedAndControllerReady]
     *   evaluates to `true`, ensuring that visibility requests are never lost if
     *   [View.getWindowInsetsController] is `null` on early layout passes.
     * - Automatically unregistered and set to `null` via [clearListeners] once
     *   [isStatusBarBaselineCapturedAndControllerReady] becomes `true`, or explicitly unregistered
     *   during [restoreInitialStatusBarState].
     */
    private var onLayoutChangeListener: View.OnLayoutChangeListener? = null

    /**
     * Determines whether the orchestrator has both captured the initial system baseline status bar
     * visibility and verified that the [View.getWindowInsetsController] is available to apply
     * mutations.
     *
     * View listeners ([onAttachStateChangeListener] and [onLayoutChangeListener]) remain active
     * until this evaluates to `true`, guaranteeing that late controller binding or delayed insets
     * dispatch will not drop visibility updates.
     */
    private val isStatusBarBaselineCapturedAndControllerReady: Boolean
        get() = initialStatusBarVisibility != null && view?.windowInsetsController != null

    init {
        // Attempt initial capture and application immediately in case the view is already attached
        // with insets available.
        attemptCaptureAndApply()

        // If initial status bar baseline is not captured OR WindowInsetsController is not ready,
        // attach listeners to observe window attachment and layout passes until both requirements
        // are satisfied.
        if (!isStatusBarBaselineCapturedAndControllerReady) {
            // 1. Observe window attachment if the view is unattached during initial composition.
            if (!view.isAttachedToWindow) {
                val windowAttachListener =
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            // On window attachment, retry capturing status bar baseline and
                            // applying requested visibility.
                            attemptCaptureAndApply()
                        }

                        override fun onViewDetachedFromWindow(v: View) {}
                    }
                onAttachStateChangeListener = windowAttachListener
                view.addOnAttachStateChangeListener(windowAttachListener)
            }

            // 2. Window insets and WindowInsetsController handles can be dispatched asynchronously
            // during layout passes.
            // Observe layout passes to capture unmodified system status bar insets as soon as they
            // arrive.
            val windowLayoutListener =
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        // On layout pass, retry capturing status bar baseline and applying
                        // requested visibility.
                        attemptCaptureAndApply()
                    }
                }
            onLayoutChangeListener = windowLayoutListener
            view.addOnLayoutChangeListener(windowLayoutListener)
        }
    }

    private fun clearListeners() {
        val v = view
        onAttachStateChangeListener?.let {
            v?.removeOnAttachStateChangeListener(it)
            onAttachStateChangeListener = null
        }
        onLayoutChangeListener?.let {
            v?.removeOnLayoutChangeListener(it)
            onLayoutChangeListener = null
        }
    }

    /**
     * Disposes this orchestrator by unregistering all active view listeners
     * ([onAttachStateChangeListener] and [onLayoutChangeListener]) to prevent memory leaks, without
     * mutating window insets.
     */
    override fun dispose() {
        clearListeners()
        viewRef.clear()
    }

    override fun show() {
        // Store requested visibility (show) and attempt to apply it to the status bar.
        desiredStatusBarVisibility = true
        attemptCaptureAndApply()
    }

    override fun hide() {
        // Store requested visibility (hide) and attempt to apply it to the status bar.
        desiredStatusBarVisibility = false
        attemptCaptureAndApply()
    }

    /**
     * Restores the status bar visibility back to the initial system state captured prior to any
     * scaffold modifications.
     *
     * Lifecycle & Cleanup Operations:
     * 1. **Listener Cleanup**: Immediately unregisters any active [onAttachStateChangeListener] and
     *    [onLayoutChangeListener] from [view] to prevent memory leaks and prevent asynchronous view
     *    callbacks from executing after disposal.
     * 2. **Baseline State Restoration**: Reads [initialStatusBarVisibility], which holds the
     *    unmodified system status bar state captured when the view first received window insets. If
     *    [initialStatusBarVisibility] was never captured (e.g. if the view was never attached to a
     *    window before disposal), no status bar mutation is attempted.
     * 3. **Window Mutation & Exception Handling**: Calls [android.view.WindowInsetsController.show]
     *    or [android.view.WindowInsetsController.hide] on [View.getWindowInsetsController] to
     *    revert window state. Runtime exceptions thrown by framework or OEM controller
     *    implementations are safely caught and logged to prevent app crashes during disposal.
     */
    override fun restoreInitialStatusBarState() {
        // Step 1: Immediately unregister and clear active view listeners to prevent memory leaks or
        // late execution after disposal.
        clearListeners()

        // Clear requested visibility so late layout callbacks or view re-attachments do not
        // re-hide or re-show the status bar unexpectedly.
        desiredStatusBarVisibility = null

        // Step 2: Exit early if no window status bar mutation was actually performed by this
        // orchestrator.
        if (!hasMutatedStatusBar) return

        // Step 3: Read initial system status bar visibility baseline. If baseline was never
        // captured, exit safely without mutation.
        val visible = initialStatusBarVisibility ?: return
        val v = view ?: return

        // Step 4: Revert system status bar visibility back to its original baseline state.
        try {
            if (visible) {
                v.windowInsetsController?.show(WindowInsets.Type.statusBars())
            } else {
                v.windowInsetsController?.hide(WindowInsets.Type.statusBars())
            }
            hasMutatedStatusBar = false
        } catch (e: Exception) {
            // Swallowing exceptions here as WindowInsetsController is a newer API whose
            // platform implementations across OEM hardware may throw unexpected runtime
            // exceptions. We avoid crashing the app due to framework controller issues.
            Log.w(TAG, "Failed to restore status bar", e)
        }
    }

    /**
     * Safely attempts to secure the initial system status bar visibility baseline state, and only
     * if successful, applies the current desired visibility state requested by Compose.
     */
    private fun attemptCaptureAndApply() {
        val v = view ?: return
        // Step 1: Secure initial system status bar visibility baseline state before performing any
        // window modifications.
        if (initialStatusBarVisibility == null) {
            val insets = v.rootWindowInsets
            if (insets != null) {
                initialStatusBarVisibility = insets.isVisible(WindowInsets.Type.statusBars())
            }
        }

        // Step 2: Only mutate status bar visibility if baseline is captured and
        // WindowInsetsController is available.
        if (initialStatusBarVisibility != null) {
            if (isStatusBarBaselineCapturedAndControllerReady) {
                clearListeners()
            }

            val targetVisibility = desiredStatusBarVisibility ?: return
            val controller = v.windowInsetsController ?: return

            try {
                if (targetVisibility) {
                    controller.show(WindowInsets.Type.statusBars())
                } else {
                    controller.hide(WindowInsets.Type.statusBars())
                }
                hasMutatedStatusBar = true
            } catch (e: Exception) {
                // Swallowing exceptions here as WindowInsetsController is a newer API whose
                // platform implementations across OEM hardware may throw unexpected runtime
                // exceptions. We avoid crashing the app due to framework controller issues.
                Log.w(TAG, "Failed to change status bar visibility", e)
            }
        }
    }

    override fun isForWindow(view: View): Boolean = this.view?.isSameWindow(view) ?: false
}

internal fun View.isSameWindow(other: View): Boolean {
    if (this === other) return true
    val ourRoot = this.rootView
    val targetRoot = other.rootView
    return ourRoot != null && targetRoot != null && ourRoot === targetRoot
}
