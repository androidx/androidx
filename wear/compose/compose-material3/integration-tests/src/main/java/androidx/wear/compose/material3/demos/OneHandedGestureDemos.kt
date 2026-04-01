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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.onehandedgesture.GestureAction
import androidx.wear.compose.material3.onehandedgesture.OneHandedGestureDefaults
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
private fun OneHandedGestureButton(onClick: () -> Unit, content: @Composable RowScope.() -> Unit) {
    var gestureIndicatorVisible by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

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
            onGestureIndicatorFinished = { gestureIndicatorVisible = false },
        ) {
            content()
        }
    }
}
