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

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.rotary.onRotaryScrollEvent
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.LocalTextStyle
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.ScalingLazyColumn
import androidx.wear.compose.material.ScalingLazyColumnDefaults
import androidx.wear.compose.material.ScalingLazyListScope
import androidx.wear.compose.material.ScalingLazyListState
import androidx.wear.compose.material.ScalingParams
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.SwipeToDismissBoxDefaults
import androidx.wear.compose.material.SwipeToDismissBoxState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberScalingLazyListState
import androidx.wear.compose.material.rememberSwipeToDismissBoxState
import kotlinx.coroutines.launch

@Composable
@ExperimentalWearMaterialApi
fun DemoApp(
    currentDemo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit,
) {
    DisplayDemo(currentDemo, parentDemo, onNavigateTo, onNavigateBack)
}

@Composable
@ExperimentalWearMaterialApi
private fun DisplayDemo(
    demo: Demo,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit
) {
    when (demo) {
        is ActivityDemo<*> -> {
            /* should never get here as activity demos are not added to the backstack*/
        }
        is ComposableDemo -> {
            SwipeToDismissBox(
                state = swipeDismissStateWithNavigation(onNavigateBack),
                hasBackground = parentDemo != null,
                backgroundKey = parentDemo?.title ?: SwipeToDismissBoxDefaults.BackgroundKey,
                contentKey = demo.title,
            ) { isBackground ->
                if (isBackground) {
                    if (parentDemo != null) {
                        DisplayDemo(parentDemo, null, onNavigateTo, onNavigateBack)
                    }
                } else {
                    demo.content(onNavigateBack)
                }
            }
        }
        is DemoCategory -> {
            DisplayDemoList(demo, parentDemo, onNavigateTo, onNavigateBack)
        }
    }
}

@Composable
@ExperimentalWearMaterialApi
internal fun DisplayDemoList(
    category: DemoCategory,
    parentDemo: Demo?,
    onNavigateTo: (Demo) -> Unit,
    onNavigateBack: () -> Unit
) {
    SwipeToDismissBox(
        state = swipeDismissStateWithNavigation(onNavigateBack),
        hasBackground = parentDemo != null,
        backgroundKey = parentDemo?.title ?: SwipeToDismissBoxDefaults.BackgroundKey,
        contentKey = category.title,
    ) { isBackground ->
        if (isBackground) {
            if (parentDemo != null) {
                DisplayDemo(parentDemo, null, onNavigateTo, onNavigateBack)
            }
        } else {
            ScalingLazyColumnWithRSB(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().testTag(DemoListTag),
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
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (demo.description != null) {
                        item {
                            CompositionLocalProvider(
                                LocalTextStyle provides MaterialTheme.typography.caption3
                            ) {
                                Text(
                                    text = demo.description,
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
}

@Composable
@ExperimentalWearMaterialApi
internal fun swipeDismissStateWithNavigation(
    onNavigateBack: () -> Unit
): SwipeToDismissBoxState {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeDismissTarget.Dismissal) {
            state.snapTo(SwipeDismissTarget.Original)
            onNavigateBack()
        }
    }
    return state
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ScalingLazyColumnWithRSB(
    modifier: Modifier = Modifier,
    state: ScalingLazyListState = rememberScalingLazyListState(),
    scalingParams: ScalingParams = ScalingLazyColumnDefaults.scalingParams(),
    reverseLayout: Boolean = false,
    snap: Boolean = true,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: ScalingLazyListScope.() -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    ScalingLazyColumn(
        modifier = modifier
            .onRotaryScrollEvent {
                coroutineScope.launch {
                    state.dispatchRawDelta(it.verticalScrollPixels)
                }
                true
            }
            .focusRequester(focusRequester)
            .focusable(),
        state = state,
        reverseLayout = reverseLayout,
        scalingParams = scalingParams,
        flingBehavior = if (snap) ScalingLazyColumnDefaults.snapFlingBehavior(
            state = state
        ) else ScrollableDefaults.flingBehavior(),
        horizontalAlignment = horizontalAlignment,
        content = content
    )
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
