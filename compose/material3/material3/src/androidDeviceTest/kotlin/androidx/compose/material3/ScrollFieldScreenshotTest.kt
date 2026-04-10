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

package androidx.compose.material3

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class ScrollFieldScreenshotTest() {

    @get:Rule val rule = createComposeRule(StandardTestDispatcher())

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun scrollField_lightTheme() {
        rule.setMaterialContent(lightColorScheme()) { TestContent() }
        assertScrollFieldAgainstGolden("scrollField_lightTheme")
    }

    @Test
    fun scrollField_darkTheme() {
        rule.setMaterialContent(darkColorScheme()) { TestContent() }
        assertScrollFieldAgainstGolden("scrollField_darkTheme")
    }

    private fun assertScrollFieldAgainstGolden(goldenIdentifier: String) {
        rule
            .onNodeWithTag(ScrollFieldTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }

    private val ScrollFieldTestTag = "scrollField"

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun TestContent() {
        val itemCount = 100
        // The hoisted state ensures the Pager starts exactly at our 'index'
        // for a deterministic screenshot.
        val state = rememberScrollFieldState(itemCount = itemCount, index = 0)

        ScrollField(
            state = state,
            // Since this is a static screenshot, a no-op is appropriate.
            modifier = Modifier.size(width = 80.dp, height = 160.dp).testTag(ScrollFieldTestTag),
        )
    }
}
