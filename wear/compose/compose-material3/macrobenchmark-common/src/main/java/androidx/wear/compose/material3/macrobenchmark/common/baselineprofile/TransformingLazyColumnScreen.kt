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

package androidx.wear.compose.material3.macrobenchmark.common.baselineprofile

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.compose.material3.macrobenchmark.common.MacrobenchmarkScreen

val TransformingLazyColumnScreen =
    object : MacrobenchmarkScreen {
        override val content: @Composable BoxScope.() -> Unit
            get() = {
                val transformationSpec = rememberTransformationSpec()
                val state = rememberTransformingLazyColumnState()
                AppScaffold {
                    ScreenScaffold(state) { contentPadding ->
                        TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                            item {
                                ListHeader(
                                    modifier = Modifier.transformedHeight(this, transformationSpec)
                                ) {
                                    Text("Cards")
                                }
                            }
                            items(count = 100) {
                                Card(
                                    onClick = {},
                                    modifier =
                                        Modifier.transformedHeight(this, transformationSpec)
                                            .animateItem(),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text("Card $it")
                                }
                            }
                            item {
                                ListHeader(
                                    modifier = Modifier.transformedHeight(this, transformationSpec),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text("Buttons")
                                }
                            }
                            items(count = 100) {
                                Button(
                                    onClick = {},
                                    modifier =
                                        Modifier.transformedHeight(this, transformationSpec)
                                            .animateItem(),
                                    transformation = SurfaceTransformation(transformationSpec),
                                ) {
                                    Text("Button $it")
                                }
                            }
                        }
                    }
                }
                LaunchedEffect(Unit) { state.animateScrollToItem(202) }
            }
    }
