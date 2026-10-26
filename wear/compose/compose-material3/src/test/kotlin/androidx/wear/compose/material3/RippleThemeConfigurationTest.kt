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

package androidx.wear.compose.material3

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RippleThemeConfigurationTest {

    @Test
    fun rippleDefaults_opacityFocusThemeConfiguration() {
        assertEquals(
            RippleThemeConfiguration.Focus.Opacity,
            RippleDefaults.OpacityFocusThemeConfiguration.focus,
        )
    }

    @Test
    fun rippleDefaults_insetFocusRingThemeConfiguration() {
        assertEquals(
            RippleThemeConfiguration.Focus.InsetRing(
                outerStrokeInset = 0.dp,
                outerStrokeWidth = 2.dp,
                innerStrokeInset = 0.dp,
                innerStrokeWidth = 4.dp,
            ),
            RippleDefaults.InsetFocusRingThemeConfiguration.focus,
        )
    }

    @Test
    fun rippleDefaults_themeConfiguration_defaultsToOpacity() {
        assertEquals(
            RippleDefaults.OpacityFocusThemeConfiguration,
            RippleDefaults.ThemeConfiguration,
        )
    }

    @Test
    fun rippleThemeConfiguration_focus_insetRing_equalityAndHashCode() {
        val ring1 =
            RippleThemeConfiguration.Focus.InsetRing(
                outerStrokeInset = 0.dp,
                outerStrokeWidth = 2.dp,
                innerStrokeInset = 0.dp,
                innerStrokeWidth = 4.dp,
            )
        val ring2 =
            RippleThemeConfiguration.Focus.InsetRing(
                outerStrokeInset = 0.dp,
                outerStrokeWidth = 2.dp,
                innerStrokeInset = 0.dp,
                innerStrokeWidth = 4.dp,
            )
        val ring3 =
            RippleThemeConfiguration.Focus.InsetRing(
                outerStrokeInset = 0.dp,
                outerStrokeWidth = 3.dp,
                innerStrokeInset = 0.dp,
                innerStrokeWidth = 4.dp,
            )

        assertEquals(ring1, ring2)
        assertEquals(ring1.hashCode(), ring2.hashCode())
        assertEquals(RippleThemeConfiguration(ring1), RippleThemeConfiguration(ring2))
        assertEquals(
            RippleThemeConfiguration(ring1).hashCode(),
            RippleThemeConfiguration(ring2).hashCode(),
        )

        assertNotEquals(ring1, ring3)
        assertNotEquals(ring1.hashCode(), ring3.hashCode())
        assertNotEquals(RippleThemeConfiguration(ring1), RippleThemeConfiguration(ring3))
        assertNotEquals(
            RippleThemeConfiguration(ring1).hashCode(),
            RippleThemeConfiguration(ring3).hashCode(),
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rippleThemeConfiguration_focus_insetRing_negativeDimension_throws() {
        RippleThemeConfiguration.Focus.InsetRing(
            outerStrokeInset = (-1).dp,
            outerStrokeWidth = 2.dp,
            innerStrokeInset = 0.dp,
            innerStrokeWidth = 4.dp,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rippleThemeConfiguration_focus_insetRing_outerInsetGreaterThanInnerInset_throws() {
        RippleThemeConfiguration.Focus.InsetRing(
            outerStrokeInset = 2.dp,
            outerStrokeWidth = 2.dp,
            innerStrokeInset = 0.dp,
            innerStrokeWidth = 4.dp,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rippleThemeConfiguration_focus_insetRing_disjointStrokes_throws() {
        RippleThemeConfiguration.Focus.InsetRing(
            outerStrokeInset = 0.dp,
            outerStrokeWidth = 2.dp,
            innerStrokeInset = 3.dp,
            innerStrokeWidth = 4.dp,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun rippleThemeConfiguration_focus_insetRing_innerDoesNotExtendToOuterEnd_throws() {
        RippleThemeConfiguration.Focus.InsetRing(
            outerStrokeInset = 0.dp,
            outerStrokeWidth = 4.dp,
            innerStrokeInset = 0.dp,
            innerStrokeWidth = 2.dp,
        )
    }

    @Test
    fun rippleConfiguration_focus_insetRing_defaultsToUnspecified() {
        val config = RippleConfiguration.Focus.InsetRing()
        assertEquals(Color.Unspecified, config.outerStrokeColor)
        assertEquals(Color.Unspecified, config.innerStrokeColor)
    }

    @Test
    fun rippleConfiguration_focus_insetRing_equalityAndHashCode() {
        val config1 =
            RippleConfiguration.Focus.InsetRing(
                outerStrokeColor = Color.Red,
                innerStrokeColor = Color.Blue,
            )
        val config2 =
            RippleConfiguration.Focus.InsetRing(
                outerStrokeColor = Color.Red,
                innerStrokeColor = Color.Blue,
            )
        val config3 =
            RippleConfiguration.Focus.InsetRing(
                outerStrokeColor = Color.Green,
                innerStrokeColor = Color.Blue,
            )

        assertEquals(config1, config2)
        assertEquals(config1.hashCode(), config2.hashCode())

        assertNotEquals(config1, config3)
        assertNotEquals(config1.hashCode(), config3.hashCode())
    }

    @Test
    fun splitSectionShapes_percentageOuterCorners_resolveAgainstHeightWhenNarrowerThanTall() {
        val outerShape = RoundedCornerShape(percent = 50)
        val innerShape = RoundedCornerShape(2.dp)
        val density = Density(1f)
        // Simulate Slider button: 48.dp wide x 52.dp tall inside a 52.dp tall container.
        val buttonSize = Size(48f, 52f)

        val startOutline =
            outerShape
                .startSection(innerShape)
                .createOutline(buttonSize, LayoutDirection.Ltr, density) as Outline.Rounded
        val endOutline =
            outerShape
                .endSection(innerShape)
                .createOutline(buttonSize, LayoutDirection.Ltr, density) as Outline.Rounded

        assertEquals(
            RoundRect(
                left = 0f,
                top = 0f,
                right = 48f,
                bottom = 52f,
                topLeftCornerRadius = CornerRadius(26f),
                bottomLeftCornerRadius = CornerRadius(26f),
                topRightCornerRadius = CornerRadius(2f),
                bottomRightCornerRadius = CornerRadius(2f),
            ),
            startOutline.roundRect,
        )
        assertEquals(
            RoundRect(
                left = 0f,
                top = 0f,
                right = 48f,
                bottom = 52f,
                topLeftCornerRadius = CornerRadius(2f),
                bottomLeftCornerRadius = CornerRadius(2f),
                topRightCornerRadius = CornerRadius(26f),
                bottomRightCornerRadius = CornerRadius(26f),
            ),
            endOutline.roundRect,
        )
    }
}
