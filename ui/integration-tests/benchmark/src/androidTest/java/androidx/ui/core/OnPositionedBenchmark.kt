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

package androidx.ui.core

import androidx.compose.Composable
import androidx.compose.MutableState
import androidx.compose.state
import androidx.test.filters.LargeTest
import androidx.ui.benchmark.ComposeBenchmarkRule
import androidx.ui.benchmark.toggleStateBenchmarkLayout
import androidx.ui.foundation.Box
import androidx.ui.foundation.ContentGravity
import androidx.ui.integration.test.ToggleableTestCase
import androidx.ui.layout.Stack
import androidx.ui.layout.preferredSize
import androidx.ui.test.ComposeTestCase
import androidx.ui.unit.Dp
import androidx.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class OnPositionedBenchmark {

    @get:Rule
    val benchmarkRule = ComposeBenchmarkRule()

    @Test
    fun deepHierarchyOnPositioned_layout() {
        benchmarkRule.toggleStateBenchmarkLayout({
            DeepHierarchyOnPositionedTestCase()
        })
    }
}

private class DeepHierarchyOnPositionedTestCase :
    ComposeTestCase, ToggleableTestCase {

    private lateinit var state: MutableState<Dp>

    @Composable
    override fun emitContent() {
        val size = state { 200.dp }
        this.state = size
        Stack {
            Box(Modifier.preferredSize(size.value), gravity = ContentGravity.Center) {
                StaticChildren(100)
            }
        }
    }

    @Composable
    private fun StaticChildren(count: Int) {
        if (count > 0) {
            val modifier = if (count == 1) {
                Modifier.onPositioned { it.size }
            } else {
                Modifier
            }
            Box(Modifier.preferredSize(100.dp) + modifier, gravity = ContentGravity.Center) {
                StaticChildren(count - 1)
            }
        }
    }

    override fun toggleState() {
        state.value = if (state.value == 200.dp) 150.dp else 200.dp
    }
}
