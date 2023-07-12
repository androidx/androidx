/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.expandableItem
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.rememberExpandableState
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.ListHeader
import androidx.wear.compose.material.Text
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearFoundationApi::class)
@Sampled
@Composable
fun SwipeToRevealSample() {
    SwipeToReveal(
        action = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { /* Add the primary action */ },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete"
                )
            }
        },
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Add undo action here */ },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Undo") }
            )
        }
    ) {
        Chip(
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* the click action associated with chip */ },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(text = "Swipe Me") }
        )
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Sampled
@Composable
fun SwipeToRevealWithRevealOffset() {
    val state = rememberRevealState()
    SwipeToReveal(
        state = state,
        action = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { /* Add the primary action */ },
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "Delete"
                )
                if (abs(state.offset) > revealOffset) {
                    Spacer(Modifier.size(5.dp))
                    Text("Clear")
                }
            }
        },
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { /* Add undo action here */ },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Undo") }
            )
        }
    ) {
        Chip(
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* the click action associated with chip */ },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(text = "Swipe Me") }
        )
    }
}

/**
 * A sample on how to use Swipe To Reveal within a list of items, preferably [ScalingLazyColumn].
 */
@OptIn(ExperimentalWearFoundationApi::class)
@Sampled
@Composable
fun SwipeToRevealWithExpandables() {
    // Shape of actions should match with the overlay content. For example, Chips
    // should use RoundedCornerShape(CornerSize(percent = 50)), Cards should use
    // RoundedCornerShape with appropriate radius, based on the theme.
    val actionShape = RoundedCornerShape(corner = CornerSize(percent = 50))
    val itemCount = 10
    val coroutineScope = rememberCoroutineScope()
    val expandableStates = List(itemCount) {
        rememberExpandableState(initiallyExpanded = true)
    }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            ListHeader {
                Text("Scaling Lazy Column")
            }
        }
        repeat(itemCount) { current ->
            expandableItem(
                state = expandableStates[current],
            ) { isExpanded ->
                val revealState = rememberRevealState()
                if (isExpanded) {
                    SwipeToReveal(
                        state = revealState,
                        action = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Red, actionShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            revealState.animateTo(RevealValue.Revealed)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete"
                                )
                            }
                        },
                        additionalAction = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Gray, actionShape)
                                    .clickable { /* trigger the optional action */ },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.MoreVert,
                                    contentDescription = "More Options"
                                )
                            }
                        },
                        undoAction = {
                            Chip(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    coroutineScope.launch {
                                        revealState.animateTo(RevealValue.Covered)
                                    }
                                },
                                colors = ChipDefaults.secondaryChipColors(),
                                label = { Text(text = "Undo") }
                            )
                        },
                        onFullSwipe = {
                            coroutineScope.launch {
                                delay(1000)
                                expandableStates[current].expanded = false
                            }
                        }
                    ) {
                        Chip(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { /* the click action associated with chip */ },
                            colors = ChipDefaults.secondaryChipColors(),
                            label = { Text(text = "Swipe Me") }
                        )
                    }
                }
            }
        }
    }
}