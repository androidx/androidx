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
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import org.junit.Test

class LayoutCollapsibleTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testCollapsibleRowPriority() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        collapsibleRow(
                            Modifier.fillMaxWidth().height(200).background(Color.YELLOW),
                            horizontal = ColumnLayout.START,
                            vertical = ColumnLayout.TOP,
                        ) {
                            // Child 1: priority 10 (100px wide)
                            box(
                                Modifier.size(100, 100)
                                    .background(Color.RED)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                            ) {
                                text("P10")
                            }
                            // Child 2: priority 100 (200px wide)
                            box(
                                Modifier.size(200, 100)
                                    .background(Color.GREEN)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 100f)
                            ) {
                                text("P100")
                            }
                            // Child 3: priority 50 (150px wide)
                            box(
                                Modifier.size(150, 100)
                                    .background(Color.BLUE)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 50f)
                            ) {
                                text("P50")
                            }
                        }
                    }
                },
                // 1. Initial width 1000: All should fit
                CaptureComponentTree(),

                // 2. Resize to 300:
                // Priority order: P100 (200), P50 (150), P10 (100).
                // At 300: P100 fits (200). Next is P50 (150), 200+150=350 > 300. P50 gone.
                // Since P50 doesn't fit, P10 MUST also be gone (even if 200+100=300 fits).
                // So only P100 should be visible.
                ResizeDocument(300, 600),
                CaptureComponentTree(),

                // 3. Resize to 250: P100 (200) fits. P50 (150) no. P10 (100) no.
                // Only P100 should be visible.
                ResizeDocument(250, 600),
                CaptureComponentTree(),

                // 4. Resize to 150: P100 (200) doesn't fit.
                // Since the most important element (P100) doesn't fit, everything is gone.
                // The layout itself becomes GONE.
                ResizeDocument(150, 600),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "CollapsiblePriority",
            ops,
        )
    }

    @Test
    fun testCollapsibleColumnPriority() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        collapsibleColumn(
                            Modifier.fillMaxHeight().width(200).background(Color.YELLOW),
                            horizontal = ColumnLayout.START,
                            vertical = ColumnLayout.TOP,
                        ) {
                            // Child 1: priority 10 (100px high)
                            box(
                                Modifier.size(100, 100)
                                    .background(Color.RED)
                                    .collapsiblePriority(CollapsiblePriority.VERTICAL, 10f)
                            ) {
                                text("P10")
                            }
                            // Child 2: priority 100 (200px high)
                            box(
                                Modifier.size(100, 200)
                                    .background(Color.GREEN)
                                    .collapsiblePriority(CollapsiblePriority.VERTICAL, 100f)
                            ) {
                                text("P100")
                            }
                            // Child 3: priority 50 (150px high)
                            box(
                                Modifier.size(100, 150)
                                    .background(Color.BLUE)
                                    .collapsiblePriority(CollapsiblePriority.VERTICAL, 50f)
                            ) {
                                text("P50")
                            }
                        }
                    }
                },
                // 1. Initial height 1000: All should fit
                CaptureComponentTree(),

                // 2. Resize height to 300:
                // Priority order: P100 (200), P50 (150), P10 (100)
                // P100 fits (200). P50 (150) doesn't fit (200+150=350).
                // Since P50 doesn't fit, P10 is also gone.
                // Only P100 should be visible.
                ResizeDocument(1000, 300),
                CaptureComponentTree(),

                // 3. Resize height to 250: P100 (200) fits. P50/P10 gone.
                ResizeDocument(1000, 250),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            1000,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "CollapsiblePriorityColumn",
            ops,
        )
    }

    @Test
    fun testCollapsibleMixedPriority() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        collapsibleRow(
                            Modifier.fillMaxWidth().height(200).background(Color.YELLOW),
                            horizontal = ColumnLayout.START,
                            vertical = ColumnLayout.TOP,
                        ) {
                            // Child 1: No priority (defaults to MAX_VALUE, should stay longest)
                            box(Modifier.size(100, 100).background(Color.LTGRAY)) { text("NoP") }
                            // Child 2: priority 10 (200px wide, should disappear first)
                            box(
                                Modifier.size(200, 100)
                                    .background(Color.GREEN)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                            ) {
                                text("P10")
                            }
                        }
                    }
                },
                // 1. Initial width 1000: Both should fit
                CaptureComponentTree(),

                // 2. Resize to 250:
                // Sorted: NoP (MAX_VALUE, 100), P10 (10, 200)
                // NoP fits (100). Remaining 150.
                // P10 (200) doesn't fit. P10 disappears.
                // Only NoP should stay visible.
                ResizeDocument(250, 600),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "CollapsibleMixedPriority",
            ops,
        )
    }

    @Test
    fun testCollapsiblePriorityDisappearOrder() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        collapsibleRow(
                            Modifier.fillMaxWidth().height(200).background(Color.YELLOW),
                            horizontal = ColumnLayout.START,
                            vertical = ColumnLayout.TOP,
                        ) {
                            // A: priority 1000 (100px wide)
                            box(
                                Modifier.size(100, 100)
                                    .background(Color.RED)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 1000f)
                            ) {
                                text("A")
                            }
                            // B: priority 100 (200px wide)
                            box(
                                Modifier.size(200, 100)
                                    .background(Color.GREEN)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 100f)
                            ) {
                                text("B")
                            }
                            // C: priority 10 (50px wide)
                            box(
                                Modifier.size(50, 100)
                                    .background(Color.BLUE)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                            ) {
                                text("C")
                            }
                        }
                    }
                },
                // 1. Width 1000: All fit (Total 350)
                CaptureComponentTree(),

                // 2. Width 250:
                // Sorted: A(1000, 100), B(100, 200), C(10, 50)
                // A fits (100). Remaining 150.
                // B(200) doesn't fit in 150.
                // Since B doesn't fit, C MUST disappear too, even if 100+50=150 fits.
                ResizeDocument(250, 600),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "CollapsiblePriorityOrder",
            ops,
        )
    }

    @Test
    fun testCollapsibleWeight() {
        val ops =
            arrayListOf<TestOperation>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        collapsibleRow(
                            Modifier.fillMaxWidth().height(200).background(Color.YELLOW),
                            horizontal = ColumnLayout.START,
                            vertical = ColumnLayout.TOP,
                        ) {
                            // A: priority 100 (100px wide)
                            box(
                                Modifier.size(100, 100)
                                    .background(Color.RED)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 100f)
                            ) {
                                text("A")
                            }
                            // B: weight 1 (should take remaining space)
                            box(
                                Modifier.height(100)
                                    .horizontalWeight(1f)
                                    .background(Color.GREEN)
                                    .collapsiblePriority(CollapsiblePriority.HORIZONTAL, 10f)
                            ) {
                                text("B")
                            }
                        }
                    }
                },
                // 1. Width 1000: Both fit. A is 100, B is 900.
                CaptureComponentTree(),

                // 2. Width 150:
                // Priority order: A(100, 100), B(10, weighted)
                // A fits (100). Remaining 50.
                // B is measured with 150 (maxWidth). 100 + 150 > 150.
                // B doesn't fit, so it disappears.
                // Only A should be visible.
                ResizeDocument(150, 600),
                CaptureComponentTree(),
            )
        checkLayout(
            1000,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "CollapsibleWeight",
            ops,
        )
    }
}
