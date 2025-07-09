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

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.xr.glimmer.samples.ColorsSample
import androidx.xr.glimmer.samples.IconSizesSample
import androidx.xr.glimmer.samples.ShapesSample
import androidx.xr.glimmer.samples.TypographySample
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class GlimmerThemeScreenshotTest() {

    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun colors() {
        rule.setGlimmerThemeContent { ColorsSample() }
        rule.assertRootAgainstGolden("glimmerTheme_colors", screenshotRule)
    }

    @Test
    fun typography() {
        rule.setGlimmerThemeContent { TypographySample() }
        rule.assertRootAgainstGolden("glimmerTheme_typography", screenshotRule)
    }

    @Test
    fun shapes() {
        rule.setGlimmerThemeContent { ShapesSample() }
        rule.assertRootAgainstGolden("glimmerTheme_shapes", screenshotRule)
    }

    @Test
    fun iconSizes() {
        rule.setGlimmerThemeContent { IconSizesSample() }
        rule.assertRootAgainstGolden("glimmerTheme_iconSizes", screenshotRule)
    }
}
