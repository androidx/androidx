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

package androidx.compose.remote.core

import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.creation.ComponentHeight
import androidx.compose.remote.creation.ComponentWidth
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.windowWidth
import org.junit.Test

class DirectEvaluationTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    /**
     * Test that a child can use its parent's width during the first layout pass. Before the direct
     * evaluation change, this would have resulted in size 0 in the first pass.
     */
    @Test
    fun testChildUsingParentSize() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    box(Modifier.componentId(1).size(400)) {
                        val w = ComponentWidth()
                        box(
                            Modifier.componentId(2).size((w * 0.5f).toFloat()).background(Color.RED)
                        )
                    }
                },
                validateSize(2, 200f, 200f),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "DirectEval",
            ops,
        )
    }

    /** Test that a child can use its parent's dimensions in padding. */
    @Test
    fun testChildUsingParentPadding() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    box(Modifier.componentId(1).size(400)) {
                        val w = ComponentWidth()
                        // Apply padding to the parent's content via a nested box.
                        box(Modifier.padding((w * 0.1f).toFloat()).fillMaxSize()) {
                            box(Modifier.componentId(2).fillMaxSize().background(Color.BLUE))
                        }
                    }
                },
                // The child (Component 2) fills the content area of its parent box.
                // In BoxLayout, children coordinates are relative to the content area (after
                // padding).
                // So expected size is 400 - 2*40 = 320, and position is (0,0).
                validateBounds(2, 0f, 0f, 320f, 320f),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "DirectEval",
            ops,
        )
    }

    /** Test that a variable defined before the root layout is correctly updated. */
    @Test
    fun testTopLevelVariable() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    val size = (windowWidth() * 0.5f).toFloat()
                    box(Modifier.componentId(1).size(size).background(Color.GREEN))
                },
                validateSize(1, 500f, 500f),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "DirectEval",
            ops,
        )
    }

    /** Test more complex expression using min() and multiple components. */
    @Test
    fun testComplexExpression() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.componentId(1).size(600, 400)) {
                        val w = ComponentWidth()
                        val h = ComponentHeight()
                        val minDim = (min(w, h)).toFloat()
                        box(Modifier.componentId(2).size(minDim).background(Color.YELLOW))
                    }
                },
                validateSize(2, 400f, 400f),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "DirectEval",
            ops,
        )
    }
}
