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

package androidx.wear.compose.integration.demos

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExpandableState
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.createAnchors
import androidx.wear.compose.foundation.expandableItem
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.rememberExpandableState
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExperimentalWearMaterialApi
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.SwipeToRevealCard
import androidx.wear.compose.material.SwipeToRevealChip
import androidx.wear.compose.material.SwipeToRevealDefaults
import androidx.wear.compose.material.Text
import kotlinx.coroutines.delay

@Composable
fun SwipeToRevealChips() {
    val expandableStates = List(3) { rememberExpandableState(initiallyExpanded = true) }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(text = "Swipe To Reveal Chips")
            Spacer(Modifier.size(15.dp))
        }
        repeat(3) {
            val currentState = expandableStates[it]
            expandableItem(
                state = currentState
            ) { expanded ->
                if (expanded) {
                    SwipeToRevealChipExpandable(
                        expandableState = currentState
                    )
                } else {
                    Spacer(modifier = Modifier.width(200.dp))
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealCards() {
    val emailMap = mutableMapOf(
        "Android In" to
            "Please add Swipe to dismiss to the demo.",
        "Google Bangalore" to
            "Hey everyone, We are pleased to inform that we are starting a new batch.",
        "Google India" to
            "Hi Googlers, Please be prepared for the new changes."
    )
    val expandableStates = List(emailMap.size) { rememberExpandableState(initiallyExpanded = true) }
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(text = "Swipe To Reveal Cards")
            Spacer(Modifier.size(10.dp))
        }
        repeat(emailMap.size) {
            val currentState = expandableStates[it]
            val currentFrom = emailMap.keys.elementAt(it)
            val currentEmail = emailMap.values.elementAt(it)
            expandableItem(
                state = currentState
            ) { expanded ->
                if (expanded) {
                    SwipeToRevealCardExpandable(
                        expandableState = currentState,
                        from = currentFrom,
                        email = currentEmail
                    )
                } else {
                    Spacer(modifier = Modifier.width(200.dp))
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealWithSingleAction() {
    SwipeToRevealSingleAction()
}

/**
 * Swipe to reveal in RTL. This is should be identical to LTR.
 */
@Composable
fun SwipeToRevealInRtl() {
    SwipeToRevealSingleAction(LayoutDirection.Rtl)
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearMaterialApi::class)
@Composable
private fun SwipeToRevealChipExpandable(
    expandableState: ExpandableState
) {
    val state = rememberRevealState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == RevealValue.Revealed) {
            delay(2000)
            expandableState.expanded = false
        }
    }
    SwipeToRevealChip(
        revealState = state,
        action = SwipeToRevealDefaults.action(
            icon = { Icon(SwipeToRevealDefaults.Delete, contentDescription = "Delete") },
            label = { Text(text = "Delete") },
        ),
        additionalAction = SwipeToRevealDefaults.additionalAction(
            icon = { Icon(SwipeToRevealDefaults.MoreOptions, contentDescription = "More Options") },
        ),
        undoAction = SwipeToRevealDefaults.undoAction(
            label = { Text(text = "Undo") },
        ),
    ) {
        Chip(
            onClick = { /*TODO*/ },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text("S2R Chip with defaults")
            }
        )
    }
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearMaterialApi::class)
@Composable
private fun SwipeToRevealCardExpandable(
    expandableState: ExpandableState,
    from: String,
    email: String
) {
    val state = rememberRevealState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == RevealValue.Revealed) {
            delay(2000)
            expandableState.expanded = false
        }
    }
    SwipeToRevealCard(
        revealState = state,
        action = SwipeToRevealDefaults.action(
            icon = { Icon(SwipeToRevealDefaults.Delete, contentDescription = "Delete") },
            label = { Text(text = "Delete") },
        ),
        additionalAction = SwipeToRevealDefaults.additionalAction(
            icon = { Icon(SwipeToRevealDefaults.MoreOptions, contentDescription = "More Options") },
        ),
        undoAction = SwipeToRevealDefaults.undoAction(
            label = { Text(text = "Undo") },
        ),
    ) {
        AppCard(
            onClick = {},
            modifier = Modifier.width(width = 200.dp),
            appName = { Text("Gmail") },
            appImage = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                )
            },
            time = { Text("now") },
            title = { Text("From: $from", maxLines = 1, overflow = TextOverflow.Ellipsis) }
        ) {
            Text(
                text = email,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class, ExperimentalWearMaterialApi::class)
@Composable
private fun SwipeToRevealSingleAction(
    layoutDirection: LayoutDirection = LayoutDirection.Ltr
) {
    val itemCount = 2
    val expandableState = List(itemCount) {
        rememberExpandableState(initiallyExpanded = true)
    }
    ScalingLazyColumn {
        item {
            Text("Swipe to reveal One-Action")
            Spacer(Modifier.size(10.dp))
        }
        repeat(itemCount) { curr ->
            expandableItem(
                state = expandableState[curr]
            ) { expanded ->
                val state = rememberRevealState(
                    // Setting anchor to 0.4 since there is only one action.
                    anchors = createAnchors(revealingAnchor = 0.4f),
                )
                if (expanded) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides layoutDirection
                    ) {
                        SwipeToRevealChip(
                            revealState = state,
                            action = SwipeToRevealDefaults.action(
                                icon = {
                                    Icon(
                                        SwipeToRevealDefaults.Delete,
                                        contentDescription = "Delete"
                                    )
                                },
                                label = { Text(text = "Delete") },
                            ),
                        ) {
                            Chip(
                                onClick = { /*TODO*/ },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Try this") }
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.width(200.dp))
                }
                LaunchedEffect(state.currentValue) {
                    if (state.currentValue == RevealValue.Revealed) {
                        delay(2000)
                        expandableState[curr].expanded = false
                    }
                }
            }
        }
    }
}
