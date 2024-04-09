/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3.demos

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val items = listOf(
    "Cupcake",
    "Donut",
    "Eclair",
    "Froyo",
    "Gingerbread",
    "Honeycomb",
    "Ice cream sandwich",
    "Jelly bean",
    "KitKat",
    "Lollipop",
    "Marshmallow",
    "Nougat",
    "Oreo",
    "Pie"
)

@Composable
fun SwipeToDismissDemo() {
    // This is an example of a list of dismissible items, similar to what you would see in an
    // email app. Swiping left reveals a 'delete' icon and swiping right reveals a 'done' icon.
    // The background will start as grey, but once the dismiss threshold is reached, the colour
    // will animate to red if you're swiping left or green if you're swiping right. When you let
    // go, the item will animate out of the way if you're swiping left (like deleting an email) or
    // back to its default position if you're swiping right (like marking an email as read/unread).
    LazyColumn {
        items(items) { item ->
            var unread by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val dismissState = rememberSwipeToDismissBoxState(
                confirmValueChange = {
                    if (it == SwipeToDismissBoxValue.StartToEnd) unread = !unread
                    it != SwipeToDismissBoxValue.StartToEnd
                },
                positionalThreshold = { distance -> distance * .25f }
            )
            SwipeToDismissBox(
                state = dismissState,
                modifier = Modifier.padding(vertical = 4.dp),
                backgroundContent = {
                    val direction = dismissState.dismissDirection
                    val color by animateColorAsState(
                        when (dismissState.targetValue) {
                            SwipeToDismissBoxValue.Settled -> Color.LightGray
                            SwipeToDismissBoxValue.StartToEnd -> Color.Green
                            SwipeToDismissBoxValue.EndToStart -> Color.Red
                        }
                    )
                    val alignment = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd,
                        SwipeToDismissBoxValue.Settled -> Alignment.CenterStart
                        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    }
                    val icon = when (direction) {
                        SwipeToDismissBoxValue.StartToEnd,
                        SwipeToDismissBoxValue.Settled -> Icons.Default.Done
                        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                    }
                    val scale by animateFloatAsState(
                        if (dismissState.targetValue == SwipeToDismissBoxValue.Settled)
                            0.75f else 1f
                    )
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(color)
                            .padding(horizontal = 20.dp),
                        contentAlignment = alignment
                    ) {
                        Icon(
                            icon,
                            contentDescription = "Localized description",
                            modifier = Modifier.scale(scale)
                        )
                    }
                }
            ) {
                Card {
                    ListItem(
                        headlineContent = {
                            Text(item, fontWeight = if (unread) FontWeight.Bold else null)
                        },
                        modifier = Modifier.semantics {
                            // Provide accessible alternatives to swipe actions.
                            val label = if (unread) "Mark Read" else "Mark Unread"
                            customActions = listOf(
                                CustomAccessibilityAction(label) { unread = !unread; true },
                                CustomAccessibilityAction("Delete") {
                                    scope.launch {
                                        dismissState.dismiss(SwipeToDismissBoxValue.EndToStart)
                                    }
                                    true
                                }
                            )
                        },
                        supportingContent = { Text("Swipe me left or right!") },
                    )
                }
            }
        }
    }
}
