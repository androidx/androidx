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

import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.toggleStateBenchmarkRecompose
import androidx.ui.material.ColorPalette
import androidx.ui.integration.test.material.ImmutableColorPaletteTestCase
import androidx.ui.integration.test.material.ObservableColorPaletteTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Benchmark to compare performance of the default observable [ColorPalette] that will be memoized
 * and mutated when incoming values change, compared to a simple immutable [ColorPalette] that
 * will cause all consumers to be recomposed whenever its value changes.
 */
@LargeTest
@RunWith(JUnit4::class)
class ColorPaletteBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun observablePalette_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose { ObservableColorPaletteTestCase() }
    }

    @Test
    fun immutablePalette_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose { ImmutableColorPaletteTestCase() }
    }
}
