/*
 * Copyright 2024 The Android Open Source Project
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

import android.view.View
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastLastOrNull
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.ScrollInfoProvider
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * State object for [AppScaffold] that coordinates application-level scaffold state between
 * [AppScaffold], individual [ScreenScaffold] instances, and [HorizontalPagerScaffold] or
 * [VerticalPagerScaffold] instances.
 *
 * @param appTimeText The default time text composable provided by [AppScaffold].
 * @param doesAppScaffoldWantStatusBar Whether the system status bar overlay is enabled at the app
 *   level.
 * @param appWindowView The [View] associated with the root [AppScaffold] window.
 */
internal class ScaffoldState(
    appTimeText: State<@Composable () -> Unit> = mutableStateOf({}),
    doesAppScaffoldWantStatusBar: State<Boolean> = mutableStateOf(true),
    appWindowView: State<View>,
) {
    val screenContent =
        ScreenContent(
            doesAppScaffoldWantStatusBar = doesAppScaffoldWantStatusBar,
            appTimeText = appTimeText,
            appWindowView = appWindowView,
        )

    /**
     * Represents the scale factor applied to the parent screen. This should be used when scaling is
     * needed for transitions or other animations affecting the parent.
     */
    var parentScale = mutableFloatStateOf(1f)

    /** Whether the scroll indicator should be kept visible even when it's idle. */
    val keepIndicatorVisible: MutableState<Boolean> = mutableStateOf(false)
}

/**
 * Coordinates the application's stack of screens, managing status bar visibility, scroll info
 * providers, time text transitions, and multi-window status bar orchestration for the active top
 * screen.
 *
 * This class coordinates screen lifecycle across multiple Android windows (such as the main
 * Activity window and dialog subcomposition windows). It maintains a collection of
 * [StatusBarOrchestrator] instances, routes status bar visibility commands to the active top-most
 * screen's window orchestrator, and automatically disposes window orchestrators when they are no
 * longer in use.
 *
 * @param doesAppScaffoldWantStatusBar Default status bar overlay visibility configured by
 *   [AppScaffold].
 * @param appTimeText Default application-level [TimeText] composable.
 * @param appWindowView The [View] associated with the root [AppScaffold] window.
 */
internal class ScreenContent(
    private val doesAppScaffoldWantStatusBar: State<Boolean>,
    private val appTimeText: State<@Composable () -> Unit>,
    private val appWindowView: State<View>,
) {
    /** Active [StatusBarOrchestrator] instances for windows managed by this scaffold. */
    private val orchestrators = mutableListOf<StatusBarOrchestrator>()

    /**
     * Returns the active top-most screen's status bar orchestrator, or falls back to the app window
     * orchestrator if no screen is on the stack.
     */
    val currentActiveOrchestrator: State<StatusBarOrchestrator> = derivedStateOf {
        val targetView = statusBarItems.lastOrNull()?.view?.value ?: appWindowView.value
        orchestrators.fastFirstOrNull { it.isForWindow(targetView) }
            ?: run {
                cleanupUnusedOrchestrators()
                StatusBarOrchestrator(targetView).also { orchestrators.add(it) }
            }
    }

    /**
     * Evaluates status bar visibility for the active top-most screen on the stack, falling back to
     * [doesAppScaffoldWantStatusBar] if no screen is on the stack.
     */
    val shouldActiveWindowShowStatusBar: State<Boolean> = derivedStateOf {
        statusBarItems.lastOrNull()?.showStatusBar?.value ?: doesAppScaffoldWantStatusBar.value
    }

    /**
     * Evaluates status bar visibility specifically for the root App Window by selecting the
     * top-most screen belonging to [appWindowView], falling back to [doesAppScaffoldWantStatusBar].
     *
     * Used by [AppScaffold] to determine whether the status bar should be shown on the root App
     * Window.
     */
    val shouldAppWindowShowStatusBar: State<Boolean> = derivedStateOf {
        val appView = appWindowView.value
        statusBarItems
            .toList()
            .fastLastOrNull { appView.isSameWindow(it.view.value) }
            ?.showStatusBar
            ?.value ?: doesAppScaffoldWantStatusBar.value
    }

    /**
     * Returns the [ScrollInfoProvider] for the active top-most screen on the stack that provides
     * one, or `null` if no screen on the stack is scrollable.
     */
    val currentScrollInfoProvider: State<ScrollInfoProvider?> = derivedStateOf {
        screenItems
            .toList()
            .fastLastOrNull { it.scrollInfoProvider.value != null }
            ?.scrollInfoProvider
            ?.value
    }

    /**
     * Returns the anchor item scroll offset from the active top-most screen's [ScrollInfoProvider],
     * or [Float.NaN] if no provider is present.
     */
    val currentAnchorItemOffset: State<Float> = derivedStateOf {
        currentScrollInfoProvider.value?.anchorItemOffset ?: Float.NaN
    }

    /**
     * Evaluates the active time text composable from the screen stack, falling back to
     * [appTimeText] if none provided and the system status bar overlay is not enabled.
     */
    val currentTimeText: State<@Composable () -> Unit> = derivedStateOf {
        screenItems.toList().fastLastOrNull { it.timeText.value != null }?.timeText?.value
            ?: if (doesAppScaffoldWantStatusBar.value) {
                {}
            } else {
                appTimeText.value
            }
    }

    /**
     * Renders the active time text element, wrapping it with scroll-away behavior if a
     * [ScrollInfoProvider] is present on the active screen.
     */
    val timeText: @Composable (() -> Unit)
        get() = {
            if (!shouldAppWindowShowStatusBar.value) {
                val timeText = currentTimeText.value
                val scrollInfoProvider = currentScrollInfoProvider.value
                Box(
                    modifier =
                        scrollInfoProvider?.let {
                            Modifier.fillMaxSize().scrollAway(it) { screenStage.value }
                        } ?: Modifier
                ) {
                    timeText()
                }
            }
        }

    /**
     * Removes the screen content associated with [key] from the screen stack.
     *
     * @param key The unique key identifying the screen content to remove.
     */
    fun removeScreenContent(key: Any) {
        val index = screenItems.indexOfFirst { it.key === key }
        if (index >= 0) {
            screenItems.removeAt(index)
        }
    }

    /**
     * Adds screen content (time text and scroll info provider) to the top of the screen stack using
     * reactive [State] handles.
     *
     * @param key The unique key identifying this screen.
     * @param timeText The custom time text composable state for this screen.
     * @param scrollInfoProvider The [ScrollInfoProvider] state for scroll-driven effects.
     */
    fun addScreenContent(
        key: Any,
        timeText: State<(@Composable () -> Unit)?>,
        scrollInfoProvider: State<ScrollInfoProvider?>,
    ) {
        val existingIndex = screenItems.indexOfFirst { it.key === key }
        if (existingIndex >= 0) {
            screenItems.removeAt(existingIndex)
        }
        screenItems.add(
            ScreenData(
                key = key,
                scrollInfoProvider = scrollInfoProvider,
                timeText = timeText,
            )
        )
    }

    /**
     * Removes the status bar configuration associated with [key] from the status bar stack and
     * disposes its associated window orchestrator if no remaining status bar layers or host view
     * are using it.
     *
     * @param key The unique key identifying the status bar layer to remove.
     */
    fun removeStatusBar(key: Any) {
        val index = statusBarItems.indexOfFirst { it.key === key }
        if (index >= 0) {
            statusBarItems.removeAt(index)
            cleanupUnusedOrchestrators()
        }
    }

    /**
     * Adds a status bar configuration to the top of the status bar stack using reactive [State]
     * handles.
     *
     * @param key The unique key identifying this status bar layer.
     * @param view The [View] state associated with this layer, used to resolve its root window.
     * @param showStatusBar Whether the status bar overlay is enabled for this layer.
     */
    fun addStatusBar(
        key: Any,
        view: State<View>,
        showStatusBar: State<Boolean>,
    ) {
        val existingIndex = statusBarItems.indexOfFirst { it.key === key }
        if (existingIndex >= 0) {
            statusBarItems.removeAt(existingIndex)
        }
        statusBarItems.add(
            StatusBarData(
                key = key,
                view = view,
                showStatusBar = showStatusBar,
            )
        )
    }

    /** Restores all registered window status bar states and clears all active orchestrators. */
    fun cleanupAllOrchestrators() {
        orchestrators.fastForEach { it.restoreInitialStatusBarState() }
        orchestrators.clear()
    }

    internal val screenStage: MutableState<ScreenStage> = mutableStateOf(ScreenStage.New)

    @Composable
    internal fun UpdateIdlingDetectorIfNeeded() {
        val scrollInfoProvider = currentScrollInfoProvider.value
        SideEffect(scrollInfoProvider) { screenStage.value = ScreenStage.New }
        if (scrollInfoProvider?.isScrollInProgress == true) {
            screenStage.value = ScreenStage.Scrolling
        } else {
            LaunchedEffect(scrollInfoProvider) {
                // Entering the idle state will show the Time text (if it's hidden) AND hide the
                // scroll indicator.
                delay(IDLE_DELAY)
                screenStage.value = ScreenStage.Idle
            }
        }
    }

    /**
     * Disposes and evicts any [StatusBarOrchestrator] that is no longer in active use by either
     * [appWindowView] or any active screen in [statusBarItems].
     */
    private fun cleanupUnusedOrchestrators() {
        val appView = appWindowView.value
        orchestrators.removeAll { orchestrator ->
            val inUse =
                orchestrator.isForWindow(appView) ||
                    statusBarItems.toList().fastAny { orchestrator.isForWindow(it.view.value) }
            if (!inUse) {
                // An orchestrator not in use belongs to a secondary window (e.g. a Dialog)
                // with no remaining screens on the stack (appWindowView is retained in inUse).
                // We do not restore its initial insets state here: if the window is being dismissed
                // and its initial baseline differs from what the host window wants (e.g. a dialog
                // started hidden, but the host screen is enabled), resetting the departing window
                // would briefly mutate SystemUI and cause status bar flicker. We only dispose
                // listeners and let WindowManager transition insets naturally.
                orchestrator.dispose()
                true
            } else {
                false
            }
        }
    }

    /** Stack of active screens registered with the scaffold. */
    private val screenItems = mutableStateListOf<ScreenData>()

    /** Stack of active status bar configurations registered with the scaffold. */
    private val statusBarItems = mutableStateListOf<StatusBarData>()

    /**
     * Internal metadata representing a screen registered with [ScreenContent].
     *
     * @property key Unique identifier for the screen.
     * @property scrollInfoProvider Provider for scroll information used for scroll-away effects.
     * @property timeText Optional custom time text composable for this screen.
     */
    private class ScreenData(
        val key: Any,
        val scrollInfoProvider: State<ScrollInfoProvider?>,
        val timeText: State<(@Composable () -> Unit)?>,
    )

    /**
     * Internal metadata representing a status bar configuration registered with [ScreenContent].
     *
     * @property key Unique identifier for the status bar layer.
     * @property view The Android [View] associated with this layer, used to resolve its root
     *   window.
     * @property showStatusBar Whether the status bar overlay is enabled for this layer.
     */
    private class StatusBarData(
        val key: Any,
        val view: State<View>,
        val showStatusBar: State<Boolean>,
    )
}

@Composable
internal fun AnimatedIndicator(
    isVisible: () -> Boolean,
    modifier: Modifier = Modifier,
    animationSpec: AnimationSpec<Float>? = INDICATOR_FADE_OUT_ANIMATION,
    content: @Composable (BoxScope.() -> Unit)? = null,
) {
    // Skip if no indicator provided
    content?.let { pageIndicator ->
        if (animationSpec == null) {
            // if no animationSpec is provided then indicator will always be visible
            Box(modifier = modifier, content = pageIndicator)
        } else {
            // if animationSpec is provided this will be used to fade out indicator
            val alphaValue = remember { mutableFloatStateOf(0f) }
            LaunchedEffect(isVisible) {
                launch {
                    snapshotFlow { if (isVisible()) 1f else 0f }
                        .distinctUntilChanged()
                        .collectLatest { targetValue ->
                            animate(
                                alphaValue.floatValue,
                                targetValue,
                                animationSpec = animationSpec,
                            ) { value, _ ->
                                alphaValue.floatValue = value
                            }
                        }
                }
            }
            Box(
                modifier = modifier.graphicsLayer { alpha = alphaValue.floatValue },
                content = pageIndicator,
            )
        }
    }
}

internal val LocalScaffoldState = compositionLocalOf<ScaffoldState?> { null }

/**
 * Registers an active screen content layer (timeText and scrollInfoProvider) with
 * [ScaffoldState.screenContent]. Typically used by [ScreenScaffold], [HorizontalPagerScaffold], and
 * [VerticalPagerScaffold] to sync their internal state with the global [AppScaffold].
 *
 * Coordinates screen lifecycle with the scaffold, attaching [timeText] and [scrollInfoProvider]
 * state handles while active and removing the screen content registration when leaving composition
 * or becoming inactive.
 */
@Composable
internal fun ScreenContentRegistration(
    timeText: State<(@Composable () -> Unit)?>,
    scrollInfoProvider: State<ScrollInfoProvider?>,
) {
    // If there is no AppScaffold in the hierarchy (such as when ScreenScaffold or
    // PagerScaffold is rendered standalone in tests or isolated previews), there is no
    // application-level time text or scroll-away coordination, making registration a safe no-op.
    val scaffoldState = LocalScaffoldState.current ?: return
    val screenIsActive = LocalScreenIsActive.current

    val key = remember { Any() }

    DisposableEffect(screenIsActive, scaffoldState) {
        if (screenIsActive) {
            scaffoldState.screenContent.addScreenContent(
                key = key,
                timeText = timeText,
                scrollInfoProvider = scrollInfoProvider,
            )
        }
        onDispose { scaffoldState.screenContent.removeScreenContent(key) }
    }
}

/**
 * Registers an active status bar layer with [ScaffoldState.screenContent]. Typically used by
 * [ScreenScaffold], [HorizontalPagerScaffold], [VerticalPagerScaffold], and [StatusBarSuppression]
 * to sync their status bar state with the global [AppScaffold].
 *
 * Coordinates status bar lifecycle with the scaffold, attaching [showStatusBar] state handle while
 * active and removing the status bar registration when leaving composition or becoming inactive.
 */
@Composable
internal fun StatusBarRegistration(showStatusBar: State<Boolean>) {
    // If there is no AppScaffold in the hierarchy, system status bar orchestration is not active
    // on this window, so registering status bar state with an application scaffold is a safe no-op.
    val scaffoldState = LocalScaffoldState.current ?: return
    val screenIsActive = LocalScreenIsActive.current

    val key = remember { Any() }
    val viewState = rememberUpdatedState(LocalView.current)

    DisposableEffect(screenIsActive, scaffoldState) {
        if (screenIsActive) {
            scaffoldState.screenContent.addStatusBar(
                key = key,
                view = viewState,
                showStatusBar = showStatusBar,
            )
        }
        onDispose { scaffoldState.screenContent.removeStatusBar(key) }
    }
}

private const val IDLE_DELAY = 2000L

internal object AnimationCoordinator {
    fun register() {
        if (registeredCount.incrementAndGet() > 0) running = true
    }

    fun unregister() {
        if (registeredCount.decrementAndGet() <= 0) running = false
    }

    /**
     * The frame time in milliseconds in the calling context of frame dispatch. Used to coordinate
     * animations. If animations are not running this will be Long.MAX_VALUE. Provided by
     * [withInfiniteAnimationFrameMillis].
     */
    val frameMillis = mutableLongStateOf(Long.MAX_VALUE)

    @Composable
    fun Looper() {
        LaunchedEffect(running) {
            if (running) {
                // DO NOT check running in the while, since this may see changes that the
                // LaunchedEffect misses. When running becomes false, this function will recompose
                // and the LaunchedEffect will cancel the running coroutine anyway.
                while (isActive) {
                    withInfiniteAnimationFrameMillis { frameMillis.longValue = it }
                }
            } else {
                // This should make all animations finish :)
                frameMillis.longValue = Long.MAX_VALUE
            }
        }
    }

    private val registeredCount = AtomicInteger(0)
    private var running by mutableStateOf(false)
}

internal val INDICATOR_FADE_OUT_ANIMATION: AnimationSpec<Float> =
    spring(stiffness = Spring.StiffnessMediumLow)
