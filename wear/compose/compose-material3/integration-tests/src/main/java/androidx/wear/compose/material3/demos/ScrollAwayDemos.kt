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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ScrollInfoProvider
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.integration.demos.common.Centralize
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenStage
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TimeText
import androidx.wear.compose.material3.samples.ScrollAwaySample
import androidx.wear.compose.material3.scrollAway

val ScrollAwayDemos =
    listOf(
        ComposableDemo("Scaling Lazy Column") { ScrollAwaySample() },
        ComposableDemo("LazyColumn") { Centralize { ScrollAwayLazyColumn() } },
        ComposableDemo("Column") { Centralize { ScrollAwayColumn() } }
    )

@Composable
fun ScrollAwayLazyColumn() {
    val scrollState = rememberLazyListState()
    val focusRequester = rememberActiveFocusRequester()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = scrollState,
            modifier =
                Modifier.fillMaxSize()
                    .rotaryScrollable(
                        RotaryScrollableDefaults.behavior(
                            scrollableState = scrollState,
                            flingBehavior = ScrollableDefaults.flingBehavior()
                        ),
                        focusRequester = focusRequester
                    )
        ) {
            item {
                ListHeader {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        text = "Lazy Column",
                        textAlign = TextAlign.Center
                    )
                }
            }
            items(5) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 2.dp),
                    onClick = {},
                    label = { Text("Item ${it + 1}") },
                )
            }
        }

        TimeText(
            // It is recommended to use the [AppScaffold] and [ScreenScaffold] with [TimeText],
            // so that the correct scroll-away behavior is provided by default.
            // This demo shows how [Modifier.scrollAway] can be applied to an item if the
            // default handling is unsuitable.
            modifier =
                Modifier.scrollAway(
                    scrollInfoProvider = ScrollInfoProvider(scrollState),
                    screenStage = {
                        if (scrollState.isScrollInProgress) ScreenStage.Scrolling
                        else ScreenStage.Idle
                    }
                ),
            content = { text("ScrollAway") }
        )
    }
}

@Composable
fun ScrollAwayColumn() {
    val scrollState = rememberScrollState()
    val focusRequester = rememberActiveFocusRequester()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier.verticalScroll(scrollState)
                    .rotaryScrollable(
                        RotaryScrollableDefaults.behavior(
                            scrollableState = scrollState,
                            flingBehavior = ScrollableDefaults.flingBehavior()
                        ),
                        focusRequester = focusRequester
                    )
        ) {
            ListHeader {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    text = "Column",
                    textAlign = TextAlign.Center
                )
            }
            repeat(5) {
                FilledTonalButton(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 36.dp, vertical = 2.dp),
                    onClick = {},
                    label = { Text("Item ${it + 1}") },
                )
            }
        }

        TimeText(
            // It is recommended to use the [AppScaffold] and [ScreenScaffold] with [TimeText],
            // so that the correct scroll-away behavior is provided by default.
            // This demo shows how [Modifier.scrollAway] can be applied to an item if the
            // default handling is unsuitable.
            modifier =
                Modifier.scrollAway(
                    scrollInfoProvider = ScrollInfoProvider(scrollState),
                    screenStage = {
                        if (scrollState.isScrollInProgress) ScreenStage.Scrolling
                        else ScreenStage.Idle
                    }
                ),
            content = { text("ScrollAway") }
        )
    }
}
