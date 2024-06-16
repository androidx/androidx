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

package androidx.compose.runtime.benchmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.random.Random
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class SlotTableIntegrationBenchmark : ComposeBenchmarkBase() {

    @UiThreadTest
    @Test
    fun create() = runBlockingTestWithFrameClock {
        measureCompose {
            Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                repeat(100) { key(it) { Pixel(color = Color.Blue) } }
            }
        }
    }

    @UiThreadTest
    @Test
    fun removeManyGroups() = runBlockingTestWithFrameClock {
        var includeGroups by mutableStateOf(true)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    if (includeGroups) {
                        repeat(100) { key(it) { Pixel(color = Color.Blue) } }
                    }
                }
            }
            update { includeGroups = false }
            reset { includeGroups = true }
        }
    }

    @UiThreadTest
    @Test
    fun removeAlternatingGroups() = runBlockingTestWithFrameClock {
        var insertAlternatingGroups by mutableStateOf(true)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    repeat(100) { index ->
                        if (index % 2 == 0 || insertAlternatingGroups) {
                            key(index) { Pixel(color = Color.Blue) }
                        }
                    }
                }
            }
            update { insertAlternatingGroups = false }
            reset { insertAlternatingGroups = true }
        }
    }

    @UiThreadTest
    @Test
    fun removeManyReplaceGroups() = runBlockingTestWithFrameClock {
        var insertAlternatingGroups by mutableStateOf(true)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    repeat(100) { index ->
                        if (index % 2 == 0 || insertAlternatingGroups) {
                            Pixel(color = Color(red = 0, green = 2 * index, blue = 0))
                        }
                    }
                }
            }
            update { insertAlternatingGroups = false }
            reset { insertAlternatingGroups = true }
        }
    }

    @UiThreadTest
    @Test
    fun insertManyGroups() = runBlockingTestWithFrameClock {
        var includeGroups by mutableStateOf(false)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    if (includeGroups) {
                        repeat(100) { key(it) { Pixel(color = Color.Blue) } }
                    }
                }
            }
            update { includeGroups = true }
            reset { includeGroups = false }
        }
    }

    @UiThreadTest
    @Test
    fun insertAlternatingGroups() = runBlockingTestWithFrameClock {
        var insertAlternatingGroups by mutableStateOf(false)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    repeat(100) { index ->
                        if (index % 2 == 0 || insertAlternatingGroups) {
                            key(index) { Pixel(color = Color.Blue) }
                        }
                    }
                }
            }
            update { insertAlternatingGroups = true }
            reset { insertAlternatingGroups = false }
        }
    }

    @UiThreadTest
    @Test
    fun insertManyReplaceGroups() = runBlockingTestWithFrameClock {
        var insertAlternatingGroups by mutableStateOf(false)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    repeat(100) { index ->
                        if (index % 2 == 0 || insertAlternatingGroups) {
                            Pixel(color = Color(red = 0, green = 2 * index, blue = 0))
                        }
                    }
                }
            }
            update { insertAlternatingGroups = true }
            reset { insertAlternatingGroups = false }
        }
    }

    @UiThreadTest
    @Test
    fun updateManyNestedGroups() = runBlockingTestWithFrameClock {
        var seed by mutableIntStateOf(1337)
        measureRecomposeSuspending {
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
                    }
                )
            }
            update { seed++ }
        }
    }

    @UiThreadTest
    @Test
    fun updateDisjointGroups() = runBlockingTestWithFrameClock {
        var seed by mutableIntStateOf(1337)
        measureRecomposeSuspending {
            compose {
                MinimalBox {
                    repeat(10) { container ->
                        MinimalBox {
                            MatryoshkaLayout(
                                depth = 100,
                                content = { depth ->
                                    if (depth > 50) {
                                        val random = Random(seed * container + depth)
                                        NonRenderingText(
                                            text = random.nextString(),
                                            textColor = Color(random.nextInt()),
                                            textSize = random.nextInt(6, 32).dp,
                                            ellipsize = random.nextBoolean(),
                                            minLines = random.nextInt(),
                                            maxLines = random.nextInt(),
                                        )
                                    } else {
                                        NonRenderingText("foo")
                                    }
                                }
                            )
                        }
                    }
                }
            }
            update { seed++ }
        }
    }

    @UiThreadTest
    @Test
    fun updateDeepCompositionLocalHierarchy() = runBlockingTestWithFrameClock {
        val PixelColorLocal = compositionLocalOf { Color.Unspecified }
        var seed by mutableIntStateOf(1337)
        measureRecomposeSuspending {
            compose {
                val random = remember(seed) { Random(seed) }
                Pixel(PixelColorLocal.current)
                CompositionLocalProvider(PixelColorLocal provides Color(random.nextInt())) {
                    Pixel(PixelColorLocal.current)
                    CompositionLocalProvider(PixelColorLocal provides Color(random.nextInt())) {
                        Pixel(PixelColorLocal.current)
                        CompositionLocalProvider(PixelColorLocal provides Color(random.nextInt())) {
                            Pixel(PixelColorLocal.current)
                            CompositionLocalProvider(
                                PixelColorLocal provides Color(random.nextInt())
                            ) {
                                Pixel(PixelColorLocal.current)
                                CompositionLocalProvider(
                                    PixelColorLocal provides Color(random.nextInt())
                                ) {
                                    Pixel(PixelColorLocal.current)
                                    CompositionLocalProvider(
                                        PixelColorLocal provides Color(random.nextInt())
                                    ) {
                                        Pixel(PixelColorLocal.current)
                                        CompositionLocalProvider(
                                            PixelColorLocal provides Color(random.nextInt())
                                        ) {
                                            Pixel(PixelColorLocal.current)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            update { seed++ }
        }
    }

    @UiThreadTest
    @Test
    fun reverseGroups() = runBlockingTestWithFrameClock {
        val originalItems = (1..100).toList()
        var keys by mutableStateOf(originalItems)
        measureRecomposeSuspending {
            compose {
                Column(modifier = Modifier.size(width = 20.dp, height = 300.dp)) {
                    keys.forEach { key(it) { Pixel(color = Color.Blue) } }
                }
            }
            update { keys = keys.reversed() }
            reset { keys = originalItems }
        }
    }
}

@Composable
private fun Pixel(color: Color) {
    Layout(modifier = Modifier.background(color)) { _, _ -> layout(1, 1) {} }
}

@Composable
private fun NonRenderingText(
    text: String,
    textColor: Color = Color.Unspecified,
    textSize: Dp = Dp.Unspecified,
    ellipsize: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = Int.MAX_VALUE
) {
    use(text)
    use(textColor.value.toInt())
    use(textSize.value)
    use(ellipsize)
    use(minLines)
    use(maxLines)
    Layout { _, _ -> layout(1, 1) {} }
}

@Composable
private fun MinimalBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier, MinimalBoxMeasurePolicy)
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
            measurePolicy = MinimalBoxMeasurePolicy
        )
    }
}

private val MinimalBoxMeasurePolicy = MeasurePolicy { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints) }
    val (usedWidth, usedHeight) =
        placeables.fold(initial = IntOffset(0, 0)) { (maxWidth, maxHeight), placeable ->
            IntOffset(
                maxOf(maxWidth, placeable.measuredWidth),
                maxOf(maxHeight, placeable.measuredHeight)
            )
        }

    layout(width = usedWidth, height = usedHeight) { placeables.forEach { it.place(0, 0) } }
}

private fun Random.nextString(length: Int = 16) =
    buildString(length) { repeat(length) { append(nextInt('A'.code, 'z'.code).toChar()) } }

@Suppress("UNUSED_PARAMETER") private fun use(value: Any?) {}

@Suppress("UNUSED_PARAMETER") private fun use(value: Int) {}

@Suppress("UNUSED_PARAMETER") private fun use(value: Long) {}

@Suppress("UNUSED_PARAMETER") private fun use(value: Float) {}

@Suppress("UNUSED_PARAMETER") private fun use(value: Double) {}

@Suppress("UNUSED_PARAMETER") private fun use(value: Boolean) {}
