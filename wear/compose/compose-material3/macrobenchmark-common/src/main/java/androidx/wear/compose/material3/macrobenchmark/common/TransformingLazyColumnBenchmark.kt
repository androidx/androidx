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

package androidx.wear.compose.material3.macrobenchmark.common

import android.graphics.Point
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.test.uiautomator.By
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.ScreenScaffoldDefaults
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.scrollTransform
import kotlinx.coroutines.launch

val TransformingLazyColumnBenchmark =
    object : MacrobenchmarkScreen {
        override val content: @Composable (BoxScope.() -> Unit)
            get() = {
                val state = rememberTransformingLazyColumnState()
                val coroutineScope = rememberCoroutineScope()
                AppScaffold {
                    ScreenScaffold(
                        state,
                        edgeButton = {
                            EdgeButton(
                                onClick = { coroutineScope.launch { state.scrollToItem(1) } }
                            ) {
                                Text("To top")
                            }
                        }
                    ) {
                        TransformingLazyColumn(
                            state = state,
                            contentPadding =
                                ScreenScaffoldDefaults.contentPaddingWithEdgeButton(
                                    EdgeButtonSize.Small,
                                    start = 10.dp,
                                    end = 10.dp,
                                    top = 20.dp,
                                    extraBottom = 20.dp
                                ),
                            modifier =
                                Modifier.background(MaterialTheme.colorScheme.background)
                                    .semantics { contentDescription = CONTENT_DESCRIPTION }
                        ) {
                            items(5000) {
                                Text(
                                    "Item $it",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier =
                                        Modifier.fillMaxWidth()
                                            // Apply Material 3 Motion transformations.
                                            .scrollTransform(
                                                this,
                                                backgroundColor =
                                                    MaterialTheme.colorScheme.surfaceContainer,
                                                shape = MaterialTheme.shapes.small
                                            )
                                            .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

        override val exercise: MacrobenchmarkScope.() -> Unit
            get() = {
                val list = device.findObject(By.desc(CONTENT_DESCRIPTION))
                // Setting a gesture margin is important otherwise gesture nav is triggered.
                list.setGestureMargin(device.displayWidth / 5)
                repeat(5) {
                    list.drag(Point(list.visibleCenter.x, list.visibleCenter.y / 3))
                    device.waitForIdle()
                }
            }
    }
