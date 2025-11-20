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

package androidx.wear.compose.integration.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.Text

@Composable
fun SwipeToRevealSingleButtonWithAnchoring() {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        SwipeToReveal(
            primaryAction = {
                PrimaryActionButton(
                    onClick = { /* This block is called when the primary action is executed. */ },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                    text = { Text("Delete") },
                )
            },
            onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
            },
            undoPrimaryAction = {
                UndoActionButton(
                    onClick = { /* This block is called when the undo primary action is executed. */
                    },
                    text = { Text("Undo Delete") },
                )
            },
        ) {
            Button(
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Use custom actions to make the primary action accessible
                        customActions =
                            listOf(
                                CustomAccessibilityAction("Delete") {
                                    /* Add the primary action click handler here */
                                    true
                                }
                            )
                    },
                onClick = {},
            ) {
                Text("This Button has only one action", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun SwipeToRevealBothDirectionsNonAnchoring() {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        SwipeToReveal(
            primaryAction = {
                PrimaryActionButton(
                    onClick = { /* This block is called when the primary action is executed. */ },
                    icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                    text = { Text("Delete") },
                )
            },
            onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
            },
            undoPrimaryAction = {
                UndoActionButton(
                    onClick = { /* This block is called when the undo primary action is executed. */
                    },
                    text = { Text("Undo Delete") },
                )
            },
            revealDirection = Bidirectional,
            hasPartiallyRevealedState = false,
        ) {
            Button(
                modifier =
                    Modifier.fillMaxWidth().semantics {
                        // Use custom actions to make the primary action accessible
                        customActions =
                            listOf(
                                CustomAccessibilityAction("Delete") {
                                    /* Add the primary action click handler here */
                                    true
                                }
                            )
                    },
                onClick = {},
            ) {
                Text("This Button has only one action", modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
