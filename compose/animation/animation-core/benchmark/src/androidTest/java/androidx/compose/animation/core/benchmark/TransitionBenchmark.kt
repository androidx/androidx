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

package androidx.compose.animation.core.benchmark

import androidx.compose.animation.core.updateTransition
import androidx.compose.runtime.Composable
import androidx.compose.testutils.LayeredComposeTestCase
import androidx.compose.testutils.benchmark.ComposeBenchmarkRule
import androidx.compose.testutils.benchmark.benchmarkFirstCompose
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class TransitionBenchmark {

    @get:Rule val rule = ComposeBenchmarkRule()

    @Test
    fun createTransitionThroughUpdateTransition() {
        rule.benchmarkFirstCompose(::TransitionBenchmarkCaseFactory)
    }
}

private class TransitionBenchmarkCaseFactory : LayeredComposeTestCase() {
    @Composable
    override fun MeasuredContent() {
        updateTransition(targetState = Unit, label = null)
    }
}
