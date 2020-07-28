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
import androidx.compose.material.Colors
import androidx.ui.integration.test.material.ImmutableColorsTestCase
import androidx.ui.integration.test.material.ObservableColorsTestCase
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Benchmark to compare performance of the default observable [Colors] that will be memoized
 * and mutated when incoming values change, compared to a simple immutable [Colors] that
 * will cause all consumers to be recomposed whenever its value changes.
 */
@LargeTest
@RunWith(JUnit4::class)
class ColorsBenchmark {
    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun observablePalette_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose { ObservableColorsTestCase() }
    }

    @Test
    fun immutablePalette_recompose() {
        benchmarkRule.toggleStateBenchmarkRecompose { ImmutableColorsTestCase() }
    }
}
