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

package androidx.wear.compose.material3.samples

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.LocalReduceMotion
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Card
import androidx.wear.compose.material3.CompactButton
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight

@Preview
@Composable
fun TransformingLazyColumnButtonsSample() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                items(count = 50) {
                    Button(
                        onClick = {},
                        modifier =
                            Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Item $it")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TransformingLazyColumnExpandableCardSample() {
    val state = rememberTransformingLazyColumnState()
    val transformationSpec = rememberTransformationSpec()
    var expandedIndex by remember { mutableIntStateOf(-1) }
    AppScaffold {
        ScreenScaffold(state) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                items(count = 50) { cardIndex ->
                    TitleCard(
                        onClick = {
                            expandedIndex = if (expandedIndex == cardIndex) -1 else cardIndex
                        },
                        modifier =
                            Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                        transformation = SurfaceTransformation(transformationSpec),
                        title = { Text("Card $cardIndex") },
                        subtitle = {
                            AnimatedVisibility(expandedIndex == cardIndex) {
                                Text("Expanded content is available here")
                            }
                        },
                        content = { Text("Tap on Card to expand") },
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun TransformingLazyColumnReducedMotionSample() {
    // This simulates how TransformingLazyColumn looks like when reduce motion is enabled.
    CompositionLocalProvider(LocalReduceMotion provides true) {
        val transformationSpec = rememberTransformationSpec()
        val state = rememberTransformingLazyColumnState()
        AppScaffold {
            ScreenScaffold(state) { contentPadding ->
                TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                    items(count = 10) {
                        Button(
                            onClick = {},
                            modifier =
                                Modifier.fillMaxWidth()
                                    .transformedHeight(this, transformationSpec)
                                    .animateItem(),
                            transformation = SurfaceTransformation(transformationSpec),
                        ) {
                            Text("Item $it")
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun TransformingLazyColumnAnimationSample() {
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()
    var elements by remember { mutableStateOf(listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)) }
    var nextElement by remember { mutableIntStateOf(10) }

    fun addCardAfter(index: Int) {
        elements =
            elements.subList(0, index + 1) +
                listOf(nextElement++) +
                elements.subList(index + 1, elements.count())
    }

    fun removeCardAt(index: Int) {
        elements = elements.subList(0, index) + elements.subList(index + 1, elements.count())
    }

    AppScaffold {
        ScreenScaffold(
            state,
            edgeButton = {
                EdgeButton(onClick = { elements = elements.shuffled() }) { Text("Shuffle") }
            },
        ) { contentPadding ->
            TransformingLazyColumn(state = state, contentPadding = contentPadding) {
                itemsIndexed(elements, key = { _, key -> key }) { index, cardKey ->
                    Card(
                        onClick = {},
                        modifier =
                            Modifier.transformedHeight(this, transformationSpec).animateItem(),
                        transformation = SurfaceTransformation(transformationSpec),
                    ) {
                        Text("Card $cardKey")
                        Row {
                            Spacer(modifier = Modifier.weight(1f))
                            CompactButton(
                                onClick = { removeCardAt(index) },
                                enabled = elements.count() > 1,
                            ) {
                                Text("-")
                            }
                            CompactButton(onClick = { addCardAfter(index) }) { Text("+") }
                        }
                    }
                }
            }
        }
    }
}
