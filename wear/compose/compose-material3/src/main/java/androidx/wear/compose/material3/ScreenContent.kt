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

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastLastOrNull
import androidx.wear.compose.foundation.LocalScreenIsActive
import androidx.wear.compose.foundation.ScrollInfoProvider

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
    private val appTimeText: State<(@Composable () -> Unit)?>,
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
                appTimeText.value ?: {}
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
        UpdateIdlingDetectorIfNeeded(currentScrollInfoProvider.value, screenStage)
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

/**
 * Registers an active screen content layer (timeText and scrollInfoProvider) with
 * [AppScaffoldState.screenContent]. Typically used by [ScreenScaffold], [HorizontalPagerScaffold],
 * and [VerticalPagerScaffold] to sync their internal state with the enclosing [AppScaffoldState].
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
    val scaffoldState = LocalScaffoldState.current as? AppScaffoldState ?: return
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
