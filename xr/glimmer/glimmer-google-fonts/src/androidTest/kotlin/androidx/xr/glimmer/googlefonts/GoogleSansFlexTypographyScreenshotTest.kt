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

@file:Suppress("MentionsGoogle")

package androidx.xr.glimmer.googlefonts

import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import androidx.test.screenshot.matchers.MSSIMMatcher
import androidx.xr.glimmer.googlefonts.samples.GoogleSansFlexTypographyUsage
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val GOLDEN_DIRECTORY = "xr/glimmer/glimmer-google-fonts"

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class GoogleSansFlexTypographyScreenshotTest {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_DIRECTORY)

    @Test
    fun defaultGoogleSansFlexTypography() {
        rule.setContent { GoogleSansFlexTypographyUsage() }
        assertRootAgainstGolden("glimmerGoogleFonts_googleSansFlexTypography")
    }

    private fun assertRootAgainstGolden(goldenName: String) {
        val matcherThreshold = 0.995
        rule
            .onRoot()
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName, MSSIMMatcher(matcherThreshold))
    }
}
