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

package androidx.compose.runtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.random.Random
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BenchmarkValidationTests : BaseComposeTest() {
    @get:Rule override val activityRule = makeTestActivityRule()

    @Test
    fun testGroupElidingFocused() {
        var state by mutableStateOf(true)
        compose {
                if (state) {
                    repeat(100) { MyLayout { SimpleText("Value: $it") } }
                }
            }
            .then { state = false }
            .then { state = true }
            .then { state = false }
            .then { state = true }
    }

    @Test
    fun testRemoveManyGroups() {
        var includeGroups by mutableStateOf(true)
        var called = 0
        compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    if (includeGroups) {
                        repeat(100) {
                            key(it) {
                                Pixel(color = Color.Blue)
                                called++
                            }
                        }
                    }
                }
            }
            .then {
                assertEquals(100, called)
                called = 0
                includeGroups = false
            }
            .then {
                assertEquals(0, called)
                includeGroups = true
            }
            .then {
                assertEquals(100, called)
                called = 0
                includeGroups = false
            }
            .then {
                assertEquals(0, called)
                includeGroups = true
            }
            .then { assertEquals(100, called) }
            .done()
    }

    @Test
    fun testInsertAlternatingGroups() {
        var insertAlternatingGroups by mutableStateOf(false)
        compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    repeat(1000) { index ->
                        if (index % 2 == 0 || insertAlternatingGroups) {
                            key(index) { Pixel(color = Color.Blue) }
                        }
                    }
                }
            }
            .then { insertAlternatingGroups = true }
            .repeatedly(10) { activity, index -> insertAlternatingGroups = index % 2 == 0 }
    }

    @Test
    fun testUpdateManyNestedGroups() {
        var seed by mutableIntStateOf(1337)
        compose {
                val random = remember(seed) { Random(seed) }
                MatryoshkaLayout(
                    depth = 100,
                    content = {
                        MinimalBox {
                            Pixel(color = Color(random.nextInt()))
                            Pixel(color = Color.Red)
                            Pixel(color = Color.Green)
                            Pixel(color = Color.Blue)
                        }
                        MinimalBox { NonRenderingText("abcdef") }
                        NonRenderingText(
                            text = random.nextString(),
                            textColor = Color(random.nextInt()),
                            textSize = random.nextInt(6, 32).dp,
                            ellipsize = random.nextBoolean(),
                            minLines = random.nextInt(),
                            maxLines = random.nextInt(),
                        )
                    },
                )
            }
            .then { seed++ }
            .repeatedly(4) { _, _ -> seed++ }
    }
}

@Composable
fun MyLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, measurePolicy = EmptyMeasurePolicy, modifier = modifier)
}

internal val EmptyMeasurePolicy = MeasurePolicy { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
}

@Composable
private fun SimpleText(text: String) {
    val measurer = rememberTextMeasurer()
    Box(modifier = Modifier.drawBehind { drawText(measurer, text) })
}

@Composable
private fun Pixel(color: Color) {
    Layout(modifier = Modifier.background(color)) { _, _ -> layout(1, 1) {} }
}

@Composable
private fun MatryoshkaLayout(depth: Int, content: @Composable (depth: Int) -> Unit) {
    if (depth <= 0) {
        content(0)
    } else {
        Layout(
            content = {
                content(depth)
                MatryoshkaLayout(depth - 1, content)
            },
            measurePolicy = MinimalBoxMeasurePolicy,
        )
    }
}

@Composable
private fun MinimalBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier, MinimalBoxMeasurePolicy)
}

private val MinimalBoxMeasurePolicy = MeasurePolicy { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints) }
    val (usedWidth, usedHeight) =
        placeables.fold(initial = IntOffset(0, 0)) { (maxWidth, maxHeight), placeable ->
            IntOffset(
                maxOf(maxWidth, placeable.measuredWidth),
                maxOf(maxHeight, placeable.measuredHeight),
            )
        }

    layout(width = usedWidth, height = usedHeight) { placeables.forEach { it.place(0, 0) } }
}

@Composable
private fun NonRenderingText(
    text: String,
    textColor: Color = Color.Unspecified,
    textSize: Dp = Dp.Unspecified,
    ellipsize: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE,
) {
    use(text)
    use(textColor.value.toInt())
    use(textSize.value)
    use(ellipsize)
    use(minLines)
    use(maxLines)
    Layout { _, _ -> layout(1, 1) {} }
}

private fun Random.nextString(length: Int = 16) =
    buildString(length) { repeat(length) { append(nextInt('A'.code, 'z'.code).toChar()) } }

@Suppress("UNUSED_PARAMETER") private fun use(value: Any?) {}
