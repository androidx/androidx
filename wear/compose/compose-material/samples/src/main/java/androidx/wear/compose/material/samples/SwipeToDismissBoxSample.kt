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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.SplitToggleChip
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

@Sampled
@Composable
@ExperimentalWearMaterialApi
fun SimpleSwipeToDismissBox(
    navigateBack: () -> Unit
) {
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeDismissTarget.Dismissal) {
            state.snapTo(SwipeDismissTarget.Original)
            navigateBack()
        }
    }
    SwipeToDismissBox(
        state = state,
    ) { isBackground ->
        if (isBackground) {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.secondaryVariant))
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.primary),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Swipe right to dismiss", color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}

@Sampled
@Composable
@ExperimentalWearMaterialApi
fun StatefulSwipeToDismissBox() {
    // State for managing a 2-level navigation hierarchy between
    // MainScreen and ItemScreen composables.
    // Alternatively, use SwipeDismissableNavHost from wear.compose.navigation.
    var showMainScreen by remember { mutableStateOf(true) }
    val saveableStateHolder = rememberSaveableStateHolder()

    // Swipe gesture dismisses ItemScreen to return to MainScreen.
    val state = rememberSwipeToDismissBoxState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == SwipeDismissTarget.Dismissal) {
            state.snapTo(SwipeDismissTarget.Original)
            showMainScreen = !showMainScreen
        }
    }

    // Hierarchy is ListScreen -> ItemScreen, so we show ListScreen as the background behind
    // the ItemScreen, otherwise there's no background to show.
    SwipeToDismissBox(
        state = state,
        hasBackground = !showMainScreen,
        backgroundKey = if (!showMainScreen) "MainKey" else "Background",
        contentKey = if (showMainScreen) "MainKey" else "ItemKey",
    ) { isBackground ->
        if (isBackground || showMainScreen) {
            // Best practice would be to use State Hoisting and leave this composable stateless.
            // Here, we want to support MainScreen being shown from different destinations
            // (either in the foreground or in the background during swiping) - that can be achieved
            // using SaveableStateHolder and rememberSaveable as shown below.
            saveableStateHolder.SaveableStateProvider(
                key = "MainKey",
                content = {
                    // Composable that maintains its own state
                    // and can be shown in foreground or background.
                    val checked = rememberSaveable { mutableStateOf(true) }
                    Column(
                        modifier = Modifier
                            .fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement =
                        Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
                    ) {
                        SplitToggleChip(
                            checked = checked.value,
                            label = { Text("Item details") },
                            modifier = Modifier.height(40.dp),
                            onCheckedChange = { v -> checked.value = v },
                            onClick = { showMainScreen = false }
                        )
                    }
                }
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colors.primary),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("Show details here...", color = MaterialTheme.colors.onPrimary)
                Text("Swipe right to dismiss", color = MaterialTheme.colors.onPrimary)
            }
        }
    }
}
