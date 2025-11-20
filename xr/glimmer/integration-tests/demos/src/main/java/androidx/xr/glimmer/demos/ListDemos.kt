/**
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package androidx.xr.glimmer.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Vertical
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.glimmer.Text
import androidx.xr.glimmer.list.VerticalList
import androidx.xr.glimmer.surface

internal val ListDemos = listOf(ComposableDemo("VerticalList") { VerticalListDemo() })

@Composable
private fun VerticalListDemo() {
    var itemsCount by remember { mutableIntStateOf(5) }
    var arrangementIndex by remember { mutableIntStateOf(0) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ItemCounter(
            itemsCount = itemsCount,
            onClick = { newValue -> itemsCount = maxOf(0, newValue) },
        )

        VerticalArrangementSwitcher(
            name = verticalArrangements[arrangementIndex].second,
            onNextClick = { arrangementIndex = (arrangementIndex + 1) % verticalArrangements.size },
        )

        VerticalList(
            verticalArrangement = verticalArrangements[arrangementIndex].first,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(itemsCount) { index ->
                Box(
                    Modifier.fillMaxWidth().surface().padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    Text("Item-$index")
                }
            }
        }
    }
}

@Composable
private fun ItemCounter(itemsCount: Int, onClick: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        GlimmerButton({ onClick(itemsCount - 50) }) { Text(text = "-50", fontSize = 16.sp) }
        GlimmerButton({ onClick(itemsCount - 1) }) { Text(text = "-1", fontSize = 16.sp) }
        Text(
            text = itemsCount.toString(),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp),
        )
        GlimmerButton({ onClick(itemsCount + 1) }) { Text(text = "+1", fontSize = 16.sp) }
        GlimmerButton({ onClick(itemsCount + 50) }) { Text(text = "+50", fontSize = 16.sp) }
    }
}

@Composable
private fun VerticalArrangementSwitcher(name: String, onNextClick: () -> Unit) {
    SwitcherButton(
        text = "Arrangement: $name",
        modifier = Modifier.fillMaxWidth(),
        onClick = onNextClick,
    )
}

@Composable
private fun SwitcherButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier = modifier.surface(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text = text, fontSize = 14.sp, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun GlimmerButton(onClick: () -> Unit, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier =
            Modifier.defaultMinSize(minWidth = 70.dp)
                .surface(onClick = onClick, shape = CircleShape)
                .padding(vertical = 10.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

private val verticalArrangements: List<Pair<Vertical, String>> =
    listOf(
        Arrangement.spacedBy(20.dp) to "Spaced by 20.dp",
        Arrangement.Top to "Top",
        Arrangement.Center to "Center",
        Arrangement.Bottom to "Bottom",
        Arrangement.SpaceAround to "SpaceAround",
        Arrangement.SpaceEvenly to "SpaceEvenly",
        Arrangement.SpaceBetween to "SpaceBetween",
    )
