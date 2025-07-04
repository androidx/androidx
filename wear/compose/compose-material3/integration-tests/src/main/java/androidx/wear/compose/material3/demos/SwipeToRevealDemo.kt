/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.compose.material3.demos

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.GestureInclusion
import androidx.wear.compose.foundation.SwipeToDismissBoxState
import androidx.wear.compose.foundation.edgeSwipeToDismiss
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RevealDirection.Companion.Bidirectional
import androidx.wear.compose.material3.RevealValue.Companion.Covered
import androidx.wear.compose.material3.SplitSwitchButton
import androidx.wear.compose.material3.SwipeToReveal
import androidx.wear.compose.material3.SwipeToRevealDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.rememberRevealState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SwipeToRevealBothDirectionsNoPartialReveal() {
    ScalingLazyDemo {
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* This block is called when the primary action is executed. */
                        },
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
                    Text("This Button has only one action", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealBothDirections() {
    ScalingLazyDemo {
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* This block is called when the primary action is executed. */
                        },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
                },
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = { /* This block is called when the secondary action is executed. */
                        },
                        icon = { Icon(Icons.Outlined.MoreVert, contentDescription = "More") },
                    )
                },
                undoPrimaryAction = {
                    UndoActionButton(
                        onClick = { /* This block is called when the undo primary action is executed. */
                        },
                        text = { Text("Undo Delete") },
                    )
                },
                undoSecondaryAction = {
                    UndoActionButton(
                        onClick = { /* This block is called when the undo secondary action is executed. */
                        },
                        text = { Text("Undo Secondary") },
                    )
                },
                revealDirection = Bidirectional,
            ) {
                Button(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            // Use custom actions to make the primary and secondary actions
                            // accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        /* Add the primary action click handler here */
                                        true
                                    },
                                    CustomAccessibilityAction("More") {
                                        /* Add the secondary click handler here */
                                        true
                                    },
                                )
                        },
                    onClick = {},
                ) {
                    Text("This Button has two actions", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealTwoActionsWithUndo() {
    val context = LocalContext.current
    val showToasts = remember { mutableStateOf(true) }

    val primaryAction = {
        if (showToasts.value) {
            Toast.makeText(context, "Primary action executed.", Toast.LENGTH_SHORT).show()
        }
    }
    ScalingLazyDemo {
        item { ListHeader { Text("Two Undo Actions") } }
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = primaryAction,
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                        modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                    )
                },
                onSwipePrimaryAction = primaryAction,
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = {
                            if (showToasts.value) {
                                Toast.makeText(
                                        context,
                                        "Secondary action executed.",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        },
                        icon = { Icon(Icons.Filled.Lock, contentDescription = "Lock") },
                        modifier = Modifier.height(SwipeToRevealDefaults.LargeActionButtonHeight),
                    )
                },
                undoPrimaryAction = {
                    UndoActionButton(
                        onClick = {
                            if (showToasts.value) {
                                Toast.makeText(
                                        context,
                                        "Undo primary action executed.",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        },
                        text = { Text("Undo Delete") },
                    )
                },
                undoSecondaryAction = {
                    UndoActionButton(
                        onClick = {
                            if (showToasts.value) {
                                Toast.makeText(
                                        context,
                                        "Undo secondary action executed.",
                                        Toast.LENGTH_SHORT,
                                    )
                                    .show()
                            }
                        },
                        text = { Text("Undo Lock") },
                    )
                },
            ) {
                Card(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            // Use custom actions to make the primary and secondary actions
                            // accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        /* Add the primary action click handler here */
                                        true
                                    },
                                    CustomAccessibilityAction("Lock") {
                                        /* Add the secondary click handler here */
                                        true
                                    },
                                )
                        },
                    onClick = {},
                ) {
                    Text("This Card has two actions", modifier = Modifier.fillMaxSize())
                }
            }
        }
        item {
            SplitSwitchButton(
                showToasts.value,
                onCheckedChange = { showToasts.value = it },
                onContainerClick = { showToasts.value = !showToasts.value },
                toggleContentDescription = "Show toasts",
            ) {
                Text("Show toasts")
            }
        }
    }
}

@Composable
fun SwipeToRevealInScalingLazyColumn() {
    data class ListItem(val name: String, var undoButtonClicked: Boolean = false)
    val listState = remember {
        mutableStateListOf(
            ListItem("Alice"),
            ListItem("Bob"),
            ListItem("Charlie"),
            ListItem("Dave"),
            ListItem("Eve"),
        )
    }
    val coroutineScope = rememberCoroutineScope()
    ScalingLazyDemo(contentPadding = PaddingValues(0.dp)) {
        items(listState.size, key = { listState[it].name }) { index ->
            val item = remember { listState[index] }
            val primaryAction: () -> Unit = {
                coroutineScope.launch {
                    delay(2000)
                    // After a delay, remove the item from the list if the last
                    // action performed by the user is still the primary action, so
                    // the user didn't press "Undo".
                    if (!item.undoButtonClicked) {
                        listState.remove(item)
                    }
                }
            }
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = primaryAction,
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = primaryAction,
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = { /* This block is called when the secondary action is executed. */
                        },
                        icon = { Icon(Icons.Filled.MoreVert, contentDescription = "Duplicate") },
                    )
                },
                undoPrimaryAction = {
                    UndoActionButton(
                        onClick = { item.undoButtonClicked = true },
                        text = { Text("Undo Delete") },
                    )
                },
                revealDirection = Bidirectional,
            ) {
                Button(
                    {},
                    Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics {
                        // Use custom actions to make the primary and secondary actions accessible
                        customActions =
                            listOf(
                                CustomAccessibilityAction("Delete") {
                                    /* Add the primary action click handler here */
                                    true
                                },
                                CustomAccessibilityAction("Duplicate") {
                                    /* Add the secondary click handler here */
                                    true
                                },
                            )
                    },
                ) {
                    Text("Name:\n${item.name}\n\nMessage:\nMessage body.")
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealSingleButtonWithPartialReveal() {
    ScalingLazyDemo {
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* This block is called when the primary action is executed. */
                        },
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
                    Text("This Button has only one action", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealWithLongLabels() {
    ScalingLazyDemo {
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* This block is called when the primary action is executed. */
                        },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = {
                            Text("Delete action with an extremely long label that should truncate.")
                        },
                    )
                },
                onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
                },
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = { /* This block is called when the secondary action is executed. */
                        },
                        icon = { Icon(Icons.Outlined.Lock, contentDescription = "Lock") },
                    )
                },
                undoPrimaryAction = {
                    UndoActionButton(
                        onClick = { /* This block is called when the undo primary action is executed. */
                        },
                        text = {
                            Text(
                                "Undo Delete action with an extremely long label that should truncate."
                            )
                        },
                    )
                },
                undoSecondaryAction = {
                    UndoActionButton(
                        onClick = { /* This block is called when the undo secondary action is executed. */
                        },
                        text = {
                            Text(
                                "Undo Lock action with an extremely long label that should truncate."
                            )
                        },
                    )
                },
            ) {
                Button(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            // Use custom actions to make the primary and secondary actions
                            // accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        /* Add the primary action click handler here */
                                        true
                                    },
                                    CustomAccessibilityAction("Lock") {
                                        /* Add the secondary click handler here */
                                        true
                                    },
                                )
                        },
                    onClick = {},
                ) {
                    Text(
                        "This Button has actions with extremely long labels that should truncate.",
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealWithCustomIcons() {
    ScalingLazyDemo {
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* This block is called when the primary action is executed. */
                        },
                        icon = {
                            // Although this practice is not recommended, this demo deliberately
                            // passes Text in the icon slot so that this edge case can be
                            // visualised.
                            Text(
                                "🗑",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
                },
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = { /* This block is called when the secondary action is executed. */
                        },
                        icon = {
                            // Although this practice is not recommended, this demo deliberately
                            // passes Text in the icon slot so that this edge case can be
                            // visualised.
                            Text(
                                "U",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                    )
                },
                undoPrimaryAction = {
                    UndoActionButton(
                        onClick = { /* This block is called when the undo primary action is executed. */
                        },
                        icon = {
                            // Although this practice is not recommended, this demo deliberately
                            // passes Text in the icon slot so that this edge case can be
                            // visualised.
                            Text(
                                "<",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        text = { Text("Undo Delete") },
                    )
                },
                undoSecondaryAction = {
                    UndoActionButton(
                        onClick = { /* This block is called when the undo secondary action is executed. */
                        },
                        icon = {
                            // Although this practice is not recommended, this demo deliberately
                            // passes Text in the icon slot so that this edge case can be
                            // visualised.
                            Text(
                                text = "🔙",
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                            )
                        },
                        text = { Text("Undo Update") },
                    )
                },
            ) {
                Button(
                    modifier =
                        Modifier.fillMaxWidth().semantics {
                            // Use custom actions to make the primary and secondary actions
                            // accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        /* Add the primary action click handler here */
                                        true
                                    },
                                    CustomAccessibilityAction("Update") {
                                        /* Add the secondary click handler here */
                                        true
                                    },
                                )
                        },
                    onClick = {},
                ) {
                    Text("This Button has two actions.", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Usage of [edgeSwipeToDismiss] modifier with [SwipeToReveal] is discouraged. Instead, the
 * [GestureInclusion] parameter should be used.
 *
 * This demo is to check compatibility with code that is still using [edgeSwipeToDismiss] after
 * [GestureInclusion] was introduced.
 */
@Composable
fun SwipeToRevealWithEdgeSwipeToDismiss(swipeToDismissBoxState: SwipeToDismissBoxState) {
    ScalingLazyDemo {
        item {
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* This block is called when the primary action is executed. */
                        },
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
                modifier = Modifier.edgeSwipeToDismiss(swipeToDismissBoxState),
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
                    Text("This Button has only one action", modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
fun SwipeToRevealWithTransformingLazyColumnNoResetOnScrollDemo() {
    val transformationSpec = rememberTransformationSpec()
    val tlcState = rememberTransformingLazyColumnState()

    TransformingLazyColumn(
        state = tlcState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(count = 100) { index ->
            val revealState = rememberRevealState(initialValue = Covered)

            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* Called when the primary action is executed. */ },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
                },
                modifier =
                    Modifier.transformedHeight(this@items, transformationSpec).graphicsLayer {
                        with(transformationSpec) { applyContainerTransformation(scrollProgress) }
                        // Is needed to disable clipping.
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        clip = false
                    },
                revealState = revealState,
                revealDirection = Bidirectional,
            ) {
                TitleCard(
                    onClick = {},
                    title = { Text("Message #$index") },
                    subtitle = {
                        Text(
                            "Body of the message that should be long enough to take at least two lines."
                        )
                    },
                    modifier =
                        Modifier.semantics {
                            // Use custom actions to make the primary action accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        /* Add the primary action click handler here */
                                        true
                                    }
                                )
                        },
                )
            }
        }
    }
}

@Composable
fun SwipeToRevealWithTransformingLazyColumnIconActionNoResetOnScrollDemo() {
    val transformationSpec = rememberTransformationSpec()
    val tlcState = rememberTransformingLazyColumnState()

    TransformingLazyColumn(
        state = tlcState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(count = 100) { index ->
            val revealState = rememberRevealState(initialValue = Covered)

            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = { /* Called when the primary action is executed. */ },
                        modifier = Modifier.heightIn(70.dp),
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = {},
                        containerColor = Color(red = 0.427f, green = 0.835f, blue = 0.549f),
                    )
                },
                secondaryAction = {
                    SecondaryActionButton(
                        onClick = { /* Called when the primary action is executed. */ },
                        modifier = Modifier.heightIn(70.dp),
                        icon = { Icon(Icons.Outlined.Share, contentDescription = "Share") },
                        containerColor = Color(0.949f, 0.722f, 0.71f),
                        contentColor = Color(0.207f, 0.148f, 0.145f),
                    )
                },
                onSwipePrimaryAction = { /* This block is called when the full swipe gesture is performed. */
                },
                modifier =
                    Modifier.transformedHeight(this@items, transformationSpec).graphicsLayer {
                        with(transformationSpec) { applyContainerTransformation(scrollProgress) }
                        // Is needed to disable clipping.
                        compositingStrategy = CompositingStrategy.ModulateAlpha
                        clip = false
                    },
                revealState = revealState,
                revealDirection = Bidirectional,
            ) {
                TitleCard(
                    onClick = {},
                    title = {
                        Icon(Icons.Outlined.AccountCircle, contentDescription = "Share")
                        Spacer(Modifier.width(4.dp))
                        Text(
                            modifier = Modifier.align(Alignment.CenterVertically),
                            text = "Sender #$index",
                        )
                    },
                    subtitle = {
                        Text("Message #$index")
                        Text("Body of the message")
                        Text("13:31")
                    },
                    modifier =
                        Modifier.semantics {
                            // Use custom actions to make the primary and secondary actions
                            // accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        /* Add the primary action click handler here */
                                        true
                                    },
                                    CustomAccessibilityAction("Share") {
                                        /* Add the secondary action click handler here */
                                        true
                                    },
                                )
                        },
                )
            }
        }
    }
}

@Composable
fun SwipeToRevealWithTransformingLazyColumnExpansionAndDeletionDemo() {
    val transformationSpec = rememberTransformationSpec()
    val tlcState = rememberTransformingLazyColumnState()

    var expandedItemKey by remember { mutableStateOf<String?>(null) }

    val messages = remember {
        mutableStateListOf<MessageItem>().apply {
            for (i in 1..100) {
                add(
                    MessageItem(
                        "Message #${i}",
                        body = "Body of the message",
                        longBody =
                            "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.",
                    )
                )
            }
        }
    }

    TransformingLazyColumn(
        state = tlcState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        modifier = Modifier.background(Color.Black),
    ) {
        items(items = messages, key = { it.title }) { message ->
            val isCurrentlyExpanded = message.title == expandedItemKey
            val revealState = rememberRevealState(initialValue = Covered)
            SwipeToReveal(
                primaryAction = {
                    PrimaryActionButton(
                        onClick = {
                            if (message.title == expandedItemKey) {
                                expandedItemKey = null
                            }
                            messages.remove(message)
                        },
                        icon = { Icon(Icons.Outlined.Delete, contentDescription = "Delete") },
                        text = { Text("Delete") },
                    )
                },
                onSwipePrimaryAction = {
                    if (message.title == expandedItemKey) {
                        expandedItemKey = null
                    }
                    messages.remove(message)
                },
                modifier =
                    Modifier.transformedHeight(this@items, transformationSpec)
                        .graphicsLayer {
                            with(transformationSpec) {
                                applyContainerTransformation(scrollProgress)
                            }
                            // Is needed to disable clipping.
                            compositingStrategy = CompositingStrategy.ModulateAlpha
                            clip = false
                        }
                        .animateItem(),
                revealState = revealState,
                revealDirection = Bidirectional,
            ) {
                TitleCard(
                    onClick = {
                        if (expandedItemKey == message.title) {
                            expandedItemKey = null
                        } else {
                            expandedItemKey = message.title
                        }
                    },
                    title = { Text(message.title) },
                    subtitle = { Text(message.body) },
                    modifier =
                        Modifier.semantics {
                            // Use custom actions to make the primary action accessible
                            customActions =
                                listOf(
                                    CustomAccessibilityAction("Delete") {
                                        if (message.title == expandedItemKey) {
                                            expandedItemKey = null
                                        }
                                        messages.remove(message)
                                        true
                                    }
                                )
                        },
                ) {
                    AnimatedVisibility(visible = isCurrentlyExpanded) {
                        Text(text = message.longBody, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
        }
    }
}

data class MessageItem(val title: String, val body: String, val longBody: String)
