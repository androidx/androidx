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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListItemInfo
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.integration.demos.common.AdaptiveScreen
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Text

@Composable
fun EdgeButtonBelowListDemo() {
    // NOTE: This demo recomposes the Edge Button when it's appearing / disappearing.
    AdaptiveScreen {
        val state = rememberScalingLazyListState()
        val screenHeightPx =
            with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val heightPx = remember {
            derivedStateOf {
                val marginPx = 5f // !?
                var lastItemInfo: ScalingLazyListItemInfo? = null
                state.layoutInfo.visibleItemsInfo.fastForEach { ii ->
                    if (ii.index == state.layoutInfo.totalItemsCount - 1) lastItemInfo = ii
                }
                lastItemInfo?.let {
                    val bottomEdge = it.offset + screenHeightPx / 2 + it.size / 2
                    (screenHeightPx - bottomEdge - marginPx).coerceAtLeast(0f)
                } ?: 0f
            }
        }

        val labels =
            listOf(
                "Hi",
                "Hello World",
                "Hello world again?",
                "More content as we add stuff",
                "I don't know if this will fit now, testing",
                "Really long text that it's going to take multiple lines",
                "And now we are really pushing it because the screen is really small",
            )
        val selectedLabel = remember { mutableIntStateOf(0) }

        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            ScalingLazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                autoCentering = null,
                contentPadding = PaddingValues(10.dp, 20.dp, 10.dp, 100.dp)
            ) {
                items(labels.size) {
                    Card(
                        onClick = { selectedLabel.intValue = it },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(labels[it])
                    }
                }
            }
            // We isolate the call to EdgeButton to a function so only that is recomposed when the
            // height changes.
            EdgeButtonCall(heightPx) {
                Text(
                    labels[selectedLabel.intValue],
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                )
            }
        }
    }
}

@Composable
private fun EdgeButtonCall(heightPx: State<Float>, content: @Composable RowScope.() -> Unit) {
    val heightDp = with(LocalDensity.current) { heightPx.value.toDp() }
    EdgeButton(
        onClick = {},
        buttonHeight = ButtonDefaults.EdgeButtonHeightLarge,
        colors = buttonColors(containerColor = Color.DarkGray),
        modifier = Modifier.height(heightDp),
        content = content
    )
}

@Suppress("PrimitiveInCollection")
@Composable
fun EdgeButtonSizeDemo() {
    val sizes =
        listOf(
            ButtonDefaults.EdgeButtonHeightExtraSmall,
            ButtonDefaults.EdgeButtonHeightSmall,
            ButtonDefaults.EdgeButtonHeightMedium,
            ButtonDefaults.EdgeButtonHeightLarge
        )
    val sizesNames = listOf("XS", "S", "M", "L")
    val size = remember { mutableIntStateOf(0) }

    AdaptiveScreen {
        Column(
            Modifier.align(Alignment.TopCenter).fillMaxSize().padding(top = 0.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            repeat(2) { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(2) { column ->
                        val ix = row * 2 + column
                        CompactButton(
                            onClick = { size.intValue = ix },
                            colors =
                                if (ix == size.intValue) ButtonDefaults.filledTonalButtonColors()
                                else ButtonDefaults.outlinedButtonColors(),
                            border =
                                if (ix == size.intValue) null
                                else ButtonDefaults.outlinedButtonBorder(true),
                        ) {
                            Text(sizesNames[ix])
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text("Content")
            }
            EdgeButton(
                onClick = {},
                buttonHeight = sizes[size.intValue],
                colors = ButtonDefaults.outlinedButtonColors(),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true)
            ) {
                CheckIcon()
            }
        }
    }
}
