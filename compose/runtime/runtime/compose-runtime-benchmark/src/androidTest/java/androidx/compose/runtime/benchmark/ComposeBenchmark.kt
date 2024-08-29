/*
 * Copyright 2019 The Android Open Source Project
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.test.annotation.UiThreadTest
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

@LargeTest
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class, ExperimentalTestApi::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ComposeBenchmark : ComposeBenchmarkBase() {

    @UiThreadTest
    @Test
    fun benchmark_01_Compose_OneRect() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureCompose { OneRect(model) }
    }

    @UiThreadTest
    @Test
    fun benchmark_02_Compose_TenRects() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureCompose { TenRects(model) }
    }

    @UiThreadTest
    @Test
    fun benchmark_03_Compose_100Rects() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureCompose { HundredRects(model = model) }
    }

    @UiThreadTest
    @Test
    fun benchmark_04_Recompose_OneRect() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureRecomposeSuspending {
            compose { OneRect(model) }
            update { model.toggle() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_04_Recompose_OneRect_WithRecomposer() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureRecomposeSuspending {
            compose { OneRect(model) }
            update { model.toggle() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_05_Recompose_TenRect_Wide() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureRecomposeSuspending {
            compose { TenRects(model, narrow = false) }
            update { model.toggle() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_06_Recompose_TenRect_Narrow() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureRecomposeSuspending {
            compose { TenRects(model, narrow = true) }
            update { model.toggle() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_07_Recompose_100Rect_Wide() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureRecomposeSuspending {
            compose { HundredRects(model, narrow = false) }
            update { model.toggle() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_08_Recompose_100Rect_Narrow() = runBlockingTestWithFrameClock {
        val model = ColorModel()
        measureRecomposeSuspending {
            compose { HundredRects(model, narrow = true) }
            update { model.toggle() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_10_NestedRowColumnsWithModifier() = runBlockingTestWithFrameClock {
        var pad by mutableStateOf(0)
        val modifier = Modifier.composed { Modifier.padding(pad.dp) }
        measureRecomposeSuspending {
            compose { Column(modifier = modifier) { repeat(100) { Text("Some text") } } }
            update { pad = 10 }
            reset { pad = 0 }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_10_derivedState_reads_compose() = runBlockingTestWithFrameClock {
        val state1 by mutableStateOf(1)
        val state2 by mutableStateOf(3)
        val state3 by mutableStateOf(6)
        val list by derivedStateOf { List(state1 + state2 + state3) { "$it" } }

        measureCompose {
            Column {
                for (i in list.indices) {
                    Text(list[i])
                }
            }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_10_derivedState_reads_recompose() = runBlockingTestWithFrameClock {
        var state1 by mutableStateOf(1)
        var state2 by mutableStateOf(3)
        val state3 by mutableStateOf(6)
        val list by derivedStateOf { List(state1 + state2 + state3) { "$it" } }

        measureRecomposeSuspending {
            compose {
                Column {
                    for (i in list.indices) {
                        Text(list[i])
                    }
                }
            }
            update { state1 += 1 }
            reset { state1 = 1 }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_reverse_list() = runBlockingTestWithFrameClock {
        val state =
            mutableStateListOf(
                1,
                2,
                3,
                4,
                5,
                6,
                7,
                8,
                9,
                10,
                11,
                12,
                13,
                14,
                15,
                16,
                17,
                18,
                19,
                20
            )

        measureRecomposeSuspending {
            compose {
                Column {
                    for (value in state) {
                        key(value) { Text(value.toString()) }
                    }
                }
            }
            update { state.reverse() }
            reset { state.reverse() }
        }
    }

    @UiThreadTest
    @Test
    fun benchmark_f_compose_Rect_1() = runBlockingTestWithFrameClock {
        measureComposeFocused { Rect() }
    }

    @UiThreadTest
    @Test
    fun benchmark_f_compose_Rect_10() = runBlockingTestWithFrameClock {
        measureComposeFocused { repeat(10) { Rect() } }
    }

    @UiThreadTest
    @Test
    fun benchmark_f_compose_Rect_100() = runBlockingTestWithFrameClock {
        measureComposeFocused { repeat(100) { Rect() } }
    }

    @UiThreadTest
    @Test
    fun benchmark_g_group_eliding_focused_1000() = runBlockingTestWithFrameClock {
        measureCompose { repeat(1000) { MyLayout { SimpleText("Value: $it") } } }
    }
}

@Composable
fun MyLayout(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Layout(content = content, measurePolicy = EmptyMeasurePolicy, modifier = modifier)
}

internal val EmptyMeasurePolicy = MeasurePolicy { _, constraints ->
    layout(constraints.minWidth, constraints.minHeight) {}
}

class ColorModel(color: Color = Color.Black) {
    var color: Color by mutableStateOf(color)

    fun toggle() {
        color = if (color == Color.Black) Color.Red else Color.Black
    }
}

private val defaultModifier = Modifier.background(Color.Yellow)

@Composable
private fun Rect() {
    Column(defaultModifier) {}
}

@Composable
private fun SimpleText(text: String) {
    val measurer = rememberTextMeasurer()
    Box(modifier = Modifier.drawBehind { drawText(measurer, text) })
}

@Composable
private fun Rect(color: Color) {
    val modifier = remember(color) { Modifier.background(color) }
    Column(modifier) {}
}

@Composable
fun OneRect(model: ColorModel) {
    Rect(model.color)
}

@Composable fun Observe(body: @Composable () -> Unit) = body()

@Composable
fun TenRects(model: ColorModel, narrow: Boolean = false) {
    if (narrow) {
        Observe { Rect(model.color) }
    } else {
        Rect(model.color)
    }
    repeat(9) { Rect() }
}

@Composable
fun HundredRects(model: ColorModel, narrow: Boolean = false) {
    repeat(100) {
        if (it % 10 == 0)
            if (narrow) {
                Observe { Rect(model.color) }
            } else {
                Rect(model.color)
            }
        else Rect()
    }
}
