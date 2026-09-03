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
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastLastOrNull
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
 * @param appShowStatusBar Whether the system status bar overlay is enabled at the app level.
 * @param appWindowView The [View] associated with the root [AppScaffold] window.
 */
internal class ScaffoldState(
    appTimeText: State<@Composable () -> Unit> = mutableStateOf({}),
    appShowStatusBar: State<Boolean> = mutableStateOf(true),
    appWindowView: State<View>,
) {
    val screenContent =
        ScreenContent(
            appShowStatusBar = appShowStatusBar,
            appTimeText = appTimeText,
            appWindowView = appWindowView,
        )

    /**
     * Represents the scale factor applied to the parent screen. This should be used when scaling is
     * needed for transitions or other animations affecting the parent.
     */
    var parentScale = mutableFloatStateOf(1f)
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
 * @param appShowStatusBar Default status bar overlay visibility configured by [AppScaffold].
 * @param appTimeText Default application-level [TimeText] composable.
 * @param appWindowView The [View] associated with the root [AppScaffold] window.
 */
internal class ScreenContent(
    private val appShowStatusBar: State<Boolean>,
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
        val targetView = contentItems.lastOrNull()?.view?.value ?: appWindowView.value
        orchestrators.fastFirstOrNull { it.isForWindow(targetView) }
            ?: run {
                cleanupUnusedOrchestrators()
                StatusBarOrchestrator(targetView).also { orchestrators.add(it) }
            }
    }

    /**
     * Evaluates status bar visibility for the active top-most screen on the stack, falling back to
     * [appShowStatusBar] if no screen is on the stack.
     */
    val currentScreenShowStatusBar: State<Boolean> = derivedStateOf {
        contentItems.lastOrNull()?.showStatusBar?.value ?: appShowStatusBar.value
    }

    /**
     * Evaluates status bar visibility specifically for the root App Window by selecting the
     * top-most screen belonging to [appWindowView], falling back to [appShowStatusBar].
     *
     * Used by [AppScaffold] to determine whether the status bar should be shown on the root App
     * Window.
     */
    val shouldAppWindowShowStatusBar: State<Boolean> = derivedStateOf {
        val appView = appWindowView.value
        contentItems
            .toList()
            .fastLastOrNull { appView.isSameWindow(it.view.value) }
            ?.showStatusBar
            ?.value ?: appShowStatusBar.value
    }

    /**
     * Returns the [ScrollInfoProvider] for the active top-most screen on the stack, or `null` if
     * the active top screen is non-scrollable.
     */
    val currentScrollInfoProvider: State<ScrollInfoProvider?> = derivedStateOf {
        contentItems.lastOrNull()?.scrollInfoProvider?.value
    }

    /**
     * Returns the anchor item scroll offset from the active top-most screen's [ScrollInfoProvider],
     * or [Float.NaN] if no provider is present.
     */
    val currentAnchorItemOffset: State<Float> = derivedStateOf {
        currentScrollInfoProvider.value?.anchorItemOffset ?: Float.NaN
    }

    /**
     * Renders the active time text element, wrapping it with scroll-away behavior if a
     * [ScrollInfoProvider] is present on the active screen.
     */
    val timeText: @Composable (() -> Unit)
        get() = {
            val (_, timeText) = currentContent()
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

    /**
     * Removes the screen associated with [key] from the screen stack and disposes its associated
     * window orchestrator if no remaining screens or host view are using it.
     *
     * @param key The unique key identifying the screen to remove.
     */
    fun removeScreen(key: Any) {
        val index = contentItems.indexOfFirst { it.key === key }
        if (index >= 0) {
            contentItems.removeAt(index)
            cleanupUnusedOrchestrators()
        }
    }

    /**
     * Adds a screen to the top of the screen stack using reactive [State] handles.
     *
     * @param key The unique key identifying this screen.
     * @param view The [View] state associated with this screen, used to resolve its root window.
     * @param timeText The custom time text composable state for this screen.
     * @param scrollInfoProvider The [ScrollInfoProvider] state for scroll-driven effects.
     * @param showStatusBar Whether status bar overlay is enabled for this screen.
     */
    fun addScreen(
        key: Any,
        view: State<View>,
        timeText: State<(@Composable () -> Unit)?>,
        scrollInfoProvider: State<ScrollInfoProvider?>,
        showStatusBar: State<Boolean>,
    ) {
        // If a screen with this key is already present, remove it first. This ensures no duplicate
        // entries exist in contentItems (which would cause removeScreen to orphan duplicate
        // entries and leak window orchestrators during cleanup) and pushes the newly activated
        // screen to the top of the stack.
        val existingIndex = contentItems.indexOfFirst { it.key === key }
        if (existingIndex >= 0) {
            contentItems.removeAt(existingIndex)
        }

        contentItems.add(
            ScreenContentData(
                key = key,
                view = view,
                scrollInfoProvider = scrollInfoProvider,
                showStatusBar = showStatusBar,
                timeText = timeText,
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
     * [appWindowView] or any active screen in [contentItems].
     */
    private fun cleanupUnusedOrchestrators() {
        val appView = appWindowView.value
        orchestrators.removeAll { orchestrator ->
            val inUse =
                orchestrator.isForWindow(appView) ||
                    contentItems.toList().fastAny { orchestrator.isForWindow(it.view.value) }
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

    /**
     * Finds the nearest [ScreenContentData] that provides scroll info and the active time text
     * composable from the screen stack, falling back to [appTimeText] if none provided.
     */
    private fun currentContent(): Pair<ScreenContentData?, @Composable (() -> Unit)> {
        var resultTimeText: @Composable (() -> Unit)? = null
        var resultContent: ScreenContentData? = null
        contentItems.toList().fastForEach {
            if (it.timeText.value != null) {
                resultTimeText = it.timeText.value
            }
            if (it.scrollInfoProvider.value != null) {
                resultContent = it
            }
        }
        return resultContent to (resultTimeText ?: appTimeText.value)
    }

    /** Stack of active screens registered with the scaffold. */
    private val contentItems = mutableStateListOf<ScreenContentData>()

    /**
     * Internal metadata representing a screen registered with [ScreenContent].
     *
     * @property key Unique identifier for the screen.
     * @property view The Android [View] associated with the screen, used to resolve its root
     *   window.
     * @property scrollInfoProvider Provider for scroll information used for scroll-away effects.
     * @property showStatusBar Whether the status bar overlay is enabled for this screen.
     * @property timeText Optional custom time text composable for this screen.
     */
    private class ScreenContentData(
        val key: Any,
        val view: State<View>,
        val scrollInfoProvider: State<ScrollInfoProvider?>,
        val showStatusBar: State<Boolean>,
        val timeText: State<(@Composable () -> Unit)?>,
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
