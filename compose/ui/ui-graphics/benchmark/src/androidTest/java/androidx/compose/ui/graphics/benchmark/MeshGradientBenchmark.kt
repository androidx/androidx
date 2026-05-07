/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.compose.ui.graphics.benchmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkFirstDraw
import androidx.compose.testutils.benchmark.benchmarkFirstLayout
import androidx.compose.testutils.benchmark.benchmarkFirstMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkDraw
import androidx.compose.testutils.benchmark.toggleStateBenchmarkLayout
import androidx.compose.testutils.benchmark.toggleStateBenchmarkMeasure
import androidx.compose.testutils.benchmark.toggleStateBenchmarkRecompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.meshGradient
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class MeshGradientBenchmark(private val rows: Int, private val columns: Int) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "row_{0}, column_{1}")
        fun data() = listOf(arrayOf(2, 2), arrayOf(5, 5), arrayOf(10, 10))
    }

    @get:Rule val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun createMeshGradientModifier_compose() {
        benchmarkRule.benchmarkFirstCompose { CreateMeshGradientModifierTestCase(rows, columns) }
    }

    @Test
    fun createMeshGradientModifier_measure() {
        benchmarkRule.benchmarkFirstMeasure { CreateMeshGradientModifierTestCase(rows, columns) }
    }

    @Test
    fun createMeshGradientModifier_layout() {
        benchmarkRule.benchmarkFirstLayout { CreateMeshGradientModifierTestCase(rows, columns) }
    }

    @Test
    fun createMeshGradientModifier_draw() {
        benchmarkRule.benchmarkFirstDraw { CreateMeshGradientModifierTestCase(rows, columns) }
    }

    @Test
    fun updateMeshGradientModifier_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(
            { UpdateMeshGradientModifierTestCase(rows, columns) },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun updateMeshGradientModifier_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            { UpdateMeshGradientModifierTestCase(rows, columns) },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun resizeMeshGradientModifier_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(
            { ResizeMeshGradientModifierTestCase(rows, columns) },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun resizeMeshGradientModifier_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(
            { ResizeMeshGradientModifierTestCase(rows, columns) },
            assertOneRecomposition = false,
        )
    }

    @Test
    fun resizeMeshGradientModifier_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(
            { ResizeMeshGradientModifierTestCase(rows, columns) },
            assertOneRecomposition = false,
        )
    }
}

private class CreateMeshGradientModifierTestCase(private val rows: Int, private val columns: Int) :
    LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        Box(
            Modifier.fillMaxSize().meshGradient(rows, columns, false) {
                for (r in 0..rows) {
                    for (c in 0..columns) {
                        setVertex(
                            r,
                            c,
                            position = Offset(c / columns.toFloat(), r / rows.toFloat()),
                            color = Color.Red,
                        )
                    }
                }
            }
        )
    }
}

private class UpdateMeshGradientModifierTestCase(private val rows: Int, private val columns: Int) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleState = false
    private lateinit var colorState: MutableState<Color>

    @Composable
    override fun MeasuredContent() {
        colorState = remember { mutableStateOf(Color.Red) }
        val currentColor = colorState.value
        Box(
            Modifier.fillMaxSize().meshGradient(rows, columns, false) {
                for (r in 0..rows) {
                    for (c in 0..columns) {
                        setVertex(
                            r,
                            c,
                            position = Offset(c / columns.toFloat(), r / rows.toFloat()),
                            color = currentColor,
                        )
                    }
                }
            }
        )
    }

    override fun toggleState() {
        toggleState = !toggleState
        colorState.value = if (toggleState) Color.Blue else Color.Red
    }
}

private class ResizeMeshGradientModifierTestCase(private val rows: Int, val columns: Int) :
    LayeredComposeTestCase(), ToggleableTestCase {
    private var toggleState = false
    private lateinit var boxSizeState: MutableState<Dp>

    @Composable
    override fun MeasuredContent() {
        boxSizeState = remember { mutableStateOf(100.dp) }
        Box(
            Modifier.size(boxSizeState.value).meshGradient(rows, columns, false) {
                for (r in 0..rows) {
                    for (c in 0..columns) {
                        setVertex(
                            r,
                            c,
                            position = Offset(c / columns.toFloat(), r / rows.toFloat()),
                            color = Color.Red,
                        )
                    }
                }
            }
        )
    }

    override fun toggleState() {
        toggleState = !toggleState
        boxSizeState.value = if (toggleState) 200.dp else 100.dp
    }
}
