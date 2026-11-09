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

package androidx.compose.ui.benchmark

import android.view.View
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.compose.ui.layout.Ruler
import androidx.compose.ui.layout.RulerMapCapacity
import androidx.compose.ui.layout.VerticalRuler
import androidx.compose.ui.layout.WindowInsetsRulers
import androidx.compose.ui.layout.WindowInsetsRulersProvider
import androidx.compose.ui.layout.WindowInsetsWatcher
import androidx.core.view.WindowInsetsCompat
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Benchmarks the lookup of a [Ruler] to its [WindowInsetsRulers] inside
 * [WindowInsetsRulersProvider], as used by `isRulerProvided` and `provideInset` on every ruler read
 * in a layout pass.
 */
@LargeTest
@RunWith(JUnit4::class)
class WindowInsetsRulersProviderBenchmark {
    @get:Rule val benchmarkRule = BenchmarkRule()

    private lateinit var watcher: WindowInsetsWatcher
    private lateinit var provider: WindowInsetsRulersProvider

    /** Every ruler of every single-type WindowInsetsRulers, current + maximum + source + target. */
    private lateinit var allRulers: Array<Ruler>

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().context
        watcher = WindowInsetsWatcher(View(context))
        watcher.currentInsets = WindowInsetsCompat.Builder().build()
        provider = WindowInsetsRulersProvider(watcher)

        val singleTypes =
            listOf(
                WindowInsetsRulers.StatusBars,
                WindowInsetsRulers.NavigationBars,
                WindowInsetsRulers.CaptionBar,
                WindowInsetsRulers.Ime,
                WindowInsetsRulers.SystemGestures,
                WindowInsetsRulers.MandatorySystemGestures,
                WindowInsetsRulers.TappableElement,
                WindowInsetsRulers.DisplayCutout,
                WindowInsetsRulers.Waterfall,
            )
        val rulers = mutableListOf<Ruler>()
        singleTypes.forEach { rulers += it.current.rulers() }
        singleTypes.forEach { rulers += it.maximum.rulers() }
        allRulers = rulers.toTypedArray()
    }

    private fun androidx.compose.ui.layout.RectRulers.rulers(): List<Ruler> =
        listOf(left, top, right, bottom)

    @Test
    fun isRulerProvided_statusBarsCurrentLeft() {
        val ruler = WindowInsetsRulers.StatusBars.current.left
        benchmarkRule.measureRepeated { provider.isRulerProvided(ruler) }
    }

    @Test
    fun isRulerProvided_displayCutoutMaximumBottom() {
        val ruler = WindowInsetsRulers.DisplayCutout.maximum.bottom
        benchmarkRule.measureRepeated { provider.isRulerProvided(ruler) }
    }

    @Test
    fun isRulerProvided_waterfallCurrentLeft() {
        val ruler = WindowInsetsRulers.Waterfall.current.left
        benchmarkRule.measureRepeated { provider.isRulerProvided(ruler) }
    }

    @Test
    fun isRulerProvided_unknownRuler() {
        val ruler = VerticalRuler()
        benchmarkRule.measureRepeated { provider.isRulerProvided(ruler) }
    }

    /** All 72 current+maximum rulers of all single-type insets. */
    @Test
    fun isRulerProvided_allRulers() {
        val rulers = allRulers
        benchmarkRule.measureRepeated {
            for (i in rulers.indices) {
                provider.isRulerProvided(rulers[i])
            }
        }
    }

    /** First lookup on a freshly-created provider. */
    @Test
    fun isRulerProvided_firstLookupOnNewProvider() {
        val ruler = WindowInsetsRulers.StatusBars.current.left
        benchmarkRule.measureRepeated {
            val p = runWithMeasurementDisabled { WindowInsetsRulersProvider(watcher) }
            p.isRulerProvided(ruler)
        }
    }

    /** One-time cost of building the Ruler lookup map (paid on the first lookup per process). */
    @Test
    fun createRulerMap() {
        benchmarkRule.measureRepeated {
            WindowInsetsRulersProvider.RulerMap.createKeys(IntArray(RulerMapCapacity))
        }
    }
}
