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

package androidx.compose.animation.benchmark

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.ToggleableTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.compose.testutils.benchmark.benchmarkToFirstPixel
import androidx.compose.testutils.benchmark.toggleStateBenchmarkCompose
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CrossfadeBenchmark {
    @get:Rule val rule = ComposeBenchmarkRule()

    @Test fun compose() = rule.benchmarkFirstCompose(::CrossfadeTestCase)

    @Test fun firstPixel() = rule.benchmarkToFirstPixel(::CrossfadeTestCase)

    @Test
    fun toggleState_compose() =
        rule.toggleStateBenchmarkCompose(::CrossfadeTestCase, assertOneRecomposition = false)
}

private class CrossfadeTestCase : LayeredComposeTestCase(), ToggleableTestCase {
    var state by mutableStateOf(true)

    @Composable
    override fun MeasuredContent() {
        val transition = updateTransition(state)
        transition.Crossfade { targetState ->
            Box(Modifier.fillMaxSize().background(if (targetState) Color.Red else Color.Green))
        }
    }

    override fun toggleState() {
        state = !state
    }
}
