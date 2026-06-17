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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.VerticalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SwitchButton
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.VerticalPagerScaffold
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.GesturePriority
import androidx.wear.compose.material3.onehandedgesture.LocalOneHandedGestureEnabled
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureHorizontalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureVerticalPageIndicator
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture

@Sampled
@Composable
fun OneHandedGestureButtonSample() {
    var label by remember { mutableStateOf("Gesturable Button") }
    val onClick = remember { { label = "Clicked/Gestured" } }
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    action = GestureAction.Primary,
                    interactionSource = interactionSource,
                    gestureLabel = "activate the button",
                    onGesture = onClick,
                ),
        ) {
            OneHandedGestureIndicator(interactionSource = interactionSource) { Text(label) }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureDisableButtonSample() {
    var counter by remember { mutableIntStateOf(0) }
    var enabled by remember { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SwitchButton(checked = enabled, onCheckedChange = { enabled = it }) {
                Text("Gestures enabled")
            }
            Spacer(modifier = Modifier.height(6.dp))
            CompositionLocalProvider(LocalOneHandedGestureEnabled provides enabled) {
                Button(
                    onClick = {},
                    interactionSource = interactionSource,
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            interactionSource = interactionSource,
                            gestureLabel = "increase the counter",
                            onGesture = { counter++ },
                        ),
                ) {
                    OneHandedGestureIndicator(interactionSource = interactionSource) {
                        Text("Gestured $counter times")
                    }
                }
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
    val scrollState = rememberTransformingLazyColumnState()
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val scrollInteractionSource = remember { MutableInteractionSource() }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (scrollState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Clickable,
                            interactionSource = buttonInteractionSource,
                            gestureLabel = "close",
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = scrollState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureIndicator(interactionSource = buttonInteractionSource) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                interactionSource = scrollInteractionSource,
                state = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Scrollable,
                        interactionSource = scrollInteractionSource,
                        gestureLabel = "scroll",
                        onGesture = { OneHandedGestureDefaults.scrollDown(scrollState) },
                    ),
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureScalingLazyColumnSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val slcState = rememberScalingLazyListState()
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val slcInteractionSource = remember { MutableInteractionSource() }

    ScreenScaffold(
        scrollState = slcState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (slcState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Clickable,
                            interactionSource = buttonInteractionSource,
                            gestureLabel = "close",
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = slcState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureIndicator(interactionSource = buttonInteractionSource) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                interactionSource = slcInteractionSource,
                state = slcState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        ScalingLazyColumn(
            state = slcState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Scrollable,
                        interactionSource = slcInteractionSource,
                        gestureLabel = "scroll",
                        onGesture = { OneHandedGestureDefaults.scrollDown(slcState) },
                    ),
            autoCentering = null,
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureTransformingLazyColumnScrollToNextItemSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val scrollState = rememberTransformingLazyColumnState()
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val scrollInteractionSource = remember { MutableInteractionSource() }

    ScreenScaffold(
        scrollState = scrollState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (scrollState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Clickable,
                            interactionSource = buttonInteractionSource,
                            gestureLabel = "close",
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = scrollState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureIndicator(interactionSource = buttonInteractionSource) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                interactionSource = scrollInteractionSource,
                state = scrollState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        TransformingLazyColumn(
            state = scrollState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Scrollable,
                        interactionSource = scrollInteractionSource,
                        onGesture = { OneHandedGestureDefaults.scrollDownToNextItem(scrollState) },
                    ),
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureScalingLazyColumnScrollToNextItemSample() {
    val backDispatcherOwner = LocalOnBackPressedDispatcherOwner.current
    val onClick =
        remember<() -> Unit> { { backDispatcherOwner?.onBackPressedDispatcher?.onBackPressed() } }
    val slcState = rememberScalingLazyListState()
    val buttonInteractionSource = remember { MutableInteractionSource() }
    val slcInteractionSource = remember { MutableInteractionSource() }

    ScreenScaffold(
        scrollState = slcState,
        edgeButton = {
            EdgeButton(
                onClick = onClick,
                interactionSource = buttonInteractionSource,
                modifier =
                    if (slcState.canScrollForward) {
                        Modifier
                    } else {
                        // Apply the one-handed gesture modifier only when the container cannot
                        // scroll further, ensuring the EdgeButton is fully visible and interactive
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            priority = GesturePriority.Clickable,
                            interactionSource = buttonInteractionSource,
                            gestureLabel = "close",
                            onGesture = onClick,
                        )
                    } then
                        Modifier.scrollable(
                            state = slcState,
                            orientation = Orientation.Vertical,
                            reverseDirection = true,
                            overscrollEffect = rememberOverscrollEffect(),
                        ),
            ) {
                OneHandedGestureIndicator(interactionSource = buttonInteractionSource) {
                    Text("Close")
                }
            }
        },
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                interactionSource = slcInteractionSource,
                state = slcState,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        },
    ) { contentPadding ->
        ScalingLazyColumn(
            state = slcState,
            contentPadding = contentPadding,
            modifier =
                Modifier.fillMaxSize()
                    .oneHandedGesture(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Scrollable,
                        interactionSource = slcInteractionSource,
                        onGesture = { OneHandedGestureDefaults.scrollDownToNextItem(slcState) },
                    ),
            autoCentering = null,
        ) {
            items(10) { Text("Item $it") }
        }
    }
}

@Sampled
@Composable
fun OneHandedGestureHorizontalPagerSample() {
    val pagerState = rememberPagerState(pageCount = { 10 })
    val interactionSource = remember { MutableInteractionSource() }

    HorizontalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            OneHandedGestureHorizontalPageIndicator(
                interactionSource = interactionSource,
                pagerState = pagerState,
            )
        },
    ) {
        HorizontalPager(
            state = pagerState,
            modifier =
                Modifier.oneHandedGesture(
                    action = GestureAction.Primary,
                    interactionSource = interactionSource,
                    gestureLabel = "scroll to the next page",
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
    val interactionSource = remember { MutableInteractionSource() }

    VerticalPagerScaffold(
        pagerState = pagerState,
        pageIndicator = {
            OneHandedGestureVerticalPageIndicator(
                interactionSource = interactionSource,
                pagerState = pagerState,
            )
        },
    ) {
        VerticalPager(
            state = pagerState,
            modifier =
                Modifier.oneHandedGesture(
                    action = GestureAction.Primary,
                    interactionSource = interactionSource,
                    gestureLabel = "scroll to the next page",
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
