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

import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.ExperimentalWearFoundationApi
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.foundation.rememberActiveFocusRequester
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.integration.demos.common.AdaptiveScreen
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.ButtonDefaults.buttonColors
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

@Composable
fun EdgeButtonBelowLazyColumnDemo() {
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
    AdaptiveScreen {
        val state = rememberLazyListState()
        ScreenScaffold(
            scrollState = state,
            bottomButton = {
                EdgeButton(
                    onClick = {},
                    buttonHeight = ButtonDefaults.EdgeButtonHeightLarge,
                    colors = buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(labels[selectedLabel.intValue], color = Color.White)
                }
            }
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(10.dp, 20.dp, 10.dp, 80.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
        }
    }
}

@Composable
fun EdgeButtonBelowScalingLazyColumnDemo() {
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

    AdaptiveScreen {
        val state = rememberScalingLazyListState()
        ScreenScaffold(
            scrollState = state,
            bottomButton = {
                EdgeButton(
                    onClick = {},
                    buttonHeight = ButtonDefaults.EdgeButtonHeightLarge,
                    colors = buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(labels[selectedLabel.intValue], color = Color.White)
                }
            }
        ) {
            ScalingLazyColumn(
                state = state,
                modifier = Modifier.fillMaxSize(),
                autoCentering = null,
                contentPadding = PaddingValues(10.dp, 20.dp, 10.dp, 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
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
        }
    }
}

@OptIn(ExperimentalWearFoundationApi::class)
@Composable
fun EdgeButtonBelowColumnDemo() {
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
    val bottomButtonHeight = ButtonDefaults.EdgeButtonHeightLarge

    AdaptiveScreen {
        val scrollState = rememberScrollState()
        val focusRequester = rememberActiveFocusRequester()

        ScreenScaffold(
            scrollState = scrollState,
            bottomButton = {
                EdgeButton(
                    onClick = {},
                    buttonHeight = bottomButtonHeight,
                    colors = buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(labels[selectedLabel.intValue], color = Color.White)
                }
            },
            bottomButtonHeight = bottomButtonHeight
        ) {
            Column(
                modifier =
                    Modifier.verticalScroll(scrollState)
                        .rotaryScrollable(
                            RotaryScrollableDefaults.behavior(
                                scrollableState = scrollState,
                                flingBehavior = ScrollableDefaults.flingBehavior()
                            ),
                            focusRequester = focusRequester
                        ),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                repeat(labels.size) {
                    Card(
                        onClick = { selectedLabel.intValue = it },
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        Text(labels[it])
                    }
                }
                Spacer(Modifier.height(bottomButtonHeight))
            }
        }
    }
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
