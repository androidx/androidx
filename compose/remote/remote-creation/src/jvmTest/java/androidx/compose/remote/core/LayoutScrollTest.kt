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

import androidx.compose.remote.core.layout.AdvanceTime
import androidx.compose.remote.core.layout.ApplyClick
import androidx.compose.remote.core.layout.ApplyTouchDown
import androidx.compose.remote.core.layout.ApplyTouchDrag
import androidx.compose.remote.core.layout.ApplyTouchUp
import androidx.compose.remote.core.layout.CaptureComponentTree
import androidx.compose.remote.core.layout.Color
import androidx.compose.remote.core.layout.TestOperation
import androidx.compose.remote.core.layout.ValidateNamedFloat
import androidx.compose.remote.core.layout.ValidateNamedString
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.actions.ValueStringChange
import org.junit.Test

class LayoutScrollTest : BaseLayoutTest() {

    init {
        GENERATE_GOLD_FILES = false
    }

    @Test
    fun testVerticalScrollClick() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    val text = addNamedString("click", "empty")
                    column(Modifier.fillMaxSize().verticalScroll()) {
                        for (i in 0 until 10) {
                            box(
                                Modifier.size(200)
                                    .background(if (i % 2 == 0) Color.RED else Color.BLUE)
                                    .onClick(ValueStringChange(text, "Box $i"))
                            ) {
                                text("Box $i")
                            }
                        }
                    }
                },
                CaptureComponentTree(),
                ValidateNamedString("click", "empty"),
                // Initial click on the first box (0,0 to 200,200)
                ApplyClick(100f, 100f),
                ValidateNamedString("click", "Box 0"),
                // Scroll down: drag from 500,500 to 500,100 (scrolling by 400px)
                // This should move boxes 0 and 1 out of view, box 2 now at 0, and box 4 at 400
                ApplyTouchDown(500f, 500f),
                ApplyTouchDrag(500f, 300f, 10),
                ApplyTouchDrag(500f, 100f, 20),
                ApplyTouchUp(500f, 100f, 30),
                CaptureComponentTree(),
                // Now click where Box 4 is (originally at 800, now at 400)
                // If we scrolled by 400, Box 2 is at 0, Box 3 at 200, Box 4 at 400.
                ApplyClick(100f, 450f),
                ValidateNamedString("click", "Box 4"),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "VerticalScrollClick",
            ops,
        )
    }

    @Test
    fun testHorizontalScrollClick() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    val name = addNamedString("click", "empty")
                    row(Modifier.fillMaxSize().horizontalScroll()) {
                        for (i in 0 until 10) {
                            box(
                                Modifier.size(200)
                                    .background(if (i % 2 == 0) Color.RED else Color.BLUE)
                                    .onClick(ValueStringChange(name, "Box $i"))
                            ) {
                                text("Box $i")
                            }
                        }
                    }
                },
                CaptureComponentTree(),
                ValidateNamedString("click", "empty"),
                // Initial click on the first box (0,0 to 200,200)
                ApplyClick(100f, 100f),
                ValidateNamedString("click", "Box 0"),
                // Scroll right: drag from 500,300 to 100,300 (scrolling by 400px)
                // This should move boxes 0 and 1 out of view, box 2 now at 0, and box 4 at 400
                ApplyTouchDown(500f, 300f),
                ApplyTouchDrag(300f, 300f, 10),
                ApplyTouchDrag(100f, 300f, 20),
                ApplyTouchUp(100f, 300f, 30),
                CaptureComponentTree(),
                // Now click where Box 4 is (originally at 800, now at 400)
                // If we scrolled by 400, Box 2 is at 0, Box 3 at 200, Box 4 at 400.
                ApplyClick(450f, 100f),
                ValidateNamedString("click", "Box 4"),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "HorizontalScrollClick",
            ops,
        )
    }

    @Test
    fun testNestedScrolling() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    val hClick = addNamedFloat("h_click", -1f)
                    val vClick = addNamedFloat("v_click", -1f)
                    column(Modifier.fillMaxSize().verticalScroll()) {
                        // A long vertical scroll
                        box(Modifier.size(600).background(Color.BLUE))

                        // A horizontal scroll inside the vertical scroll
                        row(Modifier.width(600).height(200).horizontalScroll()) {
                            for (i in 0 until 10) {
                                box(
                                    Modifier.size(200)
                                        .background(if (i % 2 == 0) Color.GREEN else Color.CYAN)
                                        .onClick(
                                            ValueFloatChange(Utils.idFromNan(hClick), i.toFloat())
                                        )
                                ) {
                                    text("H-Box $i")
                                }
                            }
                        }

                        box(
                            Modifier.size(600)
                                .background(Color.RED)
                                .onClick(ValueFloatChange(Utils.idFromNan(vClick), 1f))
                        )
                    }
                },
                CaptureComponentTree(),
                // 1. Test vertical scroll: drag up to reveal red box
                ApplyTouchDown(300f, 500f),
                ApplyTouchDrag(300f, 100f, 50),
                ApplyTouchUp(300f, 100f, 100),
                // Click the red box (revealed at bottom)
                ApplyClick(300f, 500f),
                ValidateNamedFloat("v_click", 1f),

                // 2. Test horizontal scroll (row is at y=600 - scroll(400) = 200)
                // Drag row left: drag from x=500 to x=100
                ApplyTouchDown(500f, 300f),
                ApplyTouchDrag(300f, 300f, 50),
                ApplyTouchDrag(100f, 300f, 100),
                ApplyTouchUp(100f, 300f, 150),
                // Click a horizontal box
                ApplyClick(100f, 300f),
                // We dragged by 400px horizontally.
                // H-Box 0 was at 0, H-Box 1 at 200, H-Box 2 at 400.
                // After 400px scroll, H-Box 2 is at 0. Click at 100 should hit H-Box 2.
                ValidateNamedFloat("h_click", 2f),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "NestedScrolling",
            ops,
        )
    }

    @Test
    fun testFlingScroll() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    val scrollPosId = addNamedFloat("scroll_pos", 0f)
                    column(Modifier.fillMaxSize().verticalScroll(scrollPosId)) {
                        box(Modifier.width(600).height(2000).background(Color.MAGENTA))
                    }
                },
                CaptureComponentTree(),
                // Drag up fast and release with velocity
                ApplyTouchDown(300f, 500f),
                ApplyTouchDrag(300f, 400f, 10), // Move slightly
                // Release with -1000px/s velocity (scrolling down)
                ApplyTouchUp(300f, 400f, 0f, -1000f, 20),
                // Immediately after touchUp, it should have moved slightly (original 100 + initial
                // fling)
                AdvanceTime(100),
                CaptureComponentTree(),
                // It should have continued moving
                AdvanceTime(1000),
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FlingScroll",
            ops,
        )
    }

    @Test
    fun testFillParentViewport() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(Modifier.fillMaxSize().verticalScroll()) {
                        // This box should be exactly the size of the viewport (600x600)
                        box(Modifier.fillParentMaxHeight().fillMaxWidth().background(Color.RED)) {
                            text("Viewport Filler")
                        }
                        // This box makes the content scrollable
                        box(Modifier.width(600).height(1000).background(Color.BLUE))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FillParentViewport",
            ops,
        )
    }

    @Test
    fun testFillParentMaxWidthHorizontal() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    row(Modifier.fillMaxSize().horizontalScroll()) {
                        // This box should be exactly the size of the viewport (600x600)
                        box(Modifier.fillParentMaxWidth().fillMaxHeight().background(Color.RED)) {
                            text("Viewport Filler")
                        }
                        // This box makes the content scrollable
                        box(Modifier.width(1000).height(600).background(Color.BLUE))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FillParentMaxWidthHorizontal",
            ops,
        )
    }

    @Test
    fun testFillMaxFraction() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(Modifier.fillMaxSize()) {
                        // This box should be half the size of the document width (300)
                        box(Modifier.fillMaxWidth(0.5f).height(100).background(Color.RED)) {
                            text("Half Width")
                        }
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FillMaxFraction",
            ops,
        )
    }

    @Test
    fun testFillParentMaxFraction() {
        val ops =
            arrayListOf<TestOperation?>(
                TestLayout {
                    column(Modifier.fillMaxSize().verticalScroll()) {
                        // This box should be half the size of the viewport height (300)
                        box(
                            Modifier.fillParentMaxHeight(0.5f).fillMaxWidth().background(Color.RED)
                        ) {
                            text("Half Viewport")
                        }
                        // This box makes the content scrollable
                        box(Modifier.width(600).height(1000).background(Color.BLUE))
                    }
                },
                CaptureComponentTree(),
            )
        checkLayout(
            600,
            600,
            7,
            RcProfiles.PROFILE_ANDROIDX or RcProfiles.PROFILE_EXPERIMENTAL,
            "FillParentMaxFraction",
            ops,
        )
    }
}
