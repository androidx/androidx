/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.material3

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ProgressIndicatorScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    @get:Rule
    val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "progressIndicatorWrapper"

    @Test
    fun linearProgressIndicator_lightTheme_determinate() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(progress = { 0.5f })
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_determinate")
    }

    @Test
    fun linearProgressIndicator_lightTheme_determinate_no_gap() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(
                    progress = { 0.5f },
                    gapSize = 0.dp
                )
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_determinate_no_gap")
    }

    @Test
    fun linearProgressIndicator_lightTheme_determinate_no_stop() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(
                    progress = { 0.5f },
                    drawStopIndicator = null
                )
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_determinate_no_stop")
    }

    @Test
    fun linearProgressIndicator_lightTheme_determinate_stop_offset() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(
                    modifier = Modifier.size(240.dp, 16.dp),
                    progress = { 0.5f }
                )
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_determinate_stop_offset")
    }

    @Test
    fun linearProgressIndicator_lightTheme_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator()
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_indeterminate")
    }

    @Test
    fun linearProgressIndicator_lightTheme_indeterminate_start() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator()
            }
        }
        rule.mainClock.advanceTimeBy(0)
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_indeterminate_start")
    }

    @Test
    fun linearProgressIndicator_lightTheme_indeterminate_no_gap() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(
                    gapSize = 0.dp
                )
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_indeterminate_no_gap")
    }

    @Test
    fun linearProgressIndicator_darkTheme_determinate() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(progress = { 0.5f })
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_darkTheme_determinate")
    }

    @Test
    fun linearProgressIndicator_lightTheme_determinate_customCap() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                LinearProgressIndicator(progress = { 0.5f }, strokeCap = StrokeCap.Butt)
            }
        }
        assertIndicatorAgainstGolden("linearProgressIndicator_lightTheme_determinate_customCap")
    }

    @Test
    fun circularProgressIndicator_lightTheme_determinate() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator(progress = { 0.5f })
            }
        }
        assertIndicatorAgainstGolden("circularProgressIndicator_lightTheme_determinate")
    }

    @Test
    fun circularProgressIndicator_lightTheme_determinate_no_gap() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator(
                    progress = { 0.5f },
                    gapSize = 0.dp
                )
            }
        }
        assertIndicatorAgainstGolden("circularProgressIndicator_lightTheme_determinate_no_gap")
    }

    @Test
    fun circularProgressIndicator_lightTheme_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator()
            }
        }
        rule.mainClock.advanceTimeBy(500)
        assertIndicatorAgainstGolden("circularProgressIndicator_lightTheme_indeterminate")
    }

    @Test
    fun circularProgressIndicator_lightTheme_indeterminate_start() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator()
            }
        }
        rule.mainClock.advanceTimeBy(0)
        assertIndicatorAgainstGolden("circularProgressIndicator_lightTheme_indeterminate_start")
    }

    @Test
    fun circularProgressIndicator_darkTheme_determinate() {
        rule.setMaterialContent(darkColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator(progress = { 0.5f })
            }
        }
        assertIndicatorAgainstGolden("circularProgressIndicator_darkTheme_determinate")
    }

    @Test
    fun circularProgressIndicator_lightTheme_determinate_customCapAndTrack() {
        rule.setMaterialContent(lightColorScheme()) {
            Box(wrap.testTag(wrapperTestTag)) {
                CircularProgressIndicator(
                    progress = { 0.5f },
                    trackColor = Color.Gray,
                    strokeCap = StrokeCap.Butt
                )
            }
        }
        assertIndicatorAgainstGolden(
            "circularProgressIndicator_lightTheme_determinate_customCapAndTrack"
        )
    }

    private fun assertIndicatorAgainstGolden(goldenName: String) {
        rule.onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }
}
