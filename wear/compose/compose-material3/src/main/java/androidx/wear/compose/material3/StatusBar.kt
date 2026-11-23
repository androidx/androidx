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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.utils.WearApiVersionHelper
import com.google.wear.settings.WearSettings
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/**
 * Suppresses the status bar and collapses top status bar inset padding to 0.dp.
 *
 * Outside of a scaffold (such as in a standalone dialog or overlay window),
 * [Modifier.suppressStatusBar] hides the status bar directly on the current window and restores the
 * initial visibility state when leaving composition or becoming inactive.
 *
 * The system status bar is supported where the target SDK version is at least VANILLA_ICE_CREAM,
 * and the system image is at least CINNAMON_BUN_2. If either of those is not met, the fallback
 * [TimeText] will also be suppressed for consistency.
 *
 * For design guidance and best practices on status bar suppression and immersive experiences on
 * Wear OS, see the [Status Bar](https://developer.android.com/training/wearables/status-bar) guide.
 *
 * **Layout-Phase Usage:** When using [ScreenScaffold], pass `contentPadding` directly to containers
 * like TransformingLazyColumn as content padding or to layout modifiers (such as
 * `Modifier.padding(contentPadding)`). Do not inspect `contentPadding.calculateTopPadding()` or
 * similar padding methods directly during the composition phase, which observes initial
 * unsuppressed insets before suppression nodes are fully attached:
 * ```
 * // Recommended (contentPadding evaluated in layout phase after child nodes attach):
 * ScreenScaffold { contentPadding ->
 *     Box(modifier = Modifier.suppressStatusBar().background(MaterialTheme.colorScheme.background)
 *         .fillMaxSize().padding(contentPadding)) {
 *     }
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

/**
 * Registers an active status bar layer with [AppScaffoldState.screenContent]. Typically used by
 * [ScreenScaffold], [HorizontalPagerScaffold], and [VerticalPagerScaffold] to sync their status bar
 * state with the enclosing [AppScaffoldState].
 *
 * Coordinates status bar lifecycle with the scaffold, attaching [showStatusBar] state handle while
 * active and removing the status bar registration when leaving composition or becoming inactive.
 */
@Composable
internal fun StatusBarRegistration(
    showStatusBar: State<Boolean>,
    scaffoldState: ScaffoldState? = LocalScaffoldState.current,
) {
    val actualScaffoldState = scaffoldState as? AppScaffoldState ?: return
    val screenIsActive = LocalScreenIsActive.current

    val key = remember { Any() }
    val viewState = rememberUpdatedState(LocalView.current)

    DisposableEffect(screenIsActive, actualScaffoldState) {
        if (screenIsActive) {
            actualScaffoldState.screenContent.addStatusBar(
                key = key,
                view = viewState,
                showStatusBar = showStatusBar,
            )
        }
        onDispose { actualScaffoldState.screenContent.removeStatusBar(key) }
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

/** Checks if the global status bar is enabled for the given application [Context]. */
internal fun isStatusBarEnabled(context: Context): Boolean =
    try {
        WearApiVersionHelper.isApiVersionAtLeast(WearApiVersionHelper.WEAR_CINNAMON_BUN_2) &&
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
    /**
     * Updates this caller's desired status bar [visible] state, whether its screen is currently
     * active ([isScreenActive]), and an optional [lifecycle] (e.g. from Compose's
     * `LocalLifecycleOwner.current`) used in preference to the view tree lifecycle.
     */
    fun update(visible: Boolean, isScreenActive: Boolean = true, lifecycle: Lifecycle? = null)

    /** Requests the status bar overlay to be displayed. */
    fun show() = update(visible = true, isScreenActive = true)

    /** Requests the status bar overlay to be hidden. */
    fun hide() = update(visible = false, isScreenActive = true)

    /**
     * Restores the status bar visibility back to the initial state prior to scaffold modifications.
     */
    fun restoreInitialStatusBarState()

    /**
     * Cleans up all active view listeners and unregisters this handle without restoring the
     * window's initial baseline when no handles remain. If sibling handles remain on the shared
     * window, hands off status bar visibility to the next effective handle.
     */
    fun dispose()

    /** Determines whether this orchestrator manages the window of [view]. */
    fun isForWindow(view: View): Boolean
}

internal fun StatusBarOrchestrator(view: View): StatusBarOrchestrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        StatusBarOrchestratorHandle(view)
    } else {
        NoOpStatusBarOrchestrator
    }

internal object NoOpStatusBarOrchestrator : StatusBarOrchestrator {
    override fun update(visible: Boolean, isScreenActive: Boolean, lifecycle: Lifecycle?) {}

    override fun restoreInitialStatusBarState() {}

    override fun dispose() {}

    override fun isForWindow(view: View): Boolean = false
}

/**
 * Lightweight caller handle implementing [StatusBarOrchestrator] for a single scaffold or
 * standalone coordinator.
 *
 * Tracks the caller's [desiredStatusBarVisibility], active/lifecycle state, and view attach/layout
 * listeners, and delegates window baseline capture, precedence resolution, and
 * [android.view.WindowInsetsController] mutations to the shared [WindowStatusBarOrchestrator] for
 * the host window (`DecorView`).
 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
private class StatusBarOrchestratorHandle(initialView: View) : StatusBarOrchestrator {
    private val viewRef: WeakReference<View> = WeakReference(initialView)
    internal val view: View?
        get() = viewRef.get()

    /**
     * Cached reference to the host window's [WindowStatusBarOrchestrator] once resolved, so that
     * [restoreInitialStatusBarState] and [dispose] can still unregister this handle even if Android
     * detaches the [View] before Compose `DisposableEffect.onDispose` runs.
     */
    private var windowOrchestrator: WindowStatusBarOrchestrator? = null

    /**
     * The target status bar visibility requested by this caller (`true` = show, `false` = hide).
     */
    internal var desiredStatusBarVisibility: Boolean? = null

    /** Whether this handle's screen is currently active (via [LocalScreenIsActive]). */
    internal var isScreenActive: Boolean = true

    /**
     * Whether this caller's observed lifecycle (from Compose's `LocalLifecycleOwner.current` when
     * supplied, or the host [View]'s `ViewTreeLifecycleOwner` otherwise) is currently at least
     * [Lifecycle.State.RESUMED] (defaults to `true` if no [androidx.lifecycle.LifecycleOwner] is
     * available).
     *
     * Evaluated dynamically from [observedLifecycle] so that sibling handles sharing a [Lifecycle]
     * never observe stale resumed states while [Lifecycle] is dispatching `ON_PAUSE` or `ON_RESUME`
     * across multiple observers.
     */
    internal val isResumed: Boolean
        get() =
            if (isObservedLifecycleDestroyed) {
                false
            } else {
                observedLifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
            }

    /**
     * Whether this handle is actively in the foreground ([isScreenActive] and at least
     * [Lifecycle.State.RESUMED]). Active handles take precedence over inactive or offscreen
     * `STARTED` handles (such as offscreen Fragments in a `ViewPager2`, Fragments capped at
     * `STARTED` via `FragmentTransaction.setMaxLifecycle`, or background Compose Navigation
     * destinations).
     */
    internal val isActive: Boolean
        get() = isScreenActive && isResumed

    /**
     * Whether this handle has ever been [isActive] while registered on the window. Used to place
     * never-active handles (such as pre-composed offscreen `STARTED` pages in a `ViewPager2`)
     * behind handles that were previously active, while preserving natural registration order among
     * inactive handles.
     */
    internal var hasBeenActive: Boolean = false

    /**
     * Optional [Lifecycle] supplied directly from Compose's `LocalLifecycleOwner.current` (e.g.
     * per-destination `NavBackStackEntry` lifecycles in Compose Navigation), held via
     * [WeakReference] to avoid creating a value-to-key reference cycle in
     * [WindowStatusBarOrchestrator]'s `WeakHashMap`. When `null`, [updateLifecycleObserver] falls
     * back to [View.findViewTreeLifecycleOwner], unless [hasExplicitLifecycle] is `true` (so a
     * cleared/destroyed explicit lifecycle never falls back to a host `ViewTreeLifecycleOwner`).
     */
    private var explicitLifecycleRef: WeakReference<Lifecycle>? = null
    private var hasExplicitLifecycle: Boolean = false
    private val explicitLifecycle: Lifecycle?
        get() = explicitLifecycleRef?.get()

    private var observedLifecycleRef: WeakReference<Lifecycle>? = null
    internal val observedLifecycle: Lifecycle?
        get() = observedLifecycleRef?.get()

    /**
     * Set when the observed [Lifecycle] emits [Lifecycle.Event.ON_DESTROY] (or is already
     * [Lifecycle.State.DESTROYED]), so [isResumed] remains `false` after clearing
     * [observedLifecycleRef] rather than reverting to `true`.
     */
    private var isObservedLifecycleDestroyed: Boolean = false

    /** Defaults to `true` to match [isResumed] when [observedLifecycle] is `null`. */
    private var lastKnownResumed: Boolean = true
    private var isRegisteringLifecycleObserver: Boolean = false
    private val lifecycleObserver =
        object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                if (isRegisteringLifecycleObserver) return
                if (event == Lifecycle.Event.ON_DESTROY) {
                    val destroyedLifecycle = source.lifecycle
                    destroyedLifecycle.removeObserver(this)
                    observedLifecycleRef = null
                    if (explicitLifecycle === destroyedLifecycle) {
                        explicitLifecycleRef = null
                    }
                    isObservedLifecycleDestroyed = true
                    val wasResumed = lastKnownResumed
                    lastKnownResumed = false
                    if (wasResumed) {
                        val v = this@StatusBarOrchestratorHandle.view
                        val orchestrator =
                            windowOrchestrator
                                ?: v?.let { WindowStatusBarOrchestrator.getIfAttached(it) }
                        orchestrator?.onHandleUpdated(this@StatusBarOrchestratorHandle, v)
                    }
                    return
                }
                val nowResumed = source.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
                if (nowResumed != lastKnownResumed) {
                    val wasActive = isScreenActive && lastKnownResumed
                    lastKnownResumed = nowResumed
                    val becameActive = !wasActive && isActive
                    val v = this@StatusBarOrchestratorHandle.view
                    val orchestrator =
                        windowOrchestrator
                            ?: v?.let { WindowStatusBarOrchestrator.getIfAttached(it) }
                    if (becameActive) {
                        orchestrator?.onHandleLifecycleResumed(this@StatusBarOrchestratorHandle, v)
                    } else {
                        orchestrator?.onHandleUpdated(this@StatusBarOrchestratorHandle, v)
                    }
                }
            }
        }

    /**
     * Attached to an initially unattached [View] so this handle retries binding to its host
     * window's [WindowStatusBarOrchestrator] as soon as the view attaches to a window
     * (`DecorView`).
     */
    private var onAttachStateChangeListener: View.OnAttachStateChangeListener? = null

    /**
     * Attached until the host window provides non-null [View.getRootWindowInsets] and
     * [View.getWindowInsetsController], so baseline capture and visibility updates retry on layout
     * and stop listening once [WindowStatusBarOrchestrator.isReady] is `true`.
     */
    private var onLayoutChangeListener: View.OnLayoutChangeListener? = null

    init {
        syncWithWindowOrchestrator()
    }

    /**
     * Registers [onAttachStateChangeListener] (when [v] is not yet attached) and
     * [onLayoutChangeListener] so this handle retries syncing with [WindowStatusBarOrchestrator]
     * once window attachment, root insets, and [android.view.WindowInsetsController] are ready.
     */
    private fun ensureListeners(v: View) {
        if (!v.isAttachedToWindow && onAttachStateChangeListener == null) {
            val windowAttachListener =
                object : View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(attachedView: View) {
                        syncWithWindowOrchestrator()
                    }

                    override fun onViewDetachedFromWindow(detachedView: View) {}
                }
            onAttachStateChangeListener = windowAttachListener
            v.addOnAttachStateChangeListener(windowAttachListener)
        }

        if (onLayoutChangeListener == null) {
            val windowLayoutListener =
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        changedView: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        syncWithWindowOrchestrator()
                    }
                }
            onLayoutChangeListener = windowLayoutListener
            v.addOnLayoutChangeListener(windowLayoutListener)
        }
    }

    /** Unregisters any active attach or layout retry listeners from [view]. */
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
     * Binds [lifecycleObserver] to the resolved [Lifecycle], using [explicitLifecycle] when
     * [hasExplicitLifecycle] is `true` (e.g. per-destination `NavBackStackEntry` lifecycles from
     * Compose's `LocalLifecycleOwner.current`), and falling back to [v]'s `ViewTreeLifecycleOwner`
     * only when [hasExplicitLifecycle] is `false` (non-Compose/standalone callers).
     *
     * Returns `true` if [observedLifecycle] changed to a different [Lifecycle] instance.
     */
    private fun updateLifecycleObserver(v: View): Boolean {
        val lifecycle =
            if (hasExplicitLifecycle) {
                explicitLifecycle
            } else {
                v.findViewTreeLifecycleOwner()?.lifecycle
            }
        val currentObserved = observedLifecycle
        if (lifecycle != null && lifecycle.currentState == Lifecycle.State.DESTROYED) {
            currentObserved?.removeObserver(lifecycleObserver)
            observedLifecycleRef = null
            if (explicitLifecycle === lifecycle) {
                explicitLifecycleRef = null
            }
            if (!isObservedLifecycleDestroyed) {
                isObservedLifecycleDestroyed = true
                lastKnownResumed = false
                return true
            }
            return false
        }
        if (lifecycle !== currentObserved) {
            currentObserved?.removeObserver(lifecycleObserver)
            observedLifecycleRef = lifecycle?.let { WeakReference(it) }
            if (lifecycle != null) {
                isObservedLifecycleDestroyed = false
            }
            lastKnownResumed =
                if (isObservedLifecycleDestroyed) {
                    false
                } else {
                    lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) ?: true
                }
            if (lifecycle != null) {
                isRegisteringLifecycleObserver = true
                try {
                    lifecycle.addObserver(lifecycleObserver)
                } finally {
                    isRegisteringLifecycleObserver = false
                }
            }
            return true
        }
        return false
    }

    /** Unregisters [lifecycleObserver] and resets [lastKnownResumed] to match [isResumed]. */
    private fun clearLifecycleObserver() {
        observedLifecycle?.removeObserver(lifecycleObserver)
        observedLifecycleRef = null
        isObservedLifecycleDestroyed = false
        lastKnownResumed = true
    }

    /**
     * Updates [observedLifecycle] via [updateLifecycleObserver] once a desired visibility is set,
     * binds to the host window's [WindowStatusBarOrchestrator] if attached, and dispatches the
     * appropriate update event.
     *
     * Evaluates `becameActive` (`!wasActiveBeforeSync && isActive`) *after*
     * [updateLifecycleObserver] has switched [observedLifecycle], so callers like [update] that
     * change [explicitLifecycle] and [isScreenActive] together compute active transitions against
     * the newly observed [Lifecycle] before choosing between
     * [WindowStatusBarOrchestrator.onHandleBecameActive],
     * [WindowStatusBarOrchestrator.onHandleLifecycleResumed], and
     * [WindowStatusBarOrchestrator.onHandleUpdated].
     */
    private fun syncWithWindowOrchestrator(
        screenBecameActive: Boolean = false,
        wasActiveBeforeSync: Boolean = isActive,
    ) {
        val v = view ?: return
        // Defer lifecycle observer registration until this handle actually requests a status bar
        // visibility. Avoids premature registration on the host ViewTreeLifecycleOwner during init
        // before AppScaffold calls update() with LocalLifecycleOwner, and prevents leaked observers
        // on untouched handles.
        val lifecycleChanged =
            if (desiredStatusBarVisibility != null) updateLifecycleObserver(v) else false
        val orchestrator =
            WindowStatusBarOrchestrator.getOrCreateIfAttached(v) ?: windowOrchestrator
        if (orchestrator == null) {
            ensureListeners(v)
            return
        }
        val previousOrchestrator = windowOrchestrator
        if (previousOrchestrator != null && previousOrchestrator !== orchestrator) {
            previousOrchestrator.onHandleUnregistered(this, null)
        }
        windowOrchestrator = orchestrator
        // Evaluate against the post-updateLifecycleObserver() state so explicitLifecycle switches
        // in update() are reflected before deciding whether to promote this handle.
        val becameActive = !wasActiveBeforeSync && isActive
        when {
            becameActive && screenBecameActive -> orchestrator.onHandleBecameActive(this, v)
            becameActive && lifecycleChanged -> orchestrator.onHandleLifecycleResumed(this, v)
            else -> orchestrator.onHandleUpdated(this, v)
        }
        if (orchestrator.isReady(v)) {
            clearListeners()
        } else {
            ensureListeners(v)
        }
    }

    override fun update(visible: Boolean, isScreenActive: Boolean, lifecycle: Lifecycle?) {
        // Capture pre-update active state before mutating isScreenActive / explicitLifecycle so
        // syncWithWindowOrchestrator() can detect transitions after switching observedLifecycle.
        val wasScreenActive = this.isScreenActive
        val wasActive = isActive
        this.isScreenActive = isScreenActive
        hasExplicitLifecycle = lifecycle != null
        explicitLifecycleRef = lifecycle?.let { WeakReference(it) }
        desiredStatusBarVisibility = visible
        syncWithWindowOrchestrator(
            screenBecameActive = !wasScreenActive && isScreenActive,
            wasActiveBeforeSync = wasActive,
        )
    }

    /** Clears listeners, lifecycle observers, and per-handle tracking state. */
    private fun reset() {
        clearListeners()
        clearLifecycleObserver()
        hasExplicitLifecycle = false
        explicitLifecycleRef = null
        isScreenActive = true
        desiredStatusBarVisibility = null
        hasBeenActive = false
    }

    override fun restoreInitialStatusBarState() {
        reset()
        val v = view
        val orchestrator =
            windowOrchestrator ?: v?.let { WindowStatusBarOrchestrator.getIfAttached(it) }
        orchestrator?.onHandleUnregistered(this, v)
        windowOrchestrator = null
    }

    override fun dispose() {
        reset()
        val v = view
        val orchestrator =
            windowOrchestrator ?: v?.let { WindowStatusBarOrchestrator.getIfAttached(it) }
        orchestrator?.onHandleDisposed(this)
        windowOrchestrator = null
        viewRef.clear()
    }

    override fun isForWindow(view: View): Boolean =
        this.view?.isSameWindow(view) == true ||
            windowOrchestrator?.rootView?.isSameWindow(view) == true
}

/**
 * Single window-scoped orchestrator per root [View] (`DecorView`) that owns the window's status bar
 * baseline and [android.view.WindowInsetsController] mutations across all
 * [StatusBarOrchestratorHandle] callers sharing the window.
 */
@androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
private class WindowStatusBarOrchestrator(initialRootView: View) {
    private val rootViewRef: WeakReference<View> = WeakReference(initialRootView)
    internal val rootView: View?
        get() = rootViewRef.get()

    /**
     * The original system status bar visibility state prior to any scaffold-driven modifications on
     * this window. Captured once from [View.getRootWindowInsets] and retained for the lifetime of
     * the window.
     */
    private var initialStatusBarVisibility: Boolean? = null

    /**
     * Tracks the last visibility successfully applied to this window's
     * [android.view.WindowInsetsController] to prevent redundant platform calls.
     */
    private var lastAppliedStatusBarVisibility: Boolean? = null

    /**
     * Tracks whether any handle has dispatched a status bar visibility mutation on this window
     * since baseline capture or last full restoration.
     */
    private var hasMutatedStatusBar: Boolean = false

    /**
     * Registered caller handles currently requesting status bar visibility on this window, ordered
     * from oldest/lowest precedence at index `0` to most recently registered or promoted (highest
     * precedence) at the end of the list.
     */
    private val activeHandles = mutableListOf<StatusBarOrchestratorHandle>()

    /** Removes handles whose host [View] has been garbage-collected. */
    private fun pruneStaleHandles() {
        for (i in activeHandles.indices.reversed()) {
            if (activeHandles[i].view == null) {
                activeHandles.removeAt(i)
            }
        }
    }

    /**
     * Returns `true` when this window's initial status bar baseline has been captured and a
     * [android.view.WindowInsetsController] is available from [callerView] or [rootView].
     */
    fun isReady(callerView: View?): Boolean {
        val v = callerView ?: rootView ?: return false
        val controller = v.windowInsetsController ?: rootView?.windowInsetsController
        return initialStatusBarVisibility != null && controller != null
    }

    /**
     * Selects the highest-precedence handle from [activeHandles] that has a non-null
     * [StatusBarOrchestratorHandle.desiredStatusBarVisibility].
     *
     * Scans from the end of [activeHandles] toward index `0` across fallback tiers:
     * 1. Active handles (`isActive`: both `isScreenActive` and `isResumed`)
     * 2. `isScreenActive && hasBeenActive` (e.g., active screen while host lifecycle is paused)
     * 3. `isScreenActive`
     * 4. `hasBeenActive`
     * 5. Any remaining eligible handle
     */
    private fun resolveEffectiveHandle(): StatusBarOrchestratorHandle? {
        pruneStaleHandles()
        var tierScreenActiveAndHasBeenActive: StatusBarOrchestratorHandle? = null
        var tierScreenActive: StatusBarOrchestratorHandle? = null
        var tierHasBeenActive: StatusBarOrchestratorHandle? = null
        var fallback: StatusBarOrchestratorHandle? = null
        for (i in activeHandles.indices.reversed()) {
            val candidate = activeHandles[i]
            if (candidate.desiredStatusBarVisibility != null) {
                if (candidate.isActive) return candidate
                if (candidate.isScreenActive && candidate.hasBeenActive) {
                    if (tierScreenActiveAndHasBeenActive == null) {
                        tierScreenActiveAndHasBeenActive = candidate
                    }
                } else if (candidate.isScreenActive) {
                    if (tierScreenActive == null) tierScreenActive = candidate
                } else if (candidate.hasBeenActive) {
                    if (tierHasBeenActive == null) tierHasBeenActive = candidate
                }
                if (fallback == null) fallback = candidate
            }
        }
        return tierScreenActiveAndHasBeenActive ?: tierScreenActive ?: tierHasBeenActive ?: fallback
    }

    /**
     * Ensures [handle] is registered in [activeHandles] when it has a desired visibility, and
     * records [StatusBarOrchestratorHandle.hasBeenActive] if it is currently active.
     *
     * Returns `true` if [handle] was already present in [activeHandles] (so callers may promote it
     * if appropriate), or `false` if it was newly appended at the end or has no desired visibility.
     */
    private fun ensureHandleRegistered(handle: StatusBarOrchestratorHandle): Boolean {
        pruneStaleHandles()
        if (handle.desiredStatusBarVisibility == null) return false
        if (handle.isActive) {
            handle.hasBeenActive = true
        }
        if (!activeHandles.contains(handle)) {
            activeHandles.add(handle)
            return false
        }
        return true
    }

    /**
     * Registers or updates [handle] in [activeHandles] without changing existing handle precedence,
     * and applies the effective window status bar visibility once baseline insets and a controller
     * are available.
     */
    fun onHandleUpdated(handle: StatusBarOrchestratorHandle, callerView: View?) {
        ensureHandleRegistered(handle)
        attemptCaptureAndApply(callerView)
    }

    /**
     * Registers [handle] (if needed), promotes it to the end of [activeHandles] when active (e.g.,
     * when [LocalScreenIsActive] becomes `true`), and applies the effective window status bar
     * visibility.
     */
    fun onHandleBecameActive(handle: StatusBarOrchestratorHandle, callerView: View?) {
        if (ensureHandleRegistered(handle) && handle.isActive) {
            activeHandles.remove(handle)
            activeHandles.add(handle)
        }
        attemptCaptureAndApply(callerView)
    }

    /**
     * Registers [handle] (if needed), promotes active handles sharing its [Lifecycle] together in
     * their existing relative order to the end of [activeHandles], and applies the effective window
     * status bar visibility.
     */
    fun onHandleLifecycleResumed(handle: StatusBarOrchestratorHandle, callerView: View?) {
        if (ensureHandleRegistered(handle) && handle.isActive && activeHandles.size > 1) {
            val lifecycle = handle.observedLifecycle
            if (lifecycle != null) {
                // Promote active handles sharing this Lifecycle together in their existing
                // relative order so a background handle does not momentarily jump ahead of a
                // foreground handle on the same Lifecycle, without moving inactive siblings.
                val sameLifecycleHandles = mutableListOf<StatusBarOrchestratorHandle>()
                for (i in activeHandles.indices) {
                    val candidate = activeHandles[i]
                    if (candidate.observedLifecycle === lifecycle && candidate.isActive) {
                        candidate.hasBeenActive = true
                        sameLifecycleHandles.add(candidate)
                    }
                }
                activeHandles.removeAll(sameLifecycleHandles)
                activeHandles.addAll(sameLifecycleHandles)
            } else {
                activeHandles.remove(handle)
                activeHandles.add(handle)
            }
        }
        attemptCaptureAndApply(callerView)
    }

    /**
     * Unregisters [handle] when its caller leaves composition or releases suppression.
     *
     * If other handles remain on this window, hands off status bar visibility to the next effective
     * handle; otherwise restores the window's original [initialStatusBarVisibility] baseline if any
     * mutation occurred.
     */
    fun onHandleUnregistered(handle: StatusBarOrchestratorHandle, callerView: View?) {
        val wasRemoved = activeHandles.remove(handle)
        pruneStaleHandles()
        if (!wasRemoved || !hasMutatedStatusBar) return
        val remainingHandle = resolveEffectiveHandle()
        val controller =
            callerView?.windowInsetsController
                ?: remainingHandle?.view?.windowInsetsController
                ?: rootView?.windowInsetsController
                ?: return

        // If another active handle is still registered on this window, hand off to its desired
        // visibility instead of clobbering the shared window back to the system baseline.
        val remainingTargetVisibility = remainingHandle?.desiredStatusBarVisibility
        if (remainingTargetVisibility != null) {
            applyVisibility(controller, remainingTargetVisibility)
            return
        }

        // No active handles remain on this window. Revert system status bar visibility back to the
        // window's original baseline state.
        val visible = initialStatusBarVisibility ?: return
        try {
            if (visible) {
                controller.show(WindowInsets.Type.statusBars())
            } else {
                controller.hide(WindowInsets.Type.statusBars())
            }
            lastAppliedStatusBarVisibility = null
            hasMutatedStatusBar = false
        } catch (e: Exception) {
            // Swallowing exceptions here as WindowInsetsController is a newer API whose
            // platform implementations across OEM hardware may throw unexpected runtime
            // exceptions. We avoid crashing the app due to framework controller issues.
            Log.w(TAG, "Failed to restore status bar", e)
        }
    }

    /**
     * Unregisters [handle] without restoring the window's initial baseline (matching
     * [StatusBarOrchestrator.dispose]).
     *
     * If other handles remain, hands off to the next effective handle. If no handles remain, clears
     * [lastAppliedStatusBarVisibility] so a future handle re-applies its desired state, while
     * keeping [hasMutatedStatusBar] intact since the physical window baseline was not restored.
     */
    fun onHandleDisposed(handle: StatusBarOrchestratorHandle) {
        val wasRemoved = activeHandles.remove(handle)
        pruneStaleHandles()
        if (activeHandles.isEmpty()) {
            lastAppliedStatusBarVisibility = null
        } else if (wasRemoved) {
            attemptCaptureAndApply(null)
        }
    }

    private fun attemptCaptureAndApply(callerView: View?) {
        val v = callerView ?: rootView ?: return

        // Step 1: Secure initial system status bar visibility baseline state before performing any
        // window modifications.
        if (initialStatusBarVisibility == null) {
            val insets = v.rootWindowInsets ?: rootView?.rootWindowInsets
            if (insets != null) {
                initialStatusBarVisibility = insets.isVisible(WindowInsets.Type.statusBars())
            }
        }

        // Step 2: Only mutate status bar visibility if baseline is captured and
        // WindowInsetsController is available.
        if (initialStatusBarVisibility != null) {
            val effectiveHandle = resolveEffectiveHandle() ?: return
            val targetVisibility = effectiveHandle.desiredStatusBarVisibility ?: return
            val controller =
                v.windowInsetsController
                    ?: effectiveHandle.view?.windowInsetsController
                    ?: rootView?.windowInsetsController
                    ?: return

            applyVisibility(controller, targetVisibility)
        }
    }

    private fun applyVisibility(
        controller: android.view.WindowInsetsController,
        targetVisibility: Boolean,
    ) {
        if (targetVisibility == lastAppliedStatusBarVisibility) return

        try {
            if (targetVisibility) {
                controller.show(WindowInsets.Type.statusBars())
            } else {
                controller.hide(WindowInsets.Type.statusBars())
            }
            lastAppliedStatusBarVisibility = targetVisibility
            hasMutatedStatusBar = true
        } catch (e: Exception) {
            // Swallowing exceptions here as WindowInsetsController is a newer API whose
            // platform implementations across OEM hardware may throw unexpected runtime
            // exceptions. We avoid crashing the app due to framework controller issues.
            Log.w(TAG, "Failed to change status bar visibility", e)
        }
    }

    companion object {
        private val orchestrators = WeakHashMap<View, WindowStatusBarOrchestrator>()

        /**
         * Resolves the root window [View] (`DecorView`) only when [view] is attached to a window.
         * When [view] is unattached, `view.rootView` returns [view] itself rather than the
         * `DecorView`, so we return `null` until attachment.
         */
        private fun resolveAttachedRootView(view: View): View? {
            val root = view.rootView ?: view
            val isAttached =
                view.isAttachedToWindow ||
                    root.isAttachedToWindow ||
                    view.rootWindowInsets != null ||
                    root.rootWindowInsets != null
            return if (isAttached) root else null
        }

        fun getIfAttached(view: View): WindowStatusBarOrchestrator? =
            synchronized(orchestrators) {
                val root = resolveAttachedRootView(view) ?: return null
                orchestrators[root]
            }

        fun getOrCreateIfAttached(view: View): WindowStatusBarOrchestrator? =
            synchronized(orchestrators) {
                val root = resolveAttachedRootView(view) ?: return null
                orchestrators.getOrPut(root) { WindowStatusBarOrchestrator(root) }
            }
    }
}

internal fun View.isSameWindow(other: View): Boolean {
    if (this === other) return true
    val ourRoot = this.rootView
    val targetRoot = other.rootView
    return ourRoot != null && targetRoot != null && ourRoot === targetRoot
}
