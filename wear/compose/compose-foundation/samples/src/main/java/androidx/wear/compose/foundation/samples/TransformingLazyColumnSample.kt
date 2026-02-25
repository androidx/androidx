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

package androidx.wear.compose.foundation.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnDefaults
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.rotary.RotaryScrollableDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ButtonDefaults
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import kotlin.math.abs
import kotlinx.coroutines.launch

@Sampled
@Preview
@Composable
fun SimpleTransformingLazyColumnSample() {
    val transformationSpec = rememberTransformationSpec()
    TransformingLazyColumn(contentPadding = PaddingValues(20.dp)) {
        items(count = 10) { index ->
            Button(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = {},
            ) {
                Text(text = "Item $index")
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnWithSnapSample() {
    val transformationSpec = rememberTransformationSpec()
    val state = rememberTransformingLazyColumnState()
    TransformingLazyColumn(
        rotaryScrollableBehavior = RotaryScrollableDefaults.snapBehavior(scrollableState = state),
        flingBehavior = TransformingLazyColumnDefaults.snapFlingBehavior(state = state),
        modifier = Modifier.fillMaxWidth(),
        state = state,
        contentPadding = PaddingValues(20.dp),
    ) {
        items(count = 20) { index ->
            Button(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, transformationSpec),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = {},
            ) {
                Text(text = "Item $index")
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnAnimateItemSample() {
    val state = rememberTransformingLazyColumnState()

    var list by remember { mutableStateOf(listOf("1", "2", "3")) }

    var next by remember { mutableIntStateOf(4) }

    Box(Modifier.fillMaxSize()) {
        TransformingLazyColumn(
            state = state,
            contentPadding = PaddingValues(5.dp),
            modifier = Modifier.background(Color.Black).fillMaxSize(),
        ) {
            items(list.size, key = { list[it] }) {
                Text(
                    "Item ${list[it]}",
                    Modifier.animateItem().clickable {
                        list = list.filter { elem -> elem != list[it] }
                    },
                )
            }
        }
        Text(
            "+",
            Modifier.align(Alignment.CenterStart).padding(horizontal = 5.dp).clickable {
                if (list.size < 25) list = list + "${next++}"
            },
        )
        Text(
            "S",
            Modifier.align(Alignment.CenterEnd).padding(horizontal = 5.dp).clickable {
                list = list.shuffled()
            },
        )
    }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnScrollToItemSample() {
    val state =
        rememberTransformingLazyColumnState(
            // Customize initial scroll position of the TransformingLazyColumn.
            initialAnchorItemIndex = 10
        )
    val coroutineScope = rememberCoroutineScope()

    TransformingLazyColumn(
        modifier = Modifier.background(Color.Black),
        state = state,
        contentPadding = PaddingValues(vertical = 20.dp),
    ) {
        items(count = 20) {
            Text(
                "Item $it",
                modifier =
                    Modifier.drawBehind {
                            val isCentered =
                                it == state.anchorItemIndex &&
                                    abs(state.anchorItemScrollOffset) < size.height
                            drawRect(if (isCentered) Color.Green else Color.DarkGray)
                        }
                        .padding(5.dp)
                        .clickable { coroutineScope.launch { state.scrollToItem(it) } },
            )
        }

        item {
            Text(
                "Scroll to top",
                modifier =
                    Modifier.clickable { coroutineScope.launch { state.animateScrollToItem(0) } },
            )
        }
    }

    LaunchedEffect(state.anchorItemIndex) { println("Anchor item index: ${state.anchorItemIndex}") }
}

@Sampled
@Preview
@Composable
fun TransformingLazyColumnMinimumVerticalContentPaddingSample() {
    val transformationSpec = rememberTransformationSpec()

    TransformingLazyColumn(contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(count = 20) { index ->
            Button(
                modifier =
                    Modifier.fillMaxWidth()
                        .transformedHeight(this, transformationSpec)
                        .minimumVerticalContentPadding(
                            ButtonDefaults.minimumVerticalListContentPadding
                        ),
                transformation = SurfaceTransformation(transformationSpec),
                onClick = {},
            ) {
                Text(text = "Item $index")
            }
        }
    }
}
