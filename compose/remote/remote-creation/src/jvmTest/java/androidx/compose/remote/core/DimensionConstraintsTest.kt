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

import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.TestOperation
import org.junit.Test

class DimensionConstraintsTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testRequiredWidthIn() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize().background(Color.YELLOW)) {
                        // This box should be 400px wide because of requiredWidthIn,
                        // even if it would normally be 100px.
                        box(
                            Modifier.componentId(2)
                                .width(100)
                                .height(100)
                                .requiredWidthIn(400f, 500f)
                                .background(Color.RED)
                        )
                        // This box should be 500px wide because of requiredWidthIn,
                        // even if it would normally be 800px.
                        box(
                            Modifier.componentId(3)
                                .width(800)
                                .height(100)
                                .requiredWidthIn(400f, 500f)
                                .background(Color.GREEN)
                        )
                    }
                },
                validateSize(2, 400f, 100f),
                validateSize(3, 500f, 100f),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
        )
    }

    @Test
    fun testRequiredHeightIn() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    row(Modifier.fillMaxSize().background(Color.YELLOW)) {
                        // This box should be 400px high because of requiredHeightIn
                        box(
                            Modifier.componentId(2)
                                .width(100)
                                .height(100)
                                .requiredHeightIn(400f, 500f)
                                .background(Color.RED)
                        )
                        // This box should be 500px high because of requiredHeightIn
                        box(
                            Modifier.componentId(3)
                                .width(100)
                                .height(800)
                                .requiredHeightIn(400f, 500f)
                                .background(Color.GREEN)
                        )
                    }
                },
                validateSize(2, 100f, 400f),
                validateSize(3, 100f, 500f),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
        )
    }

    @Test
    fun testWidthInAndRequiredHeightIn() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    box(Modifier.fillMaxSize().background(Color.YELLOW)) {
                        box(
                            Modifier.componentId(2)
                                .widthIn(200f, 300f)
                                .requiredHeightIn(400f, 500f)
                                .background(Color.BLUE)
                        )
                    }
                },
                // widthIn with WRAP should default to min (200)
                // requiredHeightIn with WRAP should default to min (400)
                validateSize(2, 200f, 400f),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            8,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "Layout",
            ops,
        )
    }
}
