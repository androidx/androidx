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

import androidx.compose.composer
import androidx.test.filters.LargeTest
import androidx.ui.core.dp
import androidx.ui.layout.Container
import androidx.ui.material.samples.TextTabs
import androidx.ui.material.surface.Surface
import androidx.ui.painting.Image
import androidx.ui.painting.ImageConfig
import androidx.ui.test.assertCountEquals
import androidx.ui.test.assertIsNotSelected
import androidx.ui.test.assertIsSelected
import androidx.ui.test.createComposeRule
import androidx.ui.test.doClick
import androidx.ui.test.findAll
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@LargeTest
@RunWith(JUnit4::class)
class TabTest {

    private val ExpectedSmallTabHeight = 48.dp
    private val ExpectedLargeTabHeight = 72.dp

    private val image = Image(
        width = 10,
        height = 10,
        config = ImageConfig.Argb8888,
        hasAlpha = false
    )

    @get:Rule
    val composeTestRule = createComposeRule(disableTransitions = true)

    @Test
    fun textTab_Height() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                Container {
                    Surface {
                        Tab(text = "Text", selected = true, onSelected = {})
                    }
                }
            }
            .assertHeightEqualsTo(ExpectedSmallTabHeight)
    }

    @Test
    fun iconTab_Height() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                Container {
                    Surface {
                        Tab(icon = image, selected = true, onSelected = {})
                    }
                }
            }
            .assertHeightEqualsTo(ExpectedSmallTabHeight)
    }

    @Test
    fun textAndIconTab_Height() {
        composeTestRule
            .setMaterialContentAndTestSizes {
                Container {
                    Surface {
                        Tab(text = "Text And Icon", icon = image, selected = true, onSelected = {})
                    }
                }
            }
            .assertHeightEqualsTo(ExpectedLargeTabHeight)
    }

    @Test
    fun tabRow_initialTabSelected() {
        composeTestRule
            .setMaterialContent {
                TextTabs()
            }

        findAll { isInMutuallyExclusiveGroup }.apply {
            forEachIndexed { index, interaction ->
                if (index == 0) {
                    interaction.assertIsSelected()
                } else {
                    interaction.assertIsNotSelected()
                }
            }
        }.assertCountEquals(3)
    }

    @Test
    fun tabRow_selectNewTab() {
        composeTestRule
            .setMaterialContent {
                TextTabs()
            }

        // Only the first tab should be selected
        findAll { isInMutuallyExclusiveGroup }.apply {
            forEachIndexed { index, interaction ->
                if (index == 0) {
                    interaction.assertIsSelected()
                } else {
                    interaction.assertIsNotSelected()
                }
            }
        }.assertCountEquals(3)

        // Click the last tab
        findAll { isInMutuallyExclusiveGroup }.last().doClick()

        // Now only the last tab should be selected
        findAll { isInMutuallyExclusiveGroup }.apply {
            forEachIndexed { index, interaction ->
                if (index == lastIndex) {
                    interaction.assertIsSelected()
                } else {
                    interaction.assertIsNotSelected()
                }
            }
        }.assertCountEquals(3)
    }
}
