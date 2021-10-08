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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

@Composable
@ExperimentalWearMaterialApi
fun SwipeToDismissBoxWithState(
    quit: () -> Unit
) {
    // This sample flips between 2 stateful composables when you swipe:
    //   1) ToggleScreen - displays a ToggleButton that alternates between On/Off
    //   2) CounterScreen - displays a counter Button that increments when clicked
    // We use SaveableStateHolder, keys and rememberSaveable to ensure that the state is
    // persisted correctly even though each composable may be shown either in the foreground
    // or in the background during the swipe gesture.
    val showCounterForContent = remember { mutableStateOf(true) }
    val state = rememberSwipeToDismissBoxState()
    val saveableStateHolder = rememberSaveableStateHolder()
    val toggleKey = "Toggle"
    val counterKey = "Counter"
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeDismissTarget.Dismissal) {
            showCounterForContent.value = !showCounterForContent.value
            state.snapTo(SwipeDismissTarget.Original)
        }
    }
    SwipeToDismissBox(
        state = state,
        backgroundKey = if (showCounterForContent.value) toggleKey else counterKey,
        contentKey = if (showCounterForContent.value) counterKey else toggleKey,
        content = { isBackground ->
            if (showCounterForContent.value xor isBackground)
                saveableStateHolder.SaveableStateProvider(counterKey) {
                    var counter by rememberSaveable { mutableStateOf(0) }
                    Column(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.primary),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Button(
                            onClick = { ++counter },
                        ) {
                            Text(text = "" + counter)
                        }
                        Button(onClick = quit) { Text(text = "Quit") }
                    }
                }
            else
                saveableStateHolder.SaveableStateProvider(toggleKey) {
                    var toggle by rememberSaveable { mutableStateOf(false) }
                    Column(
                        modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.primary),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        ToggleButton(
                            checked = toggle,
                            onCheckedChange = { toggle = !toggle },
                            content = { Text(text = if (toggle) "On" else "Off") },
                        )
                        Button(onClick = quit) { Text(text = "Quit") }
                    }
                }
        }
    )
}
