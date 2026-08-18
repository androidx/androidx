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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView

/**
 * [AppScaffold] is one of the Wear Material3 scaffold components.
 *
 * The scaffold components [AppScaffold] and [ScreenScaffold] lay out the structure of a screen and
 * coordinate transitions of the [ScrollIndicator] and [TimeText] components.
 *
 * [AppScaffold] allows static screen elements such as [TimeText] to remain visible during in-app
 * transitions such as swipe-to-dismiss. It provides a slot for the main application content, which
 * will usually be supplied by a navigation component such as SwipeDismissableNavHost.
 *
 * Example of using AppScaffold and ScreenScaffold:
 *
 * @sample androidx.wear.compose.material3.samples.ScaffoldSample
 *
 * ![ScaffoldSample Composite
 * Image](https://developer.android.com/wear/images/design/WearComposeM3_ScaffoldSample_CompositeImage.png)
 *
 * @param modifier The modifier for the top level of the scaffold.
 * @param timeText The default time (and potentially status message) to display at the top middle of
 *   the screen in this app. When [AppScaffold] is used in combination with [ScreenScaffold], the
 *   time text will be scrolled away and shown/hidden according to the scroll state of the screen.
 * @param containerColor The container color of the app drawn behind the [content], i.e. the color
 *   of the background behind the content.
 * @param contentColor The content color for the application [content].
 * @param isStatusBarEnabled Whether to display the system status bar overlay across screens inside
 *   this scaffold. On devices that support the system status bar, the system overlay status bar
 *   replaces the app-level [TimeText] to prevent overlapping. When false or on unsupported devices,
 *   the scaffold defers to local [TimeText] rendering.
 * @param content The main content for this application.
 */
@Composable
public fun AppScaffold(
    modifier: Modifier = Modifier,
    timeText: @Composable () -> Unit = { TimeText() },
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    isStatusBarEnabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    // Run the animator coordinator if needed.
    AnimationCoordinator.Looper()

    val isStatusBarSupportedState = rememberUpdatedState(LocalStatusBarEnabled.current)
    val timeTextState = rememberUpdatedState(timeText)
    val showStatusBarState = rememberUpdatedState(isStatusBarEnabled)
    val scaffoldState = remember {
        ScaffoldState(
            appTimeText = timeTextState,
            appShowStatusBar = showStatusBarState,
            isStatusBarSupported = isStatusBarSupportedState,
        )
    }

    if (isStatusBarSupportedState.value) {
        val showStatusBarOverlay by remember {
            derivedStateOf {
                val isEnabled = scaffoldState.screenContent.currentShowStatusBar.value
                val stage = scaffoldState.screenContent.screenStage.value
                val provider = scaffoldState.screenContent.currentScrollInfoProvider.value
                val offset = scaffoldState.screenContent.currentAnchorItemOffset.value

                isEnabled &&
                    (stage != ScreenStage.Scrolling ||
                        provider?.isScrollAwayValid != true ||
                        offset.isNaN() ||
                        offset <= 0f)
            }
        }

        val view = LocalView.current
        val orchestrator = remember(view) { StatusBarOrchestrator(view) }

        // Restores the initial status bar state when AppScaffold leaves composition
        // or when the underlying LocalView changes.
        DisposableEffect(orchestrator) { onDispose { orchestrator.restore() } }

        LaunchedEffect(orchestrator) {
            snapshotFlow { showStatusBarOverlay }
                .collect { show -> if (show) orchestrator.show() else orchestrator.hide() }
        }
    }

    CompositionLocalProvider(
        LocalScaffoldState provides scaffoldState,
        LocalContentColor provides contentColor,
    ) {
        Box(Modifier.fillMaxSize().background(containerColor)) {
            Box(
                modifier =
                    modifier.fillMaxSize().graphicsLayer {
                        scaleX = scaffoldState.parentScale.floatValue
                        scaleY = scaffoldState.parentScale.floatValue
                    }
            ) {
                content()
                // Draw local time text when status bar is disabled or unsupported.
                // When system status bar is enabled and supported, system overlay takes over.
                if (!scaffoldState.screenContent.currentShowStatusBar.value) {
                    scaffoldState.screenContent.timeText()
                }
            }
        }
    }
}

@Deprecated(
    message =
        "This overload is deprecated, please use the new overload with the isStatusBarEnabled parameter.",
    level = DeprecationLevel.HIDDEN,
)
@Composable
public fun AppScaffold(
    modifier: Modifier = Modifier,
    timeText: @Composable () -> Unit = { TimeText() },
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    content: @Composable BoxScope.() -> Unit,
): Unit =
    AppScaffold(
        modifier = modifier,
        timeText = timeText,
        containerColor = containerColor,
        contentColor = contentColor,
        isStatusBarEnabled = true,
        content = content,
    )
