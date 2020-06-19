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

package androidx.ui.material

import android.os.Build
import androidx.compose.state
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.ui.core.DensityAmbient
import androidx.ui.core.Modifier
import androidx.ui.core.testTag
import androidx.ui.foundation.Strings
import androidx.ui.foundation.drawBackground
import androidx.ui.graphics.Color
import androidx.ui.graphics.compositeOver
import androidx.ui.graphics.toArgb
import androidx.ui.layout.Column
import androidx.ui.layout.Stack
import androidx.ui.test.assertHasNoClickAction
import androidx.ui.test.assertIsEnabled
import androidx.ui.test.assertIsOff
import androidx.ui.test.assertIsOn
import androidx.ui.test.assertValueEquals
import androidx.ui.test.captureToBitmap
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findByTag
import androidx.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.sqrt

@MediumTest
@RunWith(JUnit4::class)
class SwitchUiTest {

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    private val defaultSwitchTag = "switch"

    @Test
    fun switch_defaultSemantics() {
        composeTestRule.setMaterialContent {
            Column {
                Switch(modifier = Modifier.testTag("checked"), checked = true, onCheckedChange = {})
                Switch(
                    modifier = Modifier.testTag("unchecked"),
                    checked = false,
                    onCheckedChange = {}
                )
            }
        }

        findByTag("checked")
            .assertIsEnabled()
            .assertIsOn()
            .assertValueEquals(Strings.Checked)
        findByTag("unchecked")
            .assertIsEnabled()
            .assertIsOff()
            .assertValueEquals(Strings.Unchecked)
    }

    @Test
    fun switch_toggle() {
        composeTestRule.setMaterialContent {
            val (checked, onChecked) = state { false }

            // Stack is needed because otherwise the control will be expanded to fill its parent
            Stack {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsOff()
            .doClick()
            .assertIsOn()
    }

    @Test
    fun switch_toggleTwice() {
        composeTestRule.setMaterialContent {
            val (checked, onChecked) = state { false }

            // Stack is needed because otherwise the control will be expanded to fill its parent
            Stack {
                Switch(
                    modifier = Modifier.testTag(defaultSwitchTag),
                    checked = checked,
                    onCheckedChange = onChecked
                )
            }
        }
        findByTag(defaultSwitchTag)
            .assertIsOff()
            .doClick()
            .assertIsOn()
            .doClick()
            .assertIsOff()
    }

    @Test
    fun switch_uncheckableWithNoLambda() {
        composeTestRule.setMaterialContent {
            val (checked, _) = state { false }
            Switch(
                modifier = Modifier.testTag(defaultSwitchTag),
                checked = checked,
                onCheckedChange = {},
                enabled = false
            )
        }
        findByTag(defaultSwitchTag)
            .assertHasNoClickAction()
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switch_checkedLeftEndIsRounded() {
        testSwitchEndRounded(true)
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
    @Test
    fun switch_uncheckedRightEndIsRounded() {
        testSwitchEndRounded(false)
    }

    /**
     * Helper method used to verify the ends of the switch opposite the thumb
     * are rounded properly
     */
    private fun testSwitchEndRounded(checked: Boolean) {
        var trackWidthPx = 0
        var strokeWidthPx = 0
        var thumbDiameter = 0
        var surfaceColor = Color.White
        composeTestRule.setMaterialContent {
            with (DensityAmbient.current) {
                trackWidthPx = TrackWidth.toIntPx()
                strokeWidthPx = TrackStrokeWidth.toIntPx()
                thumbDiameter = ThumbDiameter.toIntPx()
            }
            surfaceColor = MaterialTheme.colors.onSurface
            Switch(modifier = Modifier.testTag(defaultSwitchTag).drawBackground(Color.Gray),
                color = Color.Red,
                checked = checked,
                onCheckedChange = {},
                enabled = false
            )
        }

        findByTag(defaultSwitchTag).captureToBitmap().apply {
            // Ensure we resolved the physical pixels properly before validating the drawn pixels
            assertNotEquals(0, trackWidthPx)
            assertNotEquals(0, strokeWidthPx)
            assertEquals(height, thumbDiameter)
            assertEquals(width, trackWidthPx)

            val hyp = sqrt(2.0) / 2
            val radius = strokeWidthPx / 2
            val left = if (checked) {
                floor(radius - radius * hyp).toInt()
            } else {
                ceil(width - radius + radius * hyp).toInt()
            }

            val upper = floor(height / 2 - radius * hyp).toInt()
            val lower = ceil(height / 2 + radius * hyp).toInt()

            // sample pixels slightly outside the rounded corner radius
            assertEquals(getPixel(left, upper - 2), Color.Gray.toArgb())
            assertEquals(getPixel(left, lower + 2), Color.Gray.toArgb())

            if (checked) {
                val compositedColor =
                    Color.Red.copy(alpha = CheckedTrackOpacity).compositeOver(Color.Gray)

                val leftUpperPixel = Color(getPixel(left, upper + 3))
                assertEquals(leftUpperPixel.alpha, compositedColor.alpha, 0.01f)
                assertEquals(leftUpperPixel.red, compositedColor.red, 0.01f)
                assertEquals(leftUpperPixel.blue, compositedColor.blue, 0.01f)
                assertEquals(leftUpperPixel.green, compositedColor.green, 0.01f)

                val leftCenterPixel = Color(getPixel(0, height / 2))
                assertEquals(leftCenterPixel.alpha, compositedColor.alpha, 0.01f)
                assertEquals(leftCenterPixel.red, compositedColor.red, 0.01f)
                assertEquals(leftCenterPixel.blue, compositedColor.blue, 0.01f)
                assertEquals(leftCenterPixel.green, compositedColor.green, 0.01f)

                val leftLowerPixel = Color(getPixel(left, lower - 3))
                assertEquals(leftLowerPixel.alpha, compositedColor.alpha, 0.01f)
                assertEquals(leftLowerPixel.red, compositedColor.red, 0.01f)
                assertEquals(leftLowerPixel.blue, compositedColor.blue, 0.01f)
                assertEquals(leftLowerPixel.green, compositedColor.green, 0.01f)
            } else {
                val compositedColor = surfaceColor.copy(alpha = UncheckedTrackOpacity)
                        .compositeOver(Color.Gray)

                val rightUpperPixel = Color(getPixel(left, upper + 3))
                assertEquals(rightUpperPixel.alpha, compositedColor.alpha, 0.01f)
                assertEquals(rightUpperPixel.red, compositedColor.red, 0.01f)
                assertEquals(rightUpperPixel.blue, compositedColor.blue, 0.01f)
                assertEquals(rightUpperPixel.green, compositedColor.green, 0.01f)

                val rightCenterPixel = Color(getPixel(width - 1, height / 2))

                assertEquals(rightCenterPixel.alpha, compositedColor.alpha, 0.01f)
                assertEquals(rightCenterPixel.red, compositedColor.red, 0.01f)
                assertEquals(rightCenterPixel.blue, compositedColor.blue, 0.01f)
                assertEquals(rightCenterPixel.green, compositedColor.green, 0.01f)

                val rightLowerPixel = Color(getPixel(left, lower - 3))
                assertEquals(rightLowerPixel.alpha, compositedColor.alpha, 0.01f)
                assertEquals(rightLowerPixel.red, compositedColor.red, 0.01f)
                assertEquals(rightLowerPixel.blue, compositedColor.blue, 0.01f)
                assertEquals(rightLowerPixel.green, compositedColor.green, 0.01f)
            }
        }
    }

    @Test
    fun switch_materialSizes_whenChecked() {
        materialSizesTestForValue(true)
    }

    @Test
    fun switch_materialSizes_whenUnchecked() {
        materialSizesTestForValue(false)
    }

    private fun materialSizesTestForValue(checked: Boolean) {
        composeTestRule
            .setMaterialContentAndCollectSizes {
                Switch(checked = checked, onCheckedChange = {}, enabled = false)
            }
            .assertWidthEqualsTo { 34.dp.toIntPx() + 2.dp.toIntPx() * 2 }
            .assertHeightEqualsTo { 20.dp.toIntPx() + 2.dp.toIntPx() * 2 }
    }
}