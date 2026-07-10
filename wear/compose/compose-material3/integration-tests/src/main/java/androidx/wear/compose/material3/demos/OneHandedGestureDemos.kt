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

package androidx.wear.compose.material3.demos

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onVisibilityChanged
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.integration.demos.common.ActivityDemo
import androidx.wear.compose.integration.demos.common.ComposableDemo
import androidx.wear.compose.integration.demos.common.Material3DemoCategory
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ChildButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.GesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicatorState
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.material3.onehandedgesture.rememberOneHandedGestureConfiguration
import androidx.wear.compose.material3.samples.AppCardContentWithOneHandedGestureSample
import androidx.wear.compose.material3.samples.ButtonContentWithOneHandedGestureSample
import androidx.wear.compose.material3.samples.CompactButtonContentWithOneHandedGestureSample
import androidx.wear.compose.material3.samples.OneHandedGestureButtonSample
import androidx.wear.compose.material3.samples.OneHandedGestureDisableButtonSample
import androidx.wear.compose.material3.samples.OneHandedGestureHorizontalPagerSample
import androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnSample
import androidx.wear.compose.material3.samples.OneHandedGestureScalingLazyColumnScrollToNextItemSample
import androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnSample
import androidx.wear.compose.material3.samples.OneHandedGestureTransformingLazyColumnScrollToNextItemSample
import androidx.wear.compose.material3.samples.OneHandedGestureVerticalPagerSample
import androidx.wear.compose.material3.samples.TitleCardContentWithOneHandedGestureSample
import androidx.wear.compose.material3.samples.icons.FavoriteIcon
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

val OneHandedGestureDemos =
    listOf(
        ComposableDemo("Button") { OneHandedGestureButtonSample() },
        ComposableDemo("Enable/Disable Gestures") { OneHandedGestureDisableButtonSample() },
        ComposableDemo("TLC scrollDown with EdgeButton") {
            OneHandedGestureTransformingLazyColumnSample()
        },
        ComposableDemo("SLC scrollDown with EdgeButton") {
            OneHandedGestureScalingLazyColumnSample()
        },
        ComposableDemo("TLC scrollToNextItem with EdgeButton") {
            OneHandedGestureTransformingLazyColumnScrollToNextItemSample()
        },
        ComposableDemo("SLC scrollToNextItem with EdgeButton") {
            OneHandedGestureScalingLazyColumnScrollToNextItemSample()
        },
        ComposableDemo("TransformingLazyColumn with Button") {
            OneHandedGestureTransformingLazyColumnWithButtonDemo()
        },
        ComposableDemo("Horizontal Pager") { OneHandedGestureHorizontalPagerSample() },
        ComposableDemo("Vertical Pager") { OneHandedGestureVerticalPagerSample() },
        ComposableDemo("Two Buttons with the same priority") {
            OneHandedGestureTwoButtonsSamePriorityDemo()
        },
        ComposableDemo("Primary/Dismiss Buttons") { OneHandedGesturePrimaryDismissButtons() },
        ActivityDemo(
            "SwipeDismissableNavHost",
            OneHandedGestureSwipeDismissableNavHostDemoActivity::class,
        ),
        Material3DemoCategory(
            "Multi-slot Cards",
            listOf(
                ComposableDemo("App Card") { AppCardContentWithOneHandedGestureSample() },
                ComposableDemo("Title Card") { TitleCardContentWithOneHandedGestureSample() },
            ),
        ),
        Material3DemoCategory(
            "Multi-slot Buttons",
            listOf(
                ComposableDemo("Filled Button") { ButtonContentWithOneHandedGestureSample() },
                ComposableDemo("Filled Tonal Button") { OHGTonalButtonDemo() },
                ComposableDemo("Outlined Button") { OHGOutlinedButtonDemo() },
                ComposableDemo("Child Button") { OHGChildButtonDemo() },
                ComposableDemo("Compact Button") {
                    CompactButtonContentWithOneHandedGestureSample()
                },
            ),
        ),
    )

@Composable
fun OneHandedGestureTwoButtonsSamePriorityDemo() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(2) { idx ->
            var label by remember { mutableStateOf("Gesturable Button $idx") }

            OneHandedGestureButton(
                gestureLabel = "activate the button",
                onClick = { label = "Clicked/Gestured $idx" },
            ) {
                Text(label)
            }
        }
    }
}

@Composable
fun OneHandedGesturePrimaryDismissButtons() {
    var primaryLabel by remember { mutableStateOf("Confirm") }
    val primaryOnClick = { primaryLabel = "Confirmed" }
    val primaryInteractionSource = remember { MutableInteractionSource() }

    var dismissLabel by remember { mutableStateOf("Dismiss") }
    val dismissOnClick = { dismissLabel = "Dismissed" }
    val dismissInteractionSource = remember { MutableInteractionSource() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Both gestures")
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val primaryGestureConfig =
                    rememberOneHandedGestureConfiguration(action = GestureAction.Primary)
                val primaryIndicatorState = remember { OneHandedGestureIndicatorState() }
                val dismissGestureConfig =
                    rememberOneHandedGestureConfiguration(action = GestureAction.Dismiss)
                val dismissIndicatorState = remember { OneHandedGestureIndicatorState() }
                Button(
                    onClick = dismissOnClick,
                    interactionSource = dismissInteractionSource,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = dismissGestureConfig,
                            interactionSource = dismissInteractionSource,
                            onGestureLabel = "dismiss",
                            onGestureAvailable = { dismissIndicatorState.isIndicatorActive = true },
                            onGesture = dismissOnClick,
                        ),
                ) {
                    OneHandedGestureIndicator(dismissGestureConfig, dismissIndicatorState) {
                        Text(dismissLabel)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = primaryOnClick,
                    interactionSource = primaryInteractionSource,
                    modifier =
                        Modifier.oneHandedGesture(
                            gestureConfiguration = primaryGestureConfig,
                            interactionSource = primaryInteractionSource,
                            onGestureLabel = "confirm",
                            onGestureAvailable = { primaryIndicatorState.isIndicatorActive = true },
                            onGesture = primaryOnClick,
                        ),
                ) {
                    OneHandedGestureIndicator(primaryGestureConfig, primaryIndicatorState) {
                        Text(primaryLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun OneHandedGestureSwipeDismissableNavHostDemo() {
    val navController = rememberSwipeDismissableNavController()
    SwipeDismissableNavHost(navController = navController, startDestination = "first") {
        composable("first") {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("First screen")
                Spacer(Modifier.height(4.dp))
                OneHandedGestureButton(
                    gestureLabel = "move to the second screen",
                    onClick = { navController.navigate("second") },
                ) {
                    Text("Go to Second screen")
                }
            }
        }
        composable("second") {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Second screen")
                Spacer(Modifier.height(4.dp))
                OneHandedGestureButton(
                    gestureLabel = "move to the first screen",
                    onClick = { navController.popBackStack() },
                ) {
                    Text("Go to Previous screen")
                }
            }
        }
    }
}

@Composable
fun OneHandedGestureTransformingLazyColumnWithButtonDemo() {
    var buttonText by remember { mutableStateOf("Gesture me") }
    val onClick = { buttonText = "Gestured" }
    val scrollState = rememberTransformingLazyColumnState()
    val scrollGestureConfig =
        rememberOneHandedGestureConfiguration(
            action = GestureAction.Primary,
            priority = GesturePriority.Scrollable,
        )
    val scrollIndicatorState = remember { OneHandedGestureIndicatorState() }
    ScreenScaffold(
        scrollState = scrollState,
        scrollIndicator = {
            OneHandedGestureScrollIndicator(
                gestureConfiguration = scrollGestureConfig,
                indicatorState = scrollIndicatorState,
                scrollState = scrollState,
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
                        gestureConfiguration = scrollGestureConfig,
                        onGestureLabel = "scroll",
                        onGestureAvailable = { scrollIndicatorState.isIndicatorActive = true },
                        onGesture = { OneHandedGestureDefaults.scrollDown(scrollState) },
                    ),
        ) {
            items(10) { Text("Item $it") }
            item {
                var buttonVisible by remember { mutableStateOf(false) }
                val buttonInteractionSource = remember { MutableInteractionSource() }
                val buttonGestureSpec =
                    rememberOneHandedGestureConfiguration(
                        action = GestureAction.Primary,
                        priority = GesturePriority.Clickable,
                    )
                val buttonIndicatorState = remember { OneHandedGestureIndicatorState() }
                Button(
                    onClick = onClick,
                    interactionSource = buttonInteractionSource,
                    modifier =
                        Modifier.onVisibilityChanged { buttonVisible = it } then
                            if (buttonVisible) {
                                // Apply the one-handed gesture modifier only when the button is
                                // visible
                                Modifier.oneHandedGesture(
                                    gestureConfiguration = buttonGestureSpec,
                                    interactionSource = buttonInteractionSource,
                                    onGestureLabel = "click",
                                    onGestureAvailable = {
                                        buttonIndicatorState.isIndicatorActive = true
                                    },
                                    onGesture = onClick,
                                )
                            } else {
                                Modifier
                            },
                ) {
                    OneHandedGestureIndicator(buttonGestureSpec, buttonIndicatorState) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
private fun OneHandedGestureButton(
    gestureLabel: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig = rememberOneHandedGestureConfiguration(action = GestureAction.Primary)
    val indicatorState = remember { OneHandedGestureIndicatorState() }
    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier =
            Modifier.oneHandedGesture(
                gestureConfiguration = gestureConfig,
                interactionSource = interactionSource,
                onGestureLabel = gestureLabel,
                onGestureAvailable = { indicatorState.isIndicatorActive = true },
                onGesture = onClick,
            ),
    ) {
        OneHandedGestureIndicator(
            gestureConfiguration = gestureConfig,
            indicatorState = indicatorState,
            content = content,
        )
    }
}

@Composable
fun OHGTonalButtonDemo() {
    var label by remember { mutableStateOf("Tonal Button") }
    val onClick = { label = "Gestured" }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig = rememberOneHandedGestureConfiguration(action = GestureAction.Primary)
    val indicatorState = remember { OneHandedGestureIndicatorState() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        FilledTonalButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "click",
                    interactionSource = interactionSource,
                    onGestureAvailable = { indicatorState.isIndicatorActive = true },
                    onGesture = onClick,
                ),
        ) {
            OneHandedGestureIndicator(
                gestureConfiguration = gestureConfig,
                indicatorState = indicatorState,
                gestureIndicatorTint = MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                ButtonDefaults.Content(
                    secondaryLabel = { Text("Secondary Label") },
                    icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                    colors = ButtonDefaults.filledTonalButtonColors(),
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
fun OHGOutlinedButtonDemo() {
    var label by remember { mutableStateOf("Outlined Button") }
    val onClick = { label = "Gestured" }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig = rememberOneHandedGestureConfiguration(action = GestureAction.Primary)
    val indicatorState = remember { OneHandedGestureIndicatorState() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        OutlinedButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "click",
                    interactionSource = interactionSource,
                    onGestureAvailable = { indicatorState.isIndicatorActive = true },
                    onGesture = onClick,
                ),
        ) {
            OneHandedGestureIndicator(
                gestureConfiguration = gestureConfig,
                indicatorState = indicatorState,
                gestureIndicatorTint = MaterialTheme.colorScheme.primary,
            ) {
                ButtonDefaults.Content(
                    secondaryLabel = { Text("Secondary Label") },
                    icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                    colors = ButtonDefaults.outlinedButtonColors(),
                    label = { Text(label) },
                )
            }
        }
    }
}

@Composable
fun OHGChildButtonDemo() {
    var label by remember { mutableStateOf("Child Button") }
    val onClick = { label = "Gestured" }
    val interactionSource = remember { MutableInteractionSource() }
    val gestureConfig = rememberOneHandedGestureConfiguration(action = GestureAction.Primary)
    val indicatorState = remember { OneHandedGestureIndicatorState() }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ChildButton(
            onClick = onClick,
            interactionSource = interactionSource,
            modifier =
                Modifier.oneHandedGesture(
                    gestureConfiguration = gestureConfig,
                    onGestureLabel = "click",
                    interactionSource = interactionSource,
                    onGestureAvailable = { indicatorState.isIndicatorActive = true },
                    onGesture = onClick,
                ),
        ) {
            OneHandedGestureIndicator(
                gestureConfiguration = gestureConfig,
                indicatorState = indicatorState,
                gestureIndicatorTint = MaterialTheme.colorScheme.onSurface,
            ) {
                ButtonDefaults.Content(
                    secondaryLabel = { Text("Secondary Label") },
                    icon = { FavoriteIcon(ButtonDefaults.IconSize) },
                    colors = ButtonDefaults.childButtonColors(),
                    label = { Text(label) },
                )
            }
        }
    }
}
