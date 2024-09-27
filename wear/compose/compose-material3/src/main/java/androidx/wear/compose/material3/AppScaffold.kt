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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

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
 * @param modifier The modifier for the top level of the scaffold.
 * @param timeText The default time (and potentially status message) to display at the top middle of
 *   the screen in this app. When [AppScaffold] is used in combination with [ScreenScaffold], the
 *   time text will be scrolled away and shown/hidden according to the scroll state of the screen.
 * @param content The main content for this application.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    timeText: @Composable () -> Unit = { TimeText { time() } },
    content: @Composable BoxScope.() -> Unit,
) {
    CompositionLocalProvider(
        LocalScaffoldState provides ScaffoldState(timeText),
    ) {
        val scaffoldState = LocalScaffoldState.current
        Box(
            modifier =
                modifier.fillMaxSize().graphicsLayer {
                    scaleX = scaffoldState.parentScale.floatValue
                    scaleY = scaffoldState.parentScale.floatValue
                }
        ) {
            content()
            scaffoldState.timeText()
        }
    }
}
