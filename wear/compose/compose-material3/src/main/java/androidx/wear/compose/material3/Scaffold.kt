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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.wear.compose.foundation.ScrollInfoProvider
import kotlinx.coroutines.delay

/**
 * Base state class for scaffold components ([AppScaffold], [ScreenScaffold],
 * [HorizontalPagerScaffold], and [VerticalPagerScaffold]) that coordinates [ScrollIndicator]
 * visibility and screen idling state.
 */
internal sealed class ScaffoldState {
    /** Whether the scroll indicator should be kept visible even when it's idle. */
    val keepIndicatorVisible: MutableState<Boolean> = mutableStateOf(false)

    /**
     * The current [ScreenStage] (e.g. [ScreenStage.New], [ScreenStage.Scrolling],
     * [ScreenStage.Idle]).
     */
    abstract val screenStage: State<ScreenStage>

    /** Updates [screenStage] based on active scroll progress and idling timeout. */
    @Composable abstract fun UpdateIdlingDetectorIfNeeded()
}

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
internal class AppScaffoldState(
    appTimeText: State<(@Composable () -> Unit)?> = mutableStateOf(null),
    doesAppScaffoldWantStatusBar: State<Boolean> = mutableStateOf(true),
    appWindowView: State<View>,
) : ScaffoldState() {
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

    override val screenStage: State<ScreenStage>
        get() = screenContent.screenStage

    @Composable
    override fun UpdateIdlingDetectorIfNeeded() {
        screenContent.UpdateIdlingDetectorIfNeeded()
    }
}

/**
 * Lightweight [ScaffoldState] implementation used when there is no enclosing [AppScaffold]. Tracks
 * scroll idling state and [keepIndicatorVisible] for a single screen without maintaining a screen
 * stack or status bar orchestrators.
 *
 * @param scrollInfoProvider The [ScrollInfoProvider] state for this screen.
 */
internal class ScreenScaffoldState(private val scrollInfoProvider: State<ScrollInfoProvider?>) :
    ScaffoldState() {
    override val screenStage: MutableState<ScreenStage> = mutableStateOf(ScreenStage.New)

    @Composable
    override fun UpdateIdlingDetectorIfNeeded() {
        UpdateIdlingDetectorIfNeeded(scrollInfoProvider.value, screenStage)
    }
}

@Composable
internal fun UpdateIdlingDetectorIfNeeded(
    scrollInfoProvider: ScrollInfoProvider?,
    screenStage: MutableState<ScreenStage>,
) {
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

internal val LocalScaffoldState = compositionLocalOf<ScaffoldState?> { null }

private const val IDLE_DELAY = 2000L
