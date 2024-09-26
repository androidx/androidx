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
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExpandableState
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.RevealActionType
import androidx.wear.compose.foundation.RevealState
import androidx.wear.compose.foundation.RevealValue
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.expandableItem
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.rememberExpandableState
import androidx.wear.compose.foundation.rememberExpandableStateMapping
import androidx.wear.compose.foundation.rememberRevealState
import androidx.wear.compose.material.AppCard
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
import androidx.wear.compose.material.dialog.Alert
import androidx.wear.compose.material.dialog.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun SwipeToRevealChips(
    swipeToDismissBoxState: SwipeToDismissBoxState,
    includeSecondaryAction: Boolean
) {
    val expandableStateMapping = rememberExpandableStateMapping<Int>(initiallyExpanded = { true })
    var itemCount by remember { mutableIntStateOf(3) }

    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(text = "Swipe To Reveal Chips - Undo")
            Spacer(Modifier.size(15.dp))
        }
        repeat(itemCount) {
            val currentState = expandableStateMapping.getOrPutNew(it)
            expandableItem(state = currentState) { expanded ->
                var undoActionEnabled by remember { mutableStateOf(true) }
                val revealState = rememberRevealState()
                val coroutineScope = rememberCoroutineScope()
                val deleteItem: () -> Unit = {
                    coroutineScope.launch {
                        revealState.animateTo(RevealValue.RightRevealed)

                        // hide the content after some time if the state is still revealed
                        delay(1500)
                        if (revealState.currentValue == RevealValue.RightRevealed) {
                            // Undo should no longer be triggered
                            undoActionEnabled = false
                            currentState.expanded = false
                        }
                    }
                }
                val addItem: () -> Unit = {
                    coroutineScope.launch {
                        revealState.animateTo(RevealValue.RightRevealed)
                        itemCount++

                        // reset the state after some delay if the state is still revealed
                        delay(2000)
                        if (revealState.currentValue == RevealValue.RightRevealed) {
                            revealState.animateTo(RevealValue.Covered)
                        }
                    }
                }
                val undoDeleteItem: () -> Unit = {
                    if (undoActionEnabled) {
                        coroutineScope.launch {
                            // reset the state when undo is clicked
                            revealState.animateTo(RevealValue.Covered)
                        }
                    }
                }
                val undoAddItem: () -> Unit = {
                    coroutineScope.launch {
                        itemCount--
                        // reset the state when undo is clicked
                        revealState.animateTo(RevealValue.Covered)
                    }
                }
                if (expanded) {
                    SwipeToRevealChipExpandable(
                        modifier = Modifier.edgeSwipeToDismiss(swipeToDismissBoxState),
                        text = "Chip #$it",
                        revealState = revealState,
                        onDeleteAction = deleteItem,
                        onUndoDelete = undoDeleteItem,
                        onDuplicateAction = addItem.takeIf { includeSecondaryAction },
                        onUndoDuplicate = undoAddItem.takeIf { includeSecondaryAction }
                    )
                } else {
                    Spacer(modifier = Modifier.width(200.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun SwipeToRevealChipExpandable(
    modifier: Modifier = Modifier,
    text: String,
    revealState: RevealState,
    onDeleteAction: () -> Unit,
    onUndoDelete: () -> Unit,
    onDuplicateAction: (() -> Unit)?,
    onUndoDuplicate: (() -> Unit)?
) {
    SwipeToRevealChip(
        modifier =
            modifier.semantics {
                customActions =
                    listOfNotNull(
                        CustomAccessibilityAction("Delete") {
                            onDeleteAction()
                            true
                        },
                        onDuplicateAction?.let {
                            CustomAccessibilityAction("Duplicate") {
                                onDuplicateAction()
                                true
                            }
                        }
                    )
            },
        revealState = revealState,
        onFullSwipe = onDeleteAction,
        primaryAction = {
            SwipeToRevealPrimaryAction(
                revealState = revealState,
                icon = { Icon(SwipeToRevealDefaults.Delete, contentDescription = "Delete") },
                label = { Text(text = "Delete") },
                onClick = onDeleteAction,
            )
        },
        secondaryAction =
            onDuplicateAction?.let {
                {
                    SwipeToRevealSecondaryAction(
                        revealState = revealState,
                        content = { Icon(Icons.Outlined.Add, contentDescription = "Duplicate") },
                        onClick = onDuplicateAction
                    )
                }
            },
        undoPrimaryAction = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                label = { Text("Undo Delete") },
                onClick = onUndoDelete
            )
        },
        undoSecondaryAction =
            onUndoDuplicate?.let {
                {
                    SwipeToRevealUndoAction(
                        revealState = revealState,
                        label = { Text("Undo Duplicate") },
                        onClick = onUndoDuplicate
                    )
                }
            }
    ) {
        Chip(
            onClick = { /*TODO*/ },
            colors = ChipDefaults.secondaryChipColors(),
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text) }
        )
    }
}

@Composable
fun SwipeToRevealCards(swipeToDismissBoxState: SwipeToDismissBoxState) {
    val emailMap =
        mutableMapOf(
            "Android In" to "Please add Swipe to dismiss to the demo.",
            "Google Bangalore" to
                "Hey everyone, We are pleased to inform that we are starting a new batch.",
            "Google India" to "Hi Googlers, Please be prepared for the new changes."
        )
    val expandableStates = List(emailMap.size) { rememberExpandableState(initiallyExpanded = true) }
    ScalingLazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(text = "Swipe To Reveal Cards")
            Spacer(Modifier.size(10.dp))
        }
        repeat(emailMap.size) {
            val currentState = expandableStates[it]
            val currentFrom = emailMap.keys.elementAt(it)
            val currentEmail = emailMap.values.elementAt(it)
            expandableItem(state = currentState) { expanded ->
                if (expanded) {
                    SwipeToRevealCardExpandable(
                        expandableState = currentState,
                        from = currentFrom,
                        email = currentEmail,
                        modifier = Modifier.edgeSwipeToDismiss(swipeToDismissBoxState)
                    )
                } else {
                    Spacer(modifier = Modifier.width(200.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalWearMaterialApi::class)
@Composable
private fun SwipeToRevealCardExpandable(
    expandableState: ExpandableState,
    from: String,
    email: String,
    modifier: Modifier = Modifier
) {
    val revealState = rememberRevealState()
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    LaunchedEffect(revealState.currentValue) {
        if (revealState.currentValue == RevealValue.RightRevealed) {
            delay(2000)
            expandableState.expanded = false
        }
    }
    LaunchedEffect(showDialog) {
        if (!showDialog) {
            delay(500)
            revealState.animateTo(RevealValue.Covered)
        }
    }
    ShowDialog(
        showDialog = showDialog,
        onClick = { showDialog = false },
        onDismiss = { showDialog = false },
    )
    SwipeToRevealCard(
        modifier =
            modifier.semantics {
                customActions =
                    listOf(
                        CustomAccessibilityAction("Delete") {
                            coroutineScope.launch {
                                revealState.animateTo(RevealValue.RightRevealed)
                            }
                            true
                        },
                        CustomAccessibilityAction("More Options") {
                            showDialog = true
                            true
                        }
                    )
            },
        revealState = revealState,
        onFullSwipe = {
            coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
        },
        primaryAction = {
            SwipeToRevealPrimaryAction(
                revealState = revealState,
                icon = { Icon(SwipeToRevealDefaults.Delete, contentDescription = "Delete") },
                label = { Text(text = "Delete") },
                onClick = {
                    coroutineScope.launch { revealState.animateTo(RevealValue.RightRevealed) }
                }
            )
        },
        secondaryAction = {
            SwipeToRevealSecondaryAction(
                revealState = revealState,
                content = {
                    Icon(SwipeToRevealDefaults.MoreOptions, contentDescription = "More Options")
                },
                onClick = {
                    showDialog = true
                    // reset click type since there is no undo for this
                    revealState.lastActionType = RevealActionType.None
                }
            )
        },
        undoPrimaryAction = {
            SwipeToRevealUndoAction(
                revealState = revealState,
                label = { Text(text = "Undo") },
                onClick = {
                    coroutineScope.launch {
                        // reset the state when undo is clicked
                        revealState.animateTo(RevealValue.Covered)
                    }
                }
            )
        },
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
            Text(text = email, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ShowDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
) {
    Dialog(
        showDialog = showDialog,
        onDismissRequest = onDismiss,
    ) {
        Alert(title = { Text("Other options", textAlign = TextAlign.Center) }) {
            repeat(3) {
                item {
                    Chip(
                        label = { Text("Option $it") },
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onClick,
                        colors = ChipDefaults.primaryChipColors()
                    )
                }
            }
        }
    }
}
