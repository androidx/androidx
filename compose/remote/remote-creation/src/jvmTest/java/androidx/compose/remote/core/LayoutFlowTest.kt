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
import androidx.compose.remote.core.layout.ResizeDocument
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import org.junit.Test

class LayoutFlowTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testFlowLayoutBasic() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(
                        Modifier.fillMaxSize().background(Color.YELLOW).padding(8),
                        horizontal = RowLayout.START,
                        vertical = RowLayout.TOP,
                    ) {
                        box(Modifier.size(200).background(Color.RED))
                        box(Modifier.size(200).background(Color.GREEN))
                        box(Modifier.size(200).background(Color.BLUE))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }

    @Test
    fun testFlowLayoutOverflow() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(
                        Modifier.width(300).background(Color.YELLOW).padding(8),
                        horizontal = RowLayout.START,
                        vertical = RowLayout.TOP,
                    ) {
                        box(Modifier.size(100).background(Color.RED))
                        box(Modifier.size(100).background(Color.GREEN))
                        box(Modifier.size(100).background(Color.BLUE))
                        box(Modifier.size(100).background(Color.CYAN))
                    }
                },
                CaptureComponentTree(),
                ResizeDocument(200, 1000),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }

    @Test
    fun testFlowLayoutSpacing() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(
                        Modifier.fillMaxWidth().spacedBy(10f).background(Color.LTGRAY).padding(10),
                        horizontal = RowLayout.START,
                    ) {
                        box(Modifier.size(50).background(Color.RED))
                        box(Modifier.size(50).background(Color.GREEN))
                        box(Modifier.size(50).background(Color.BLUE))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }

    @Test
    fun testFlowLayoutMixedSizing() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(
                        Modifier.width(400).background(Color.YELLOW).padding(8),
                        horizontal = RowLayout.CENTER,
                    ) {
                        box(Modifier.width(150).height(50).background(Color.RED))
                        box(Modifier.width(200).height(100).background(Color.GREEN))
                        box(Modifier.width(100).height(50).background(Color.BLUE))
                        box(Modifier.width(250).height(80).background(Color.CYAN))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }

    @Test
    fun testFlowLayoutWeightsSimple() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(Modifier.width(600).background(Color.YELLOW).padding(8)) {
                        box(Modifier.horizontalWeight(1f).height(50).background(Color.RED))
                        box(Modifier.horizontalWeight(1f).height(50).background(Color.GREEN))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }

    @Test
    fun testFlowLayoutWeightsMixed() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(Modifier.width(600).background(Color.YELLOW).padding(8)) {
                        box(Modifier.width(100).height(50).background(Color.RED))
                        box(Modifier.horizontalWeight(1f).height(50).background(Color.GREEN))
                        box(Modifier.width(100).height(50).background(Color.BLUE))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }

    @Test
    fun testFlowLayoutWeightsMultipleRows() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    flow(Modifier.width(300).background(Color.YELLOW).padding(8)) {
                        // First row: 100 + weight(1) + 100 -> weight takes remaining space in row
                        box(Modifier.width(100).height(50).background(Color.RED))
                        box(Modifier.horizontalWeight(1f).height(50).background(Color.GREEN))
                        box(Modifier.width(100).height(50).background(Color.BLUE))
                        // Next item might wrap if it doesn't fit
                        box(Modifier.width(250).height(50).background(Color.CYAN))
                        box(Modifier.horizontalWeight(1f).height(50).background(Color.MAGENTA))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlowLayout",
            ops,
        )
    }
}
