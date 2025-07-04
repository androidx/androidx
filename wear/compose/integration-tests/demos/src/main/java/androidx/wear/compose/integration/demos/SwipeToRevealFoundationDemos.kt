/*
 * Copyright 2025 The Android Open Source Project
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

@file:OptIn(ExperimentalWearFoundationApi::class)
@file:Suppress("DEPRECATION")

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealDirection
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.createRevealAnchors
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch

/*
 * This demos how swipe to reveal for both directions can be done using just the Foundation layer,
 * but we would expect developers to use the Material or Material3 layers which make the task
 * easier.
 */
@Composable
fun SwipeToRevealDemoBothDirections() {
    val revealState =
        rememberRevealState(anchors = createRevealAnchors(revealDirection = RevealDirection.Both))
    val coroutineScope = rememberCoroutineScope()
    SwipeToReveal(
        primaryAction = {
            Box(
                modifier =
                    Modifier.fillMaxSize().clickable {
                        /* Add the primary action */
                        coroutineScope.launch {
                            if (revealState.currentValue == RevealValue.LeftRevealing) {
                                revealState.animateTo(RevealValue.LeftRevealed)
                            } else {
                                revealState.animateTo(RevealValue.RightRevealed)
                            }
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
            }
        },
        modifier =
            Modifier.semantics {
                // Use custom actions to make the primary and secondary actions accessible
                customActions =
                    listOf(
                        CustomAccessibilityAction("Delete") {
                            /* Add the primary action click handler */
                            true
                        }
                    )
            },
        state = revealState,
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    /* Add the undo action */
                    coroutineScope.launch { revealState.animateTo(RevealValue.Covered) }
                },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Undo") },
            )
        },
    ) {
        Chip(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler */
                                true
                            }
                        )
                },
            onClick = { /* the click action associated with chip */ },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(text = "Swipe Me") },
        )
    }
}

/*
 * This demos how swipe to reveal with two actions can be done using just the Foundation layer,
 * but we would expect developers to use the Material or Material3 layers which make the task
 * easier.
 */
@Composable
fun SwipeToRevealDemoTwoActions() {
    val state = rememberRevealState()
    val coroutineScope = rememberCoroutineScope()
    SwipeToReveal(
        state = state,
        primaryAction = {
            Box(
                modifier =
                    Modifier.fillMaxSize().clickable {
                        /* Add the primary action */
                        coroutineScope.launch { state.animateTo(RevealValue.RightRevealed) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Delete")
            }
        },
        secondaryAction = {
            Box(
                modifier =
                    Modifier.fillMaxSize().clickable {
                        /* Add the secondary action */
                        coroutineScope.launch { state.animateTo(RevealValue.Covered) }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = Icons.Outlined.MoreVert, contentDescription = "More")
            }
        },
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    /* Add the undo action */
                    coroutineScope.launch { state.animateTo(RevealValue.Covered) }
                },
                colors = ChipDefaults.secondaryChipColors(),
                label = { Text(text = "Undo") },
            )
        },
    ) {
        Chip(
            modifier =
                Modifier.fillMaxWidth().semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler */
                                true
                            },
                            CustomAccessibilityAction("More") {
                                /* Add the secondary action click handler */
                                true
                            },
                        )
                },
            onClick = { /* the click action associated with chip */ },
            colors = ChipDefaults.secondaryChipColors(),
            label = { Text(text = "Swipe Me") },
        )
    }
}
