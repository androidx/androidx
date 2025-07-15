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

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.runtime.Composable
import androidx.compose.testutils.assertAgainstGolden
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
class AppBarColumnScreenshotTest {
    @get:Rule val rule = createComposeRule()

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(GOLDEN_MATERIAL3)

    private val wrapperTestTag = "WrapperTestTag"

    @Test
    fun appBarColumn_fullHeight() {
        rule.setContent { AppBarColumnTest(itemCount = 2) }

        assertAgainstGolden("appBarColumn_no_overflow")
    }

    @Test
    fun appBarColumn_withOverflow() {
        rule.setContent { AppBarColumnTest(itemCount = 5) }

        assertAgainstGolden("appBarColumn_overflow")
    }

    @Test
    fun appBarColumn_withOverflow_menu() {
        rule.setContent { AppBarColumnTest(itemCount = 5) }

        rule.onNodeWithTag("Overflow").performClick()

        rule
            .onNode(isPopup())
            .captureToImage()
            .assertAgainstGolden(screenshotRule, "appBarColumn_overflow_menu")
    }

    private fun assertAgainstGolden(goldenName: String) {
        rule
            .onNodeWithTag(wrapperTestTag)
            .captureToImage()
            .assertAgainstGolden(screenshotRule, goldenName)
    }

    data class AppBarItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

    @Composable
    fun AppBarColumnTest(modifier: Modifier = Modifier, itemCount: Int) {
        val appBarItems =
            listOf(
                AppBarItem(label = "Favorite", icon = Icons.Filled.Favorite, onClick = {}),
                AppBarItem(label = "Add", icon = Icons.Filled.Add, onClick = {}),
                AppBarItem(label = "Edit", icon = Icons.Filled.Edit, onClick = {}),
            )

        AppBarColumn(
            modifier = modifier.height(150.dp).testTag(wrapperTestTag),
            overflowIndicator = { menuState ->
                IconButton(
                    modifier = Modifier.testTag("Overflow"),
                    onClick = {
                        if (menuState.isExpanded) {
                            menuState.dismiss()
                        } else {
                            menuState.show()
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Localized description",
                    )
                }
            },
        ) {
            repeat(itemCount) { index ->
                val item = appBarItems[index % 3]
                clickableItem(
                    onClick = item.onClick,
                    icon = {
                        Icon(imageVector = item.icon, contentDescription = "Localized description")
                    },
                    enabled = true,
                    label = item.label,
                )
            }
        }
    }
}
