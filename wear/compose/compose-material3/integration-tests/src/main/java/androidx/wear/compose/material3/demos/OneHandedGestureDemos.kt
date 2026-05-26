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
import androidx.compose.foundation.layout.BoxScope
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
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.GesturePriority
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureIndicator
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureScrollIndicator
import androidx.wear.compose.material3.onehandedgesture.oneHandedGesture
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

@Composable
fun OneHandedGestureTwoButtonsSamePriorityDemo() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        repeat(2) { idx ->
            var label by remember { mutableStateOf("Gesturable Button $idx") }

            OneHandedGestureButton(onClick = { label = "Clicked/Gestured $idx" }) { Text(label) }
        }
    }
}

@Composable
fun OneHandedGesturePrimaryDismissButtons() {
    var primaryLabel by remember { mutableStateOf("Confirm") }
    val primaryOnClick = remember { { primaryLabel = "Confirmed" } }
    val primaryInteractionSource = remember { MutableInteractionSource() }

    var dismissLabel by remember { mutableStateOf("Dismiss") }
    val dismissOnClick = remember { { dismissLabel = "Dismissed" } }
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
                Button(
                    onClick = dismissOnClick,
                    interactionSource = dismissInteractionSource,
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Dismiss,
                            interactionSource = dismissInteractionSource,
                            onGesture = dismissOnClick,
                        ),
                ) {
                    OneHandedGestureIndicator(interactionSource = dismissInteractionSource) {
                        Text(dismissLabel)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = primaryOnClick,
                    interactionSource = primaryInteractionSource,
                    modifier =
                        Modifier.oneHandedGesture(
                            action = GestureAction.Primary,
                            interactionSource = primaryInteractionSource,
                            onGesture = primaryOnClick,
                        ),
                ) {
                    OneHandedGestureIndicator(interactionSource = primaryInteractionSource) {
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
                OneHandedGestureButton(onClick = { navController.navigate("second") }) {
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
                OneHandedGestureButton(onClick = { navController.popBackStack() }) {
                    Text("Go to Previous screen")
                }
            }
        }
    }
}

@Composable
fun OneHandedGestureTransformingLazyColumnWithButtonDemo() {
    var buttonText by remember { mutableStateOf("Gesture me") }
    val onClick = remember { { buttonText = "Gestured" } }
    val scrollState = rememberTransformingLazyColumnState()
    val scrollInteractionSource = remember { MutableInteractionSource() }

    ScreenScaffold(
        scrollState = scrollState,
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
                        onGesture = { OneHandedGestureDefaults.scrollDown(scrollState) },
                    ),
        ) {
            items(10) { Text("Item $it") }
            item {
                var buttonVisible by remember { mutableStateOf(false) }
                val buttonInteractionSource = remember { MutableInteractionSource() }
                Button(
                    onClick = onClick,
                    interactionSource = buttonInteractionSource,
                    modifier =
                        Modifier.onVisibilityChanged { buttonVisible = it } then
                            if (buttonVisible) {
                                // Apply the one-handed gesture modifier only when the button is
                                // visible
                                Modifier.oneHandedGesture(
                                    action = GestureAction.Primary,
                                    priority = GesturePriority.Clickable,
                                    interactionSource = buttonInteractionSource,
                                    onGesture = onClick,
                                )
                            } else {
                                Modifier
                            },
                ) {
                    OneHandedGestureIndicator(interactionSource = buttonInteractionSource) {
                        Text(buttonText)
                    }
                }
            }
        }
    }
}

@Composable
private fun OneHandedGestureButton(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }

    Button(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier =
            Modifier.oneHandedGesture(
                action = GestureAction.Primary,
                interactionSource = interactionSource,
                onGesture = onClick,
            ),
    ) {
        OneHandedGestureIndicator(interactionSource = interactionSource, content = content)
    }
}
