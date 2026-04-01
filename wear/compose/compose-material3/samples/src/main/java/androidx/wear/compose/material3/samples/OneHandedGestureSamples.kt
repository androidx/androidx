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

package androidx.wear.compose.material3.samples

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.annotation.Sampled
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.GesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture

@Sampled
@Composable
fun OneHandedGestureButtonSample() {
    var label by remember { mutableStateOf("Gesturable Button") }
    val onClick = remember { { label = "Clicked/Gestured" } }
    var gestureIndicatorVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    action = GestureAction.Primary,
                    interactionSource = interactionSource,
                    onShowIndicator = { gestureIndicatorVisible = true },
                    onGesture = onClick,
                ),
        ) {
            OneHandedGestureDefaults.GestureIndicator(
                gestureIndicatorVisible,
                { gestureIndicatorVisible = false },
            ) {
                Text(label)
            }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureTransformingLazyColumnSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val tlcState = rememberTransformingLazyColumnState()
    var scrollGestureIndicatorVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    ScreenScaffold(
        scrollState = tlcState,
        edgeButton = {
            var buttonGestureIndicatorVisible by remember { mutableStateOf(false) }
            EdgeButton(
                onClick = onClick,
                interactionSource = interactionSource,
                modifier =
                    if (tlcState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Clickable,
                            interactionSource = interactionSource,
                            onShowIndicator = { buttonGestureIndicatorVisible = true },
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            tlcState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureDefaults.GestureIndicator(
                    buttonGestureIndicatorVisible,
                    { buttonGestureIndicatorVisible = false },
                ) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureDefaults.ScrollGestureIndicator(
                scrollGestureIndicatorVisible,
                onGestureIndicatorFinished = { scrollGestureIndicatorVisible = false },
                tlcState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = tlcState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Scrollable,
                        onGesture = { OneHandedGestureDefaults.scrollDown(tlcState) },
                        onShowIndicator = { scrollGestureIndicatorVisible = true },
                    ),
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureHorizontalPagerSample() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    var pageGestureIndicatorVisible by remember { mutableStateOf(false) }

    HorizontalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            OneHandedGestureDefaults.HorizontalPageGestureIndicator(
                pagerState = pagerState,
                gestureIndicatorVisible = pageGestureIndicatorVisible,
                onGestureIndicatorFinished = { pageGestureIndicatorVisible = false },
            )
        },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier.oneHandedGesture(
                    action = GestureAction.Primary,
                    onShowIndicator = { pageGestureIndicatorVisible = true },
                ) {
                    OneHandedGestureDefaults.scrollToNextPage(pagerState)
                },
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                ScreenScaffold {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "Page #$page")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Swipe left and right")
                    }
                }
            }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureVerticalPagerSample() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    var pageGestureIndicatorVisible by remember { mutableStateOf(false) }

    VerticalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            OneHandedGestureDefaults.VerticalPageGestureIndicator(
                pagerState = pagerState,
                gestureIndicatorVisible = pageGestureIndicatorVisible,
                onGestureIndicatorFinished = { pageGestureIndicatorVisible = false },
            )
        },
    ) {
        VerticalPager(
            state = pagerState,
            modifier =
                Modifier.oneHandedGesture(
                    action = GestureAction.Primary,
                    onShowIndicator = { pageGestureIndicatorVisible = true },
                ) {
                    OneHandedGestureDefaults.scrollToNextPage(pagerState)
                },
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pagerState) {
                ScreenScaffold {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(text = "Page #$page")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Swipe up and down")
                    }
                }
            }
        }
    }
}
