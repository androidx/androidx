/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.xr.glimmer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.xr.glimmer.samples.SurfaceSample
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class SurfaceScreenshotTest {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Before
    fun setUp() {
        rule.mainClock.autoAdvance = false
    }

    @After
    fun tearDown() {
        rule.mainClock.autoAdvance = true
    }

    @Test
    fun surface() {
        rule.setGlimmerThemeContent { SurfaceSample() }
        rule.assertRootAgainstGolden("surface", screenshotRule)
    }

    @Test
    fun surface_unfocused_default() {
        rule.setGlimmerThemeContent {
            Box(Modifier.surface().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("This is a surface")
            }
        }
        rule.assertRootAgainstGolden("surface_unfocused_default", screenshotRule)
    }

    @Test
    fun surface_focused_default() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysFocusedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // Advance past focus enter animation (800ms) before ambient motion initial delay (1800ms)
        rule.mainClock.advanceTimeBy(1000)
        rule.assertRootAgainstGolden("surface_focused_default", screenshotRule)
    }

    @Test
    fun surface_focused_default_ambientAnimation_25() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysFocusedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 25% of 2000ms ambient animation (500ms) = 2300ms
        rule.mainClock.advanceTimeBy(2300)
        rule.assertRootAgainstGolden("surface_focused_default_ambientAnimation_25", screenshotRule)
    }

    @Test
    fun surface_focused_default_ambientAnimation_50() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysFocusedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 50% of 2000ms ambient animation (1000ms) = 2800ms
        rule.mainClock.advanceTimeBy(2800)
        rule.assertRootAgainstGolden("surface_focused_default_ambientAnimation_50", screenshotRule)
    }

    @Test
    fun surface_focused_default_ambientAnimation_75() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysFocusedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 75% of 2000ms ambient animation (1500ms) = 3300ms
        rule.mainClock.advanceTimeBy(3300)
        rule.assertRootAgainstGolden("surface_focused_default_ambientAnimation_75", screenshotRule)
    }

    @Test
    fun surface_focused_default_ambientAnimationCompleted() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysFocusedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 2000ms ambient motion animation
        rule.mainClock.advanceTimeBy(3800)
        rule.assertRootAgainstGolden(
            "surface_focused_default_ambientAnimationCompleted",
            screenshotRule,
        )
    }

    @Test
    fun surface_focused_customFocusedColor() {
        val customFocusedColor = Color(0xFF245740)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(
                        focusedColor = customFocusedColor,
                        interactionSource = AlwaysFocusedInteractionSource,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // Advance past focus enter animation (800ms) before ambient motion initial delay (1800ms)
        rule.mainClock.advanceTimeBy(1000)
        rule.assertRootAgainstGolden("surface_focused_customFocusedColor", screenshotRule)
    }

    @Test
    fun surface_focused_customFocusedColor_ambientAnimation_25() {
        val customFocusedColor = Color(0xFF245740)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(
                        focusedColor = customFocusedColor,
                        interactionSource = AlwaysFocusedInteractionSource,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 25% of 2000ms ambient animation (500ms) = 2300ms
        rule.mainClock.advanceTimeBy(2300)
        rule.assertRootAgainstGolden(
            "surface_focused_customFocusedColor_ambientAnimation_progress25",
            screenshotRule,
        )
    }

    @Test
    fun surface_focused_customFocusedColor_ambientAnimation_50() {
        val customFocusedColor = Color(0xFF245740)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(
                        focusedColor = customFocusedColor,
                        interactionSource = AlwaysFocusedInteractionSource,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 50% of 2000ms ambient animation (1000ms) = 2800ms
        rule.mainClock.advanceTimeBy(2800)
        rule.assertRootAgainstGolden(
            "surface_focused_customFocusedColor_ambientAnimation_progress50",
            screenshotRule,
        )
    }

    @Test
    fun surface_focused_customFocusedColor_ambientAnimation_75() {
        val customFocusedColor = Color(0xFF245740)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(
                        focusedColor = customFocusedColor,
                        interactionSource = AlwaysFocusedInteractionSource,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 75% of 2000ms ambient animation (1500ms) = 3300ms
        rule.mainClock.advanceTimeBy(3300)
        rule.assertRootAgainstGolden(
            "surface_focused_customFocusedColor_ambientAnimation_progress75",
            screenshotRule,
        )
    }

    @Test
    fun surface_focused_customFocusedColor_ambientAnimationCompleted() {
        val customFocusedColor = Color(0xFF245740)
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(
                        focusedColor = customFocusedColor,
                        interactionSource = AlwaysFocusedInteractionSource,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // 1800ms initial delay + 100% of 2000ms ambient animation (2000ms) = 3800ms
        rule.mainClock.advanceTimeBy(3800)
        rule.assertRootAgainstGolden(
            "surface_focused_customFocusedColor_ambientAnimation_progress100",
            screenshotRule,
        )
    }

    @Test
    fun surface_rectShape() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(shape = RectangleShape)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        rule.assertRootAgainstGolden("surface_rectShape", screenshotRule)
    }

    @Test
    fun surface_focused_rectShape() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(
                        shape = RectangleShape,
                        interactionSource = AlwaysFocusedInteractionSource,
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // Advance past focus enter animation (800ms) before ambient motion initial delay (1800ms)
        rule.mainClock.advanceTimeBy(1000)
        rule.assertRootAgainstGolden("surface_focused_rectShape", screenshotRule)
    }

    /**
     * Practically a surface cannot be pressed without also being focused, but we test them in
     * isolation as well to make it easier to identify changes. See [surface_focused_and_pressed]
     * for the combined state.
     */
    @Test
    fun surface_pressed() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysPressedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // Skip until after the animation has finished
        rule.mainClock.advanceTimeBy(5000)
        rule.assertRootAgainstGolden("surface_pressed", screenshotRule)
    }

    /**
     * Practically a surface cannot be pressed without also being focused, but we test them in
     * isolation as well to make it easier to identify changes. See [surface_focused_and_pressed]
     * for the combined state.
     */
    @Test
    fun surface_pressed_animation() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysPressedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // Advance to partway through the animation
        rule.mainClock.advanceTimeBy(200)
        rule.assertRootAgainstGolden("surface_pressed_animation", screenshotRule)
    }

    @Test
    fun surface_focused_and_pressed() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.surface(interactionSource = AlwaysFocusedAndPressedInteractionSource)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                Text("This is a surface")
            }
        }
        // Advance past focus enter animation (800ms) before ambient motion initial delay (1800ms)
        rule.mainClock.advanceTimeBy(1000)
        rule.assertRootAgainstGolden("surface_focused_and_pressed", screenshotRule)
    }

    @Test
    fun surface_disabled() {
        rule.setGlimmerThemeContent {
            Box(Modifier.surface(enabled = false).padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("This is a surface")
            }
        }
        rule.assertRootAgainstGolden("surface_disabled", screenshotRule)
    }

    @Test
    fun surface_custom() {
        rule.setGlimmerThemeContent {
            Box(
                Modifier.size(100.dp)
                    .surface(shape = DoubleTriangleShape, color = Color.Red)
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("!")
            }
        }
        rule.assertRootAgainstGolden("surface_custom", screenshotRule)
    }

    private object DoubleTriangleShape : Shape {
        override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) =
            Outline.Generic(
                Path().apply {
                    lineTo(size.width / 2f, size.height / 2f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height)
                    lineTo(size.width / 2f, size.height / 2f)
                    lineTo(0f, size.height)
                    close()
                }
            )
    }
}
