/*
 * Copyright 2024 The Android Open Source Project
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@MediumTest
@RunWith(Parameterized::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class LoadingIndicatorScreenshotTest(private val scheme: ColorSchemeWrapper) {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrap = Modifier.wrapContentSize(Alignment.TopStart)
    private val wrapperTestTag = "loadingIndicatorWrapper"

    @Test
    fun containedLoadingIndicator_determinate_start_progress() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { ContainedLoadingIndicator(progress = { 0f }) }
        }
        assertIndicatorAgainstGolden(
            "containedLoadingIndicator_determinate_start_progress_${scheme.name}"
        )
    }

    @Test
    fun containedLoadingIndicator_determinate_mid_progress() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { ContainedLoadingIndicator(progress = { 0.5f }) }
        }
        assertIndicatorAgainstGolden(
            "containedLoadingIndicator_determinate_mid_progress_${scheme.name}"
        )
    }

    @Test
    fun containedLoadingIndicator_determinate_end_progress() {
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { ContainedLoadingIndicator(progress = { 1f }) }
        }
        assertIndicatorAgainstGolden(
            "containedLoadingIndicator_determinate_end_progress_${scheme.name}"
        )
    }

    @Test
    fun loadingIndicator_determinate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LoadingIndicator() }
        }
        assertIndicatorAgainstGolden("loadingIndicator_determinate_${scheme.name}")
    }

    @Test
    fun loadingIndicator_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { LoadingIndicator() }
        }
        assertIndicatorAgainstGolden("loadingIndicator_indeterminate_${scheme.name}")
    }

    @Test
    fun containedLoadingIndicator_indeterminate() {
        rule.mainClock.autoAdvance = false
        rule.setMaterialContent(scheme.colorScheme) {
            Box(wrap.testTag(wrapperTestTag)) { ContainedLoadingIndicator() }
        }
        assertIndicatorAgainstGolden("containedLoadingIndicator_indeterminate_${scheme.name}")
    }

    private fun assertIndicatorAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    companion object {
        private const val TestTag = "testTag"

        @Parameterized.Parameters(name = "{0}")
        @JvmStatic
        fun parameters() =
            arrayOf(
                ColorSchemeWrapper("lightTheme", lightColorScheme()),
                ColorSchemeWrapper("darkTheme", darkColorScheme()),
            )
    }

    class ColorSchemeWrapper(val name: String, val colorScheme: ColorScheme) {
        override fun toString(): String {
            return name
        }
    }
}
