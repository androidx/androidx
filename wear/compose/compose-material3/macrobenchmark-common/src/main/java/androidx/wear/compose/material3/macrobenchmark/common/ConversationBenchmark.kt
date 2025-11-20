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

package androidx.wear.compose.material3.macrobenchmark.common

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBox
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text

object ConversationBenchmark : MacrobenchmarkScreen {

    override val content: @Composable (BoxScope.() -> Unit)
        get() = {
            val state = rememberScalingLazyListState()
            AppScaffold {
                ScreenScaffold(
                    scrollState = state,
                    edgeButton = {
                        EdgeButton(
                            onClick = {},
                            modifier =
                                // In case user starts scrolling from the EdgeButton.
                                Modifier.scrollable(
                                    state,
                                    orientation = Orientation.Vertical,
                                    reverseDirection = true,
                                    // An overscroll effect should be applied to the EdgeButton for
                                    // proper
                                    // scrolling behavior.
                                    overscrollEffect = rememberOverscrollEffect(),
                                ),
                        ) {
                            Text("Open on Phone")
                        }
                    },
                ) { contentPadding ->
                    ScalingLazyColumn(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        contentPadding = contentPadding,
                        state = state,
                        modifier =
                            Modifier.fillMaxSize()
                                .background(MaterialTheme.colorScheme.background)
                                .semantics { contentDescription = CONTENT_DESCRIPTION },
                        autoCentering = null,
                    ) {
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                FilledIconButton(onClick = {}) {
                                    Icon(Icons.Outlined.AccountBox, "Store")
                                }
                                FilledIconButton(onClick = {}) {
                                    Icon(Icons.Outlined.Delete, "Delete")
                                }
                                FilledIconButton(onClick = {}) { Icon(Icons.Outlined.Star, "Star") }
                            }
                        }
                        item {
                            Text(
                                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus condimentum rhoncus est volutpat venenatis. Fusce semper, sapien ut venenatis pellentesque, lorem dui aliquam sapien, non pharetra diam neque id mi.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                        item {
                            Text(
                                "Suspendisse sollicitudin, metus ut gravida semper, nunc ipsum ullamcorper nunc, ut maximus nulla nunc dignissim justo. Duis nec nisi leo. Proin tristique massa mi, imperdiet tempus nibh vulputate quis.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                        item {
                            Text(
                                "Morbi sagittis eget neque sagittis egestas. Quisque viverra arcu a cursus dignissim.",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        item {
                            Button(
                                onClick = {},
                                label = { Text("Mark as unread", Modifier.fillMaxWidth()) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            FilledTonalButton(
                                onClick = {},
                                label = { Text("Reply", Modifier.fillMaxWidth()) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        item {
                            FilledTonalButton(
                                onClick = {},
                                label = { Text("Reply all", Modifier.fillMaxWidth()) },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }

    override val exercise: MacrobenchmarkScope.() -> Unit
        get() = {
            repeat(5) {
                repeat(times = 4) {
                    // scroll up
                    device.swipe(
                        device.displayWidth / 2,
                        device.displayHeight * 9 / 10,
                        device.displayWidth / 2,
                        device.displayHeight / 10,
                        10,
                    )
                }
                device.waitForIdle()
                repeat(times = 4) {
                    // scroll down
                    device.swipe(
                        device.displayWidth / 2,
                        device.displayHeight / 10,
                        device.displayWidth / 2,
                        device.displayHeight * 9 / 10,
                        10,
                    )
                }
                device.waitForIdle()
            }
        }
}
