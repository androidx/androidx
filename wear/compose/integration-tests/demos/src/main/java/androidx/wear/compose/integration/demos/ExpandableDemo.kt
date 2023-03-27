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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.ExpandableItemsDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.OutlinedChip
import androidx.wear.compose.material.ExpandableItemsState
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.rememberExpandableItemsState
import androidx.wear.compose.material.expandableItem
import androidx.wear.compose.material.expandableItems

@Composable
fun ExpandableListItems() {
    val state = rememberExpandableItemsState()

    val items = List(10) { "Item $it" }
    val top = items.take(3)
    val rest = items.drop(3)
    val color = MaterialTheme.colors.secondary
    ContainingScalingLazyColumn {
        items(top.size) {
            DemoItem(top[it], color = color)
        }
        expandableItems(state, rest.size) {
            DemoItem(rest[it], color = color)
        }
        item { ExpandButton(state) }
    }
}

@Composable
fun ExpandableText() {
    val state = rememberExpandableItemsState()

    ContainingScalingLazyColumn {
        expandableItem(state) { expanded ->
            Text(
                "Account Alert: you have made a large purchase.\n" +
                    "We have noticed that a large purchase was charged to " +
                    "your credit card account. " +
                    "Please contact us if you did not perform this purchase. " +
                    "Our Customer Service team is available 24 hours a day, " +
                    "7 days a week to answer your account or product support question.",
                maxLines = if (expanded) 20 else 3,
                modifier = Modifier.padding(horizontal = 10.dp)
            )
        }
        item { ExpandButton(state) }
   }
}

@Composable
private fun ContainingScalingLazyColumn(content: ScalingLazyListScope.() -> Unit) {
    val color = MaterialTheme.colors.primary
    ScalingLazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        repeat(10) {
            item {
                DemoItem("Item $it - before", color)
            }
        }
        demoSeparator()
        content()
        demoSeparator()
        repeat(10) {
            item {
                DemoItem("Item $it - after", color)
            }
        }
    }
}

@Composable
private fun ExpandButton(expandableItemsState: ExpandableItemsState) = OutlinedChip(
    label = {
        Text(if (expandableItemsState.expanded) "Show Less" else "Show More")
        Spacer(Modifier.size(10.dp))
        ExpandableItemsDefaults.Chevron(
            expandableItemsState.expandProgress,
            color = MaterialTheme.colors.primary,
            modifier = Modifier
                .size(15.dp, 11.dp)
                .align(CenterVertically)
        )
    },
    onClick = { expandableItemsState.toggle() }
)

private fun ScalingLazyListScope.demoSeparator() = item {
    Box(
        Modifier
            .padding(horizontal = 10.dp, vertical = 5.dp)
            .background(Color.White)
            .fillMaxWidth()
            .height(2.dp)
    )
}

@Composable
private fun DemoItem(
    label: String,
    color: Color
) = Chip(
    label = { Text(label) },
    onClick = { },
    secondaryLabel = { Text("line 2 - Secondary") },
    icon = { DemoIcon(resourceId = R.drawable.ic_play) },
    colors = ChipDefaults.primaryChipColors(backgroundColor = color)
)
