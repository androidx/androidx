/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.SwipeToDismissKeys
import androidx.wear.compose.foundation.SwipeToDismissValue
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberSwipeToDismissBoxState
import androidx.wear.compose.integration.demos.common.ActivityDemo
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Demo
import androidx.wear.compose.integration.demos.common.DemoCategory
import androidx.wear.compose.integration.demos.common.DemoParameters
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text

@Composable
fun DemoApp(
    currentDemo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    androidx.wear.compose.material3.AppScaffold(timeText = {}) {
        val swipeToDismissState = swipeDismissStateWithNavigation(onNavigateBack)
        DisplayDemo(swipeToDismissState, currentDemo, parentDemo, onNavigateTo, onNavigateBack)
    }
}

@Composable
private fun DisplayDemo(
    state: SwipeToDismissBoxState,
    currentDemo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit
) {
    SwipeToDismissBox(
        state = state,
        hasBackground = parentDemo != null,
        backgroundKey = parentDemo?.title ?: SwipeToDismissKeys.Background,
        contentKey = currentDemo.title,
    ) { isBackground ->
        BoxDemo(state, if (isBackground) parentDemo else currentDemo, onNavigateTo, onNavigateBack)
    }
}

@Composable
private fun BoxScope.BoxDemo(
    state: SwipeToDismissBoxState,
    demo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit
) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo -> {
            demo.content(DemoParameters(onNavigateBack, state))
        }
        is DemoCategory -> {
            DisplayDemoList(demo, onNavigateTo)
        }
        else -> {}
    }
}

@Composable
internal fun BoxScope.DisplayDemoList(category: DemoCategory, onNavigateTo: (Demo) -> Unit) {
    val state = category.getScrollStateOrInit { rememberScalingLazyListState() }
    val scaffoldWrapper =
        @Composable { it: @Composable () -> Unit ->
            // Only material3 demos benefit from the Material3 ScreenScaffold
            if (category.materialVersion == 3) {
                val timeText = @Composable { androidx.wear.compose.material3.TimeText { time() } }
                androidx.wear.compose.material3.ScreenScaffold(
                    scrollState = state,
                    timeText = remember { timeText },
                ) {
                    it()
                }
            } else {
                it()
            }
        }
    scaffoldWrapper {
        ScalingLazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().testTag(DemoListTag),
            state = state,
            autoCentering = AutoCenteringParams(itemIndex = if (category.demos.size >= 2) 2 else 1),
            contentPadding =
                PaddingValues(horizontal = LocalConfiguration.current.screenWidthDp.dp * 0.052f),
        ) {
            item {
                ListHeader {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.caption1,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            category.demos.forEach { demo ->
                item {
                    Chip(
                        onClick = { onNavigateTo(demo) },
                        colors = ChipDefaults.secondaryChipColors(),
                        label = {
                            Text(
                                text = demo.title,
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                demo.description?.let { description ->
                    item {
                        CompositionLocalProvider(
                            LocalTextStyle provides MaterialTheme.typography.caption3
                        ) {
                            Text(
                                text = description,
                                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun swipeDismissStateWithNavigation(onNavigateBack: () -> Unit): SwipeToDismissBoxState {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeToDismissValue.Dismissed) {
            state.snapTo(SwipeToDismissValue.Default)
            onNavigateBack()
        }
    }
    return state
}
