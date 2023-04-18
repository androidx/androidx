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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExpandableState
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealScope
import androidx.wear.compose.foundation.expandableItem
import androidx.wear.compose.foundation.fractionalPositionalThreshold
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.rememberExpandableState
import androidx.wear.compose.material.AppCard
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.foundation.SwipeToReveal
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.material.Text
import androidx.wear.compose.foundation.createAnchors
import androidx.wear.compose.foundation.rememberRevealState
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                }

                LaunchedEffect(currentState.expanded) {
                    if (!currentState.expanded) {
                        emailMap.remove(currentFrom)
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealWithSingleAction() {
    SwipeToRevealSingleAction()
}

@Composable
fun SwipeToRevealInRtl() {
    SwipeToRevealSingleAction(LayoutDirection.Rtl)
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun SwipeToRevealChipExpandable(
    expandableState: ExpandableState
) {
    val state = rememberRevealState()
    LaunchedEffect(state.currentValue) {
        if (state.currentValue == RevealValue.Revealed) {
            delay(2000)
            expandableState.expanded = false
            state.snapTo(RevealValue.Covered)
        }
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(width = 200.dp, height = 50.dp)
    ) {
        SwipeToRevealWithDefaultButtons(
            shape = CircleShape,
            state = state,
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
}

@OptIn(ExperimentalWearFoundationApi::class)
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
            state.snapTo(RevealValue.Covered)
        }
    }
    SwipeToRevealWithDefaultButtons(
        shape = RoundedCornerShape(10.dp),
        state = state
    ) {
        AppCard(
            onClick = {},
            modifier = Modifier.size(width = 200.dp, height = 100.dp),
            appName = { Text("Gmail") },
            appImage = {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                )
            },
            time = { Text("now") },
            title = { Text("From: $from") }
        ) {
            Text(
                text = email,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun SwipeToRevealWithDefaultButtons(
    state: RevealState = rememberRevealState(),
    shape: Shape = CircleShape,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    SwipeToReveal(
        action = {
            SwipeToRevealAction(
                color = MaterialTheme.colors.error,
                icon = Icons.Outlined.Delete,
                text = "Clear",
                contentDescription = "Delete",
                onClick = { state.animateTo(RevealValue.Revealed) },
                shape = shape,
                coroutineScope = coroutineScope,
                state = state
            )
        },
        additionalAction = {
            SwipeToRevealAction(
                color = MaterialTheme.colors.onSurfaceVariant,
                icon = Icons.Outlined.MoreVert,
                onClick = { state.animateTo(RevealValue.Covered) },
                shape = shape,
                coroutineScope = coroutineScope,
                state = state
            )
        },
        undoAction = {
            Chip(
                modifier = Modifier.fillMaxWidth(),
                onClick = { coroutineScope.launch { state.animateTo(RevealValue.Covered) } },
                colors = ChipDefaults.secondaryChipColors(),
                border = ChipDefaults.outlinedChipBorder(),
                label = { Text(text = "Undo") }
            )
        },
        state = state,
        content = content,
    )
}

@OptIn(ExperimentalWearFoundationApi::class)
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
                    anchors = createAnchors(revealingAnchor = 0.5f),
                    positionalThreshold = fractionalPositionalThreshold(0.5f)
                )
                if (expanded) {
                    CompositionLocalProvider(
                        LocalLayoutDirection provides layoutDirection
                    ) {
                        SwipeToReveal(
                            action = {
                                SwipeToRevealAction(
                                    color = MaterialTheme.colors.error,
                                    icon = Icons.Outlined.Delete,
                                    text = "Clear",
                                    contentDescription = "Delete",
                                    onClick = { state.animateTo(RevealValue.Revealed) },
                                    shape = CircleShape,
                                    state = state
                                )
                            },
                            state = state
                        ) {
                            Chip(
                                onClick = { /*TODO*/ },
                                colors = ChipDefaults.secondaryChipColors(),
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Try this") }
                            )
                        }
                    }
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

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
private fun RevealScope.SwipeToRevealAction(
    color: Color,
    icon: ImageVector,
    text: String? = null,
    contentDescription: String? = null,
    onClick: suspend () -> Unit = {},
    shape: Shape = RoundedCornerShape(15.dp),
    state: RevealState = rememberRevealState(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
) {
    Row(
        modifier = Modifier
            .clickable {
                coroutineScope.launch { onClick() }
            }
            .background(color, shape)
            .fillMaxSize(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = Color.DarkGray)
        if (abs(state.offset) > revealOffset && text != null) {
            Spacer(Modifier.size(5.dp))
            Text(text = text)
        }
    }
}
