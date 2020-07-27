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

package androidx.compose.material

import android.os.Build
import androidx.compose.runtime.mutableStateOf
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.compose.ui.platform.DensityAmbient
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.DpConstraints
import androidx.compose.runtime.Providers
import androidx.compose.ui.platform.LayoutDirectionAmbient
import androidx.compose.ui.unit.LayoutDirection
import androidx.ui.test.assertHeightIsEqualTo
import androidx.ui.test.assertValueEquals
import androidx.ui.test.assertWidthIsEqualTo
import androidx.ui.test.captureToBitmap
import androidx.ui.test.center
import androidx.ui.test.centerX
import androidx.ui.test.centerY
import androidx.ui.test.createComposeRule
import androidx.ui.test.performPartialGesture
import androidx.ui.test.onNodeWithTag
import androidx.ui.test.left
import androidx.ui.test.right
import androidx.ui.test.runOnIdle
import androidx.ui.test.runOnUiThread
import androidx.ui.test.down
import androidx.ui.test.moveBy
import androidx.ui.test.up
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

@MediumTest
@RunWith(JUnit4::class)
class SliderTest {
    private val tag = "slider"

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun sliderPosition_valueCoercion() {
        val state = mutableStateOf(0f)
        composeTestRule.setContent {
            Slider(
                modifier = Modifier.testTag(tag),
                value = state.value,
                onValueChange = { state.value = it },
                valueRange = 0f..1f
            )
        }
        runOnIdle {
            state.value = 2f
        }
        onNodeWithTag(tag).assertValueEquals("100 percent")
        runOnIdle {
            state.value = -123145f
        }
        onNodeWithTag(tag).assertValueEquals("0 percent")
    }

    @Test(expected = IllegalArgumentException::class)
    fun sliderPosition_stepsThrowWhenLessThanZero() {
        composeTestRule.setContent {
            Slider(value = 0f, onValueChange = {}, steps = -1)
        }
    }

    @Test
    fun slider_semantics() {
        val state = mutableStateOf(0f)

        composeTestRule
            .setMaterialContent {
                Slider(modifier = Modifier.testTag(tag), value = state.value,
                    onValueChange = { state.value = it })
            }

        onNodeWithTag(tag)
            .assertValueEquals("0 percent")

        runOnUiThread {
            state.value = 0.5f
        }

        onNodeWithTag(tag)
            .assertValueEquals("50 percent")
    }

    @Test
    fun slider_drag() {
        val state = mutableStateOf(0f)

        composeTestRule
            .setMaterialContent {
                Slider(
                    modifier = Modifier.testTag(tag),
                    value = state.value,
                    onValueChange = { state.value = it }
                )
            }

        runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        onNodeWithTag(tag)
            .performPartialGesture {
                down(center)
                moveBy(Offset(100f, 0f))
                up()
                expected = calculateFraction(left, right, centerX + 100)
            }
        runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    @Test
    fun slider_tap() {
        val state = mutableStateOf(0f)

        composeTestRule
            .setMaterialContent {
                Slider(
                    modifier = Modifier.testTag(tag),
                    value = state.value,
                    onValueChange = { state.value = it }
                )
            }

        runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        onNodeWithTag(tag)
            .performPartialGesture {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX + 50)
            }
        runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    @Test
    fun slider_drag_rtl() {
        val state = mutableStateOf(0f)

        composeTestRule
            .setMaterialContent {
                Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                    Slider(
                        modifier = Modifier.testTag(tag),
                        value = state.value,
                        onValueChange = { state.value = it }
                    )
                }
            }

        runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        onNodeWithTag(tag)
            .performPartialGesture {
                down(center)
                moveBy(Offset(100f, 0f))
                up()
                // subtract here as we're in rtl and going in the opposite direction
                expected = calculateFraction(left, right, centerX - 100)
            }
        runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    @Test
    fun slider_tap_rtl() {
        val state = mutableStateOf(0f)

        composeTestRule
            .setMaterialContent {
                Providers(LayoutDirectionAmbient provides LayoutDirection.Rtl) {
                    Slider(
                        modifier = Modifier.testTag(tag),
                        value = state.value,
                        onValueChange = { state.value = it }
                    )
                }
            }

        runOnUiThread {
            Truth.assertThat(state.value).isEqualTo(0f)
        }

        var expected = 0f

        onNodeWithTag(tag)
            .performPartialGesture {
                down(Offset(centerX + 50, centerY))
                up()
                expected = calculateFraction(left, right, centerX - 50)
            }
        runOnIdle {
            Truth.assertThat(abs(state.value - expected)).isLessThan(0.001f)
        }
    }

    private fun calculateFraction(a: Float, b: Float, pos: Float) =
        ((pos - a) / (b - a)).coerceIn(0f, 1f)

    @Test
    fun slider_sizes() {
        val state = mutableStateOf(0f)
        composeTestRule
            .setMaterialContentForSizeAssertions(
                parentConstraints = DpConstraints(maxWidth = 100.dp, maxHeight = 100.dp)
            ) { Slider(value = state.value, onValueChange = { state.value = it }) }
            .assertHeightIsEqualTo(48.dp)
            .assertWidthIsEqualTo(100.dp)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun slider_endsAreRounded() {
        val sliderTag = "slider"
        var thumbStrokeWidth = 0
        var thumbPx = 0
        composeTestRule.setMaterialContent {
            with(DensityAmbient.current) {
                thumbStrokeWidth = TrackHeight.toIntPx()
                thumbPx = ThumbRadius.toIntPx()
            }
            Slider(modifier = Modifier.testTag(sliderTag).background(color = Color.Gray),
                color = Color.Green,
                value = 0.5f,
                onValueChange = {}
            )
        }

        onNodeWithTag(sliderTag).captureToBitmap().apply {
            assertNotEquals(0, thumbStrokeWidth)
            assertNotEquals(0, thumbPx)

            val compositedColor =
                Color.Green.copy(alpha = InactiveTrackColorAlpha).compositeOver(Color.Gray)

            val hyp = sqrt(2.0) / 2
            val radius = thumbStrokeWidth / 2

            val left = floor(thumbPx - radius * hyp).toInt()
            val upper = floor(height / 2 - radius * hyp).toInt()
            // top left outside the rounded area has the background color
            assertEquals(getPixel(left - 1, upper - 1), Color.Gray.toArgb())
            // top left inside the rounded area by a few pixels has the track color
            assertEquals(getPixel(left + 3, upper + 3), Color.Green.toArgb())

            val lower = ceil(height / 2 + radius * hyp).toInt()
            // bottom left outside the rounded area has the background color
            assertEquals(getPixel(left - 1, lower + 1), Color.Gray.toArgb())
            // bottom left inside the rounded area by a few pixels has the track color
            assertEquals(getPixel(left + 3, lower - 3), Color.Green.toArgb())

            // top right outside the rounded area has the background color
            val right = ceil(width - thumbPx + radius * hyp).toInt()
            assertEquals(getPixel(right + 1, upper - 1), Color.Gray.toArgb())

            // top right inside the rounded area has the track color with the
            // inactive opacity composited over the background
            val upperRightInsideColor = Color(getPixel(right - 3, upper + 3))
            assertEquals(upperRightInsideColor.alpha, compositedColor.alpha, 0.01f)
            assertEquals(upperRightInsideColor.red, compositedColor.red, 0.01f)
            assertEquals(upperRightInsideColor.blue, compositedColor.blue, 0.01f)
            assertEquals(upperRightInsideColor.green, compositedColor.green, 0.01f)

            // lower right outside the rounded area has the background color
            assertEquals(getPixel(right + 1, lower + 1), Color.Gray.toArgb())

            // lower right inside the rounded area has the track color with the
            // inactive opacity composited over the background
            val lowerRightInsideColor = Color(getPixel(right - 3, lower - 3))
            assertEquals(lowerRightInsideColor.alpha, compositedColor.alpha, 0.01f)
            assertEquals(lowerRightInsideColor.red, compositedColor.red, 0.01f)
            assertEquals(lowerRightInsideColor.blue, compositedColor.blue, 0.01f)
            assertEquals(lowerRightInsideColor.green, compositedColor.green, 0.01f)

            // left along the center has the track color
            assertEquals(getPixel(thumbPx, height / 2), Color.Green.toArgb())

            // right along the center has the modulated color composited over the background
            val actualColor = Color(getPixel(width - thumbPx, height / 2))
            assertEquals(actualColor.alpha, compositedColor.alpha, 0.01f)
            assertEquals(actualColor.red, compositedColor.red, 0.01f)
            assertEquals(actualColor.blue, compositedColor.blue, 0.01f)
            assertEquals(actualColor.green, compositedColor.green, 0.01f)
        }
    }
}