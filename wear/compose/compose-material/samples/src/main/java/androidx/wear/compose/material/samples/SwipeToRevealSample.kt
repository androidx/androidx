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

package androidx.wear.compose.material.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.SwipeToRevealCard
import androidx.wear.compose.material.SwipeToRevealChip
import androidx.wear.compose.material.SwipeToRevealDefaults
import androidx.wear.compose.material.SwipeToRevealPrimaryAction
import androidx.wear.compose.material.SwipeToRevealSecondaryAction
import androidx.wear.compose.material.SwipeToRevealUndoAction
import androidx.wear.compose.material.Text

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
@Sampled
fun SwipeToRevealChipSample(swipeToDismissBoxState: SwipeToDismissBoxState) {
    val revealState = rememberRevealState()
    SwipeToRevealChip(
        revealState = revealState,
        modifier =
            Modifier.fillMaxWidth()
                // Use edgeSwipeToDismiss to allow SwipeToDismissBox to capture swipe events
                .edgeSwipeToDismiss(swipeToDismissBoxState)
                .semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler here */
                                true
                            },
                            CustomAccessibilityAction("More Options") {
                                /* Add the secondary click handler here */
                                true
                            }
                        )
                },
        primaryAction = {
            SwipeToRevealPrimaryAction(
                revealState = revealState,
                icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
                label = { Text("Delete") },
                onClick = { /* Add the click handler here */ }
            )
        },
        secondaryAction = {
            SwipeToRevealSecondaryAction(
                revealState = revealState,
                onClick = { /* Add the click handler here */ }
            ) {
                Icon(SwipeToRevealDefaults.MoreOptions, "More Options")
            }
        },
        undoPrimaryAction = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                label = { Text("Undo") },
                onClick = { /* Add the undo handler for primary action */ }
            )
        },
        undoSecondaryAction = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                label = { Text("Undo") },
                onClick = { /* Add the undo handler for secondary action */ }
            )
        },
        onFullSwipe = { /* Add the full swipe handler here */ }
    ) {
        Chip(
            modifier = Modifier.fillMaxWidth(),
            onClick = { /* Add the chip click handler here */ },
            colors = ChipDefaults.primaryChipColors(),
            border = ChipDefaults.outlinedChipBorder(),
            label = { Text("SwipeToReveal Chip", maxLines = 3) }
        )
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
@Sampled
fun SwipeToRevealCardSample(swipeToDismissBoxState: SwipeToDismissBoxState) {
    val revealState = rememberRevealState()
    SwipeToRevealCard(
        revealState = revealState,
        modifier =
            Modifier.fillMaxWidth()
                // Use edgeSwipeToDismiss to allow SwipeToDismissBox to capture swipe events
                .edgeSwipeToDismiss(swipeToDismissBoxState)
                .semantics {
                    // Use custom actions to make the primary and secondary actions accessible
                    customActions =
                        listOf(
                            CustomAccessibilityAction("Delete") {
                                /* Add the primary action click handler here */
                                true
                            },
                            CustomAccessibilityAction("More Options") {
                                /* Add the secondary click handler here */
                                true
                            }
                        )
                },
        primaryAction = {
            SwipeToRevealPrimaryAction(
                revealState = revealState,
                icon = { Icon(SwipeToRevealDefaults.Delete, "Delete") },
                label = { Text("Delete") },
                onClick = { /* Add the click handler here */ }
            )
        },
        secondaryAction = {
            SwipeToRevealSecondaryAction(
                revealState = revealState,
                onClick = { /* Add the click handler here */ }
            ) {
                Icon(SwipeToRevealDefaults.MoreOptions, "More Options")
            }
        },
        undoPrimaryAction = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                label = { Text("Undo") },
                onClick = { /* Add the undo handler for primary action */ }
            )
        },
        undoSecondaryAction = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                label = { Text("Undo") },
                onClick = { /* Add the undo handler for secondary action */ }
            )
        },
        onFullSwipe = { /* Add the full swipe handler here */ }
    ) {
        AppCard(
            onClick = { /* Add the Card click handler */ },
            appName = { Text("AppName") },
            appImage = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_airplanemode_active_24px),
                    contentDescription = "airplane",
                    modifier =
                        Modifier.size(CardDefaults.AppImageSize)
                            .wrapContentSize(align = Alignment.Center),
                )
            },
            title = { Text("AppCard") },
            time = { Text("now") }
        ) {
            Text("Basic card with SwipeToReveal actions")
        }
    }
}
