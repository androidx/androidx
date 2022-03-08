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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.SwipeDismissTarget
import androidx.wear.compose.material.SwipeToDismissBox
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberSwipeToDismissBoxState

/**
 * SwipeToDismiss demo - manages its own navigation between a List screen and a Detail screen,
 * using SwipeToDismissBox to recognise the swipe gesture and navigate backwards.
 * During the swipe gesture, a background is displayed that shows the previous screen.
 * Uses LaunchedEffect to reset the offset of the swipe by snapping back to original position.
 */
@Composable
fun SwipeToDismissDemo(
    navigateBack: () -> Unit,
    demoState: MutableState<SwipeDismissDemoState>,
) {
    val swipeDismissState = rememberSwipeToDismissBoxState()
    LaunchedEffect(swipeDismissState.currentValue) {
        if (swipeDismissState.currentValue == SwipeDismissTarget.Dismissal) {
            // Swipe has been completely dismissed because the current value is the
            // 'dismiss' target. Navigate and snap back to original position.
            when (demoState.value) {
                SwipeDismissDemoState.List -> {
                    navigateBack()
                }
                SwipeDismissDemoState.Detail -> {
                    demoState.value = SwipeDismissDemoState.List
                }
            }
            swipeDismissState.snapTo(SwipeDismissTarget.Original)
        }
    }

    SwipeToDismissBox(
        state = swipeDismissState,
    ) { isBackground ->
        if (isBackground) {
            // What to show behind the content whilst swiping.
            when (demoState.value) {
                SwipeDismissDemoState.List -> {
                    DisplayDemoList(SwipeToDismissDemos, null, {}, {})
                }
                SwipeDismissDemoState.Detail -> {
                    SwipeToDismissOptionsList()
                }
            }
        } else {
            when (demoState.value) {
                SwipeDismissDemoState.List -> SwipeToDismissOptionsList(demoState)
                SwipeDismissDemoState.Detail -> SwipeToDismissDetail()
            }
        }
    }
}

enum class SwipeDismissDemoState {
    List,
    Detail,
}

@Composable
private fun SwipeToDismissOptionsList(
    state: MutableState<SwipeDismissDemoState>? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .verticalScroll(
                rememberScrollState()
            ),
        verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(4) {
            Chip(
                onClick = { state?.value = SwipeDismissDemoState.Detail },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Click me") },
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Composable
private fun SwipeToDismissDetail() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 25.dp)
    ) {
        Text(text = "Swipe Dismiss Demo Detail", textAlign = TextAlign.Center)
        Text(
            text = "Start swiping to reveal the previous level menu. " +
                "Complete the swipe to " +
                "dismiss this screen."
        )
    }
}
