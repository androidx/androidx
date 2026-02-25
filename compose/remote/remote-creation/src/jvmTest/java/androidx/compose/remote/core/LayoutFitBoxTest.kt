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
import androidx.compose.remote.core.operations.layout.managers.FitBoxLayout
import org.junit.Test

class LayoutFitBoxTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testFitBoxSelection() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        // FitBox that will resize based on document resize
                        fitBox(
                            Modifier.fillMaxWidth().height(200),
                            horizontal = FitBoxLayout.CENTER,
                            vertical = FitBoxLayout.CENTER,
                        ) {
                            // First child: large (400px wide)
                            box(Modifier.size(400, 100).background(Color.RED)) { text("Large") }
                            // Second child: medium (200px wide)
                            box(Modifier.size(200, 100).background(Color.GREEN)) { text("Medium") }
                            // Third child: small (100px wide)
                            box(Modifier.size(100, 100).background(Color.BLUE)) { text("Small") }
                        }
                    }
                },
                // 1. Initial width 600: Large should fit
                CaptureComponentTree(),

                // 2. Resize to 300: Large doesn't fit, Medium should fit
                ResizeDocument(300, 600),
                CaptureComponentTree(),

                // 3. Resize to 150: Only Small should fit
                ResizeDocument(150, 600),
                CaptureComponentTree(),

                // 4. Resize to 50: Nothing fits, FitBox should go GONE
                ResizeDocument(50, 600),
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FitBoxSelection",
            ops,
        )
    }

    @Test
    fun testFitBoxWithConstraints() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        fitBox(
                            Modifier.fillMaxWidth().height(200),
                            horizontal = FitBoxLayout.CENTER,
                            vertical = FitBoxLayout.CENTER,
                        ) {

                            // Child 1: Prefers to be 500px, but has min 400px.
                            // With unbounded measure, it will be 500px.
                            box(Modifier.widthIn(400f, 500f).height(100).background(Color.RED)) {
                                text("Constrained Large")
                            }

                            // Child 2: Smaller
                            box(Modifier.size(200, 100).background(Color.BLUE)) { text("Small") }
                        }
                    }
                },
                // 1. Width 600: Child 1 fits (it will be 500px)
                CaptureComponentTree(),

                // 2. Width 450: Child 1 fits! Its min is 400, so it can shrink to 450.
                ResizeDocument(450, 600),
                CaptureComponentTree(),

                // 3. Width 300: Only Small fits (Child 1 min 400 > 300)
                ResizeDocument(300, 600),
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FitBoxWithConstraints",
            ops,
        )
    }
}
