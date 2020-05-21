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

package androidx.ui.benchmark.test

import androidx.compose.Composable
import androidx.compose.mutableStateOf
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.benchmarkFirstCompose
import androidx.ui.benchmark.benchmarkFirstDraw
import androidx.ui.benchmark.benchmarkFirstLayout
import androidx.ui.benchmark.benchmarkFirstMeasure
import androidx.ui.benchmark.toggleStateBenchmarkDraw
import androidx.ui.benchmark.toggleStateBenchmarkLayout
import androidx.ui.benchmark.toggleStateBenchmarkMeasure
import androidx.ui.benchmark.toggleStateBenchmarkRecompose
import androidx.ui.layout.Column
import androidx.ui.material.MaterialTheme
import androidx.ui.material.RadioGroup
import androidx.ui.test.ComposeTestCase
import androidx.ui.integration.test.ToggleableTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for [RadioGroup].
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class RadioGroupBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    private val radioCaseFactory = { RadioGroupTestCase() }

    @Test
    fun first_compose() {
        benchmarkRule.benchmarkFirstCompose(radioCaseFactory)
    }

    @Test
    fun first_measure() {
        benchmarkRule.benchmarkFirstMeasure(radioCaseFactory)
    }

    @Test
    fun first_layout() {
        benchmarkRule.benchmarkFirstLayout(radioCaseFactory)
    }

    @Test
    fun first_draw() {
        benchmarkRule.benchmarkFirstDraw(radioCaseFactory)
    }

    @Test
    fun toggleRadio_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose(radioCaseFactory)
    }

    @Test
    fun toggleRadio_measure() {
        benchmarkRule.toggleStateBenchmarkMeasure(radioCaseFactory)
    }

    @Test
    fun toggleRadio_layout() {
        benchmarkRule.toggleStateBenchmarkLayout(radioCaseFactory)
    }

    @Test
    fun toggleRadio_draw() {
        benchmarkRule.toggleStateBenchmarkDraw(radioCaseFactory)
    }
}

internal class RadioGroupTestCase : ComposeTestCase, ToggleableTestCase {

    private val radiosCount = 10
    private val options = (0 until radiosCount).toList()
    private val select = mutableStateOf(0)

    override fun toggleState() {
        select.value = (select.value + 1) % radiosCount
    }

    @Composable
    override fun emitContent() {
        MaterialTheme {
            RadioGroup {
                Column {
                    options.forEach { item ->
                        RadioGroupTextItem(
                            text = item.toString(),
                            selected = (select.value == item),
                            onSelect = { select.value = item })
                    }
                }
            }
        }
    }
}