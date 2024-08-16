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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O)
class ModalExpandedNavigationRailScreenshotTest {

    @get:Rule val composeTestRule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    @Test
    fun modalExpandedNavigationRail_lightTheme_defaultColors() {
        composeTestRule.setMaterialContent(lightColorScheme()) {
            DefaultModalExpandedNavigationRail()
        }

        assertModalExpandedNavigationRailMatches(
            goldenIdentifier = "wideNavigationRail_lightTheme_defaultColors"
        )
    }

    @Test
    fun modalExpandedNavigationRail_darkTheme_defaultColors() {
        composeTestRule.setMaterialContent(darkColorScheme()) {
            DefaultModalExpandedNavigationRail()
        }

        assertModalExpandedNavigationRailMatches(
            goldenIdentifier = "wideNavigationRail_darkTheme_defaultColors"
        )
    }

    /**
     * Asserts that the ModalExpandedNavigationRail matches the screenshot with identifier
     * [goldenIdentifier].
     *
     * @param goldenIdentifier the identifier for the corresponding screenshot
     */
    private fun assertModalExpandedNavigationRailMatches(goldenIdentifier: String) {
        // Capture and compare screenshots.
        composeTestRule
            .onNode(isDialog())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenIdentifier)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun DefaultModalExpandedNavigationRail() {
    Box(Modifier.fillMaxSize()) {
        ModalExpandedNavigationRail(
            onDismissRequest = {},
        ) {
            WideNavigationRailItem(
                railExpanded = true,
                icon = { Icon(Icons.Filled.Favorite, null) },
                label = { Text("Favorites") },
                selected = true,
                onClick = {},
            )
            WideNavigationRailItem(
                railExpanded = true,
                icon = { Icon(Icons.Filled.Home, null) },
                label = { Text("Home") },
                selected = false,
                onClick = {}
            )
            WideNavigationRailItem(
                railExpanded = true,
                icon = { Icon(Icons.Filled.Search, null) },
                label = { Text("Search") },
                selected = false,
                onClick = {}
            )
        }
    }
}
